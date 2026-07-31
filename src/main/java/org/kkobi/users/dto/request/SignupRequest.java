package org.kkobi.users.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;
    private LocalDate birthDate;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
}
