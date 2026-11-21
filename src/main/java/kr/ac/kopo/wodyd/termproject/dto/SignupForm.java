package kr.ac.kopo.wodyd.termproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {

    @NotBlank(message = "아이디를 입력하세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문/숫자 4~20자입니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 6, max = 30, message = "비밀번호는 6~30자입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임을 입력하세요.")
    @Size(min = 2, max = 12, message = "닉네임은 2~12자입니다.")
    private String nickname;

    @Size(max = 50, message = "주 게임은 50자 이내입니다.")
    private String mainGame;
}
