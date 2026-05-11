package org.example.coffee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coffee.common.Common;
import org.example.coffee.dto.user.ChangePasswordRequest;
import org.example.coffee.dto.user.ForgotPasswordRequest;
import org.example.coffee.dto.user.PendingRegisterData;
import org.example.coffee.dto.user.ResendRegisterOtpRequest;
import org.example.coffee.dto.user.ResetPasswordRequest;
import org.example.coffee.dto.user.SignUpRequest;
import org.example.coffee.dto.user.VerifyRegisterOtpRequest;
import org.example.coffee.entity.UserEntity;
import org.example.coffee.exceptionhandler.BadRequestException;
import org.example.coffee.mapper.UserMapper;
import org.example.coffee.repository.CustomRepository;
import org.example.coffee.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private UserRepository userRepository;
    private CustomRepository customRepository;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JavaMailSender javaMailSender;
    private ObjectMapper objectMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        UserMapper userMapper = mock(UserMapper.class);
        customRepository = mock(CustomRepository.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        javaMailSender = mock(JavaMailSender.class);
        objectMapper = new ObjectMapper();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        userService = new UserService(userRepository, userMapper, customRepository,
                stringRedisTemplate, javaMailSender, objectMapper);
    }

    @Test
    void signUp_success_sendsOtpAndDoesNotCreateUser() {
        SignUpRequest request = signUpRequest();

        String result = userService.signUp(request);

        assertEquals("OTP_SENT", result);
        verify(valueOperations).set(eq("register:otp:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("register:data:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(javaMailSender).send(any(SimpleMailMessage.class));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void signUp_existingUsername_fail() {
        SignUpRequest request = signUpRequest();
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.signUp(request));
    }

    @Test
    void signUp_existingEmail_fail() {
        SignUpRequest request = signUpRequest();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.signUp(request));
    }

    @Test
    void verifyRegisterOtp_success_createsUserAndDeletesRedisKeys() throws Exception {
        PendingRegisterData pending = new PendingRegisterData(
                "testuser", "$2a$10$hashed", "Test User", "0123456789", "test@example.com");
        when(valueOperations.get("register:otp:test@example.com")).thenReturn("123456");
        when(valueOperations.get("register:data:test@example.com")).thenReturn(objectMapper.writeValueAsString(pending));

        VerifyRegisterOtpRequest request = new VerifyRegisterOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");

        String result = userService.verifyRegisterOtp(request);

        assertEquals("REGISTER_SUCCESS", result);
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("testuser", userCaptor.getValue().getUsername());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
        assertEquals(Boolean.FALSE, userCaptor.getValue().getIsShop());
        assertEquals(Common.IMAGE_DEFAULT, userCaptor.getValue().getImage());
        verify(stringRedisTemplate).delete("register:otp:test@example.com");
        verify(stringRedisTemplate).delete("register:data:test@example.com");
    }

    @Test
    void verifyRegisterOtp_wrongOtp_fail() throws Exception {
        PendingRegisterData pending = new PendingRegisterData(
                "testuser", "$2a$10$hashed", "Test User", "0123456789", "test@example.com");
        when(valueOperations.get("register:otp:test@example.com")).thenReturn("123456");
        when(valueOperations.get("register:data:test@example.com")).thenReturn(objectMapper.writeValueAsString(pending));

        VerifyRegisterOtpRequest request = new VerifyRegisterOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("000000");

        assertThrows(BadRequestException.class, () -> userService.verifyRegisterOtp(request));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void verifyRegisterOtp_expired_fail() {
        VerifyRegisterOtpRequest request = new VerifyRegisterOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");

        assertThrows(BadRequestException.class, () -> userService.verifyRegisterOtp(request));
    }

    @Test
    void resendRegisterOtp_success_replacesOtp() throws Exception {
        PendingRegisterData pending = new PendingRegisterData(
                "testuser", "$2a$10$hashed", "Test User", "0123456789", "test@example.com");
        when(valueOperations.get("register:data:test@example.com")).thenReturn(objectMapper.writeValueAsString(pending));

        ResendRegisterOtpRequest request = new ResendRegisterOtpRequest();
        request.setEmail("test@example.com");

        String result = userService.resendRegisterOtp(request);

        assertEquals("OTP_SENT", result);
        verify(valueOperations).set(eq("register:otp:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("register:data:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resendRegisterOtp_noPendingData_fail() {
        ResendRegisterOtpRequest request = new ResendRegisterOtpRequest();
        request.setEmail("test@example.com");

        assertThrows(BadRequestException.class, () -> userService.resendRegisterOtp(request));
    }

    @Test
    void signUp_storesHashedPasswordInRedis() {
        SignUpRequest request = signUpRequest();
        ArgumentCaptor<String> pendingDataCaptor = ArgumentCaptor.forClass(String.class);

        userService.signUp(request);

        verify(valueOperations).set(eq("register:data:test@example.com"), pendingDataCaptor.capture(), eq(Duration.ofMinutes(5)));
        assertTrue(pendingDataCaptor.getValue().contains("\"password\":\"$2"));
    }

    @Test
    void forgotPassword_success_sendsOtp() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("Test@Example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(UserEntity.builder().id(1L).build());

        String result = userService.forgotPassword(request);

        assertEquals("OTP_SENT", result);
        verify(valueOperations).set(eq("password-reset:otp:test@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPassword_success_updatesPasswordAndDeletesOtp() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword123");
        UserEntity user = UserEntity.builder().id(1L).email("test@example.com").password("$2a$old").build();
        when(valueOperations.get("password-reset:otp:test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        String result = userService.resetPassword(request);

        assertEquals("PASSWORD_RESET_SUCCESS", result);
        verify(userRepository).save(user);
        verify(stringRedisTemplate).delete("password-reset:otp:test@example.com");
    }

    @Test
    void resetPassword_wrongOtp_fail() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("000000");
        request.setNewPassword("newPassword123");
        when(valueOperations.get("password-reset:otp:test@example.com")).thenReturn("123456");

        assertThrows(BadRequestException.class, () -> userService.resetPassword(request));
    }

    @Test
    void changePassword_success() {
        try (MockedStatic<org.example.coffee.token.TokenHelper> tokenHelperMock = mockStatic(org.example.coffee.token.TokenHelper.class)) {
            String oldPassword = org.mindrot.jbcrypt.BCrypt.hashpw("oldPassword123", org.mindrot.jbcrypt.BCrypt.gensalt());
            UserEntity user = UserEntity.builder().id(1L).password(oldPassword).build();
            tokenHelperMock.when(() -> org.example.coffee.token.TokenHelper.getUserIdFromToken("Bearer token")).thenReturn(1L);
            when(customRepository.getUserBy(1L)).thenReturn(user);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("oldPassword123");
            request.setNewPassword("newPassword123");

            String result = userService.changePassword("Bearer token", request);

            assertEquals("PASSWORD_CHANGED", result);
            verify(userRepository).save(user);
        }
    }

    private SignUpRequest signUpRequest() {
        return new SignUpRequest("testuser", "password123", "Test User", "0123456789", "Test@Example.com");
    }
}
