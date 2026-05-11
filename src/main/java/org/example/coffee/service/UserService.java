package org.example.coffee.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.coffee.cloudinary.CloudinaryHelper;
import org.example.coffee.common.Common;
import org.example.coffee.dto.user.ChangeInfoUserRequest;
import org.example.coffee.dto.user.ChangePasswordRequest;
import org.example.coffee.dto.user.ForgotPasswordRequest;
import org.example.coffee.dto.user.PendingRegisterData;
import org.example.coffee.dto.user.ResendRegisterOtpRequest;
import org.example.coffee.dto.user.ResetPasswordRequest;
import org.example.coffee.dto.user.SignUpRequest;
import org.example.coffee.dto.user.TokenResponse;
import org.example.coffee.dto.user.UserOutput;
import org.example.coffee.dto.user.UserRequest;
import org.example.coffee.dto.user.VerifyRegisterOtpRequest;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.exceptionhandler.UnauthorizedException;
import org.example.coffee.mapper.UserMapper;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.UserRepository;
import org.example.coffee.token.TokenHelper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {
    private static final Duration REGISTER_OTP_TTL = Duration.ofMinutes(5);
    private static final String REGISTER_OTP_KEY_PREFIX = "register:otp:";
    private static final String REGISTER_DATA_KEY_PREFIX = "register:data:";
    private static final Duration PASSWORD_RESET_OTP_TTL = Duration.ofMinutes(5);
    private static final String PASSWORD_RESET_OTP_KEY_PREFIX = "password-reset:otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final CustomRepository customRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final JavaMailSender javaMailSender;
	private final ObjectMapper objectMapper;

	@Transactional
	public String signUp(SignUpRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			throw new BadRequestException(Common.USERNAME_IS_EXISTS);
		}

		String email = normalizeEmail(signUpRequest.getEmail());
		if (userRepository.existsByEmail(email)) {
			throw new BadRequestException("EMAIL_IS_EXISTS");
		}

		PendingRegisterData pendingRegisterData = new PendingRegisterData(
					signUpRequest.getUsername(),
					BCrypt.hashpw(signUpRequest.getPassword(), BCrypt.gensalt()),
					signUpRequest.getFullName(),
					signUpRequest.getPhoneNumber(),
					email
		);
		String otp = generateOtp();
		savePendingRegister(email, pendingRegisterData, otp);
		sendRegisterOtp(email, otp);
		log.info("Register OTP sent to email: {}", email);
		return "OTP_SENT";
	}

	@Transactional
	public String verifyRegisterOtp(VerifyRegisterOtpRequest request) {
		String email = normalizeEmail(request.getEmail());
		String otpKey = getOtpKey(email);
		String dataKey = getDataKey(email);
		String currentOtp = stringRedisTemplate.opsForValue().get(otpKey);
		String pendingDataJson = stringRedisTemplate.opsForValue().get(dataKey);

		if (Objects.isNull(currentOtp) || Objects.isNull(pendingDataJson)) {
			throw new BadRequestException("REGISTER_OTP_EXPIRED");
		}
		if (!currentOtp.equals(request.getOtp())) {
			throw new BadRequestException("REGISTER_OTP_INVALID");
		}

		PendingRegisterData pendingRegisterData = readPendingRegisterData(pendingDataJson);
		if (userRepository.existsByUsername(pendingRegisterData.getUsername())) {
			throw new BadRequestException(Common.USERNAME_IS_EXISTS);
		}
		if (userRepository.existsByEmail(pendingRegisterData.getEmail())) {
			throw new BadRequestException("EMAIL_IS_EXISTS");
		}

		UserEntity userEntity = UserEntity.builder()
					.username(pendingRegisterData.getUsername())
					.password(pendingRegisterData.getPassword())
					.fullName(pendingRegisterData.getFullName())
					.email(pendingRegisterData.getEmail())
					.phoneNumber(pendingRegisterData.getPhoneNumber())
					.build();
		userEntity.setImage(Common.IMAGE_DEFAULT);
		userEntity.setIsShop(Boolean.FALSE);
		userRepository.save(userEntity);
		stringRedisTemplate.delete(otpKey);
		stringRedisTemplate.delete(dataKey);
		log.info("Register success: {}", pendingRegisterData.getUsername());
		return "REGISTER_SUCCESS";
	}

    public String resendRegisterOtp(ResendRegisterOtpRequest request) {
		String email = normalizeEmail(request.getEmail());
		String pendingDataJson = stringRedisTemplate.opsForValue().get(getDataKey(email));
		if (Objects.isNull(pendingDataJson)) {
			throw new BadRequestException("REGISTER_OTP_EXPIRED");
		}

		PendingRegisterData pendingRegisterData = readPendingRegisterData(pendingDataJson);
		String otp = generateOtp();
		savePendingRegister(email, pendingRegisterData, otp);
		sendRegisterOtp(email, otp);
		log.info("Register OTP resent to email: {}", email);
        return "OTP_SENT";
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserEntity userEntity = userRepository.findByEmail(email);
        if (Objects.isNull(userEntity)) {
            throw new BadRequestException("EMAIL_NOT_FOUND");
        }

        String otp = generateOtp();
        stringRedisTemplate.opsForValue().set(getPasswordResetOtpKey(email), otp, PASSWORD_RESET_OTP_TTL);
        sendOtpEmail(email, "Coffee password reset OTP",
                "Your password reset OTP is: " + otp + ". This code expires in 5 minutes.");
        log.info("Password reset OTP sent to email: {}", email);
        return "OTP_SENT";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        String key = getPasswordResetOtpKey(email);
        String currentOtp = stringRedisTemplate.opsForValue().get(key);
        if (Objects.isNull(currentOtp)) {
            throw new BadRequestException("PASSWORD_RESET_OTP_EXPIRED");
        }
        if (!currentOtp.equals(request.getOtp())) {
            throw new BadRequestException("PASSWORD_RESET_OTP_INVALID");
        }

        UserEntity userEntity = userRepository.findByEmail(email);
        if (Objects.isNull(userEntity)) {
            throw new BadRequestException("EMAIL_NOT_FOUND");
        }

        userEntity.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        userRepository.save(userEntity);
        stringRedisTemplate.delete(key);
        log.info("Password reset success for email: {}", email);
        return "PASSWORD_RESET_SUCCESS";
    }

    @Transactional
    public String changePassword(String accessToken, ChangePasswordRequest request) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserEntity userEntity = customRepository.getUserBy(userId);
        if (!BCrypt.checkpw(request.getOldPassword(), userEntity.getPassword())) {
            throw new UnauthorizedException(Common.INCORRECT_PASSWORD);
        }

        userEntity.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        userRepository.save(userEntity);
        log.info("Password changed for user: {}", userId);
        return "PASSWORD_CHANGED";
    }

	@Transactional
	public TokenResponse logIn(UserRequest logInRequest) {
		UserEntity userEntity = userRepository.findByUsername(logInRequest.getUsername());
		if (Objects.isNull(userEntity)) {
			throw new UnauthorizedException(Common.RECORD_NOT_FOUND);
		}
		String currentHashedPassword = userEntity.getPassword();
		if (BCrypt.checkpw(logInRequest.getPassword(), currentHashedPassword)) {
			TokenResponse tokenResponse = TokenResponse.builder()
						.accessToken(TokenHelper.generateToken(userEntity))
						.isShop(userEntity.getIsShop())
						.build();
			log.info("Login success: {}", logInRequest.getUsername());
			return tokenResponse;
		}
		log.warn("Login failed: {}", logInRequest.getUsername());
		throw new UnauthorizedException(Common.INCORRECT_PASSWORD);
	}

	@Transactional
	public void changeInformation(ChangeInfoUserRequest changeInfoUserRequest, String accessToken, MultipartFile multipartFile) {
		Long userId = TokenHelper.getUserIdFromToken(accessToken);
		UserEntity userEntity = customRepository.getUserBy(userId);
		userMapper.updateEntityFromInput(userEntity, changeInfoUserRequest);
		userEntity.setPhoneNumber(changeInfoUserRequest.getPhoneNumber());
		if (Objects.nonNull(multipartFile) && !multipartFile.isEmpty()) {
			userEntity.setImage(CloudinaryHelper.uploadAndGetFileUrl(multipartFile));
		}
		userRepository.save(userEntity);
	}

	@Transactional(readOnly = true)
	public UserOutput getInformation(String accessToken) {
		Long userId = TokenHelper.getUserIdFromToken(accessToken);
		UserEntity userEntity = customRepository.getUserBy(userId);
		return UserOutput.builder()
					.fullName(userEntity.getFullName())
					.phoneNumber(userEntity.getPhoneNumber())
					.email(userEntity.getEmail())
					.imageUrl(userEntity.getImage())
					.build();
	}

	private void savePendingRegister(String email, PendingRegisterData pendingRegisterData, String otp) {
		try {
			String pendingDataJson = objectMapper.writeValueAsString(pendingRegisterData);
			stringRedisTemplate.opsForValue().set(getOtpKey(email), otp, REGISTER_OTP_TTL);
			stringRedisTemplate.opsForValue().set(getDataKey(email), pendingDataJson, REGISTER_OTP_TTL);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(Common.ACTION_FAIL);
		}
	}

	private PendingRegisterData readPendingRegisterData(String pendingDataJson) {
		try {
			return objectMapper.readValue(pendingDataJson, PendingRegisterData.class);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(Common.ACTION_FAIL);
		}
	}

    private void sendRegisterOtp(String email, String otp) {
        sendOtpEmail(email, "Coffee registration OTP",
                "Your registration OTP is: " + otp + ". This code expires in 5 minutes.");
    }

    private void sendOtpEmail(String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send OTP email to {}", email, e);
            throw new BadRequestException("EMAIL_SEND_FAILED");
        }
    }

	private String generateOtp() {
		return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private String getOtpKey(String email) {
		return REGISTER_OTP_KEY_PREFIX + email;
	}

    private String getDataKey(String email) {
        return REGISTER_DATA_KEY_PREFIX + email;
    }

    private String getPasswordResetOtpKey(String email) {
        return PASSWORD_RESET_OTP_KEY_PREFIX + email;
    }
}
