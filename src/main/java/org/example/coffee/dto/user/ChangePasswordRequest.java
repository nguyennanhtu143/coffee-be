package org.example.coffee.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank(message = "Old password không được để trống")
    private String oldPassword;

    @NotBlank(message = "New password không được để trống")
    @Size(min = 6, message = "New password phải từ 6 ký tự trở lên")
    private String newPassword;
}
