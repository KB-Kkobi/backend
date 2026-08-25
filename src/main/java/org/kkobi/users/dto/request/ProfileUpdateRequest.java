package org.kkobi.users.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.kkobi.users.enums.ProfileImageType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "생년월일은 필수 입력값입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate birthDate;

    private ProfileImageType profileImage;
}
