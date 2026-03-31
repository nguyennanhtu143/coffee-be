package org.example.coffee.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ChangeInfoUserRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String gender;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phoneNumber;
}
