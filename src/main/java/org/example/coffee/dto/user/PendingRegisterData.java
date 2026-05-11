package org.example.coffee.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PendingRegisterData {
    private String username;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String email;
}
