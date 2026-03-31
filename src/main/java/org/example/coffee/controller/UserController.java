package org.example.coffee.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.user.*;
import org.example.coffee.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/user")

public class UserController {
    private final UserService userService;

    @Operation(summary = "Đăng kí tài khoản")
    @PostMapping("/sign-up")
    public String signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        return userService.signUp(signUpRequest);
    }

    @Operation(summary = "Đăng nhập")
    @PostMapping("/log-in")
    public TokenResponse logIn(@Valid @RequestBody UserRequest logInRequest) {
        return userService.logIn(logInRequest);
    }

    @Operation(summary = "Thay đổi thông tin người dùng")
    @PostMapping("/change-information")
    public void changeInformation(@RequestPart(name = "changeInfoUser") String changeInfoUserRequestString,
                                  @RequestHeader("Authorization") String accessToken,
                                  @RequestPart(name = "image", required = false) MultipartFile multipartFile) throws JsonProcessingException {
        ChangeInfoUserRequest changeInfoUserRequest;
        ObjectMapper objectMapper = new ObjectMapper();
        changeInfoUserRequest = objectMapper.readValue(changeInfoUserRequestString, ChangeInfoUserRequest.class);
        userService.changeInformation(changeInfoUserRequest,accessToken, multipartFile);
    }

    @Operation(summary = "Lấy ra thông tin người dùng")
    @GetMapping("/get-information")
    public UserOutput getInformation(@RequestHeader("Authorization") String accessToken) {
        return userService.getInformation(accessToken);
    }
}
