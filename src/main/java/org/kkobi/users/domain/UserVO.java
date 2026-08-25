package org.kkobi.users.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kkobi.users.enums.ProfileImageType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long userId;
    private String email;
    private String password;
    private String nickname;
    private LocalDate birthDate;
    private ProfileImageType profileImage;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
