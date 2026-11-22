package kr.ac.kopo.wodyd.termproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원 정보 수정 폼. 마이페이지(본인)와 관리자 페이지가 함께 쓴다.
 * 비밀번호 칸은 바꿀 때만 채운다. 비워 두면 비밀번호는 그대로다.
 */
@Getter
@Setter
public class MemberEditForm {

    @NotBlank(message = "닉네임을 입력하세요.")
    @Size(min = 2, max = 12, message = "닉네임은 2~12자입니다.")
    private String nickname;

    /** 주로 하는 게임 이름 (선택). 비우면 등록하지 않은 상태가 된다. */
    @Size(max = 50, message = "주 게임은 50자 이내입니다.")
    private String mainGame;

    /** 본인이 비밀번호를 바꿀 때 확인용으로 받는다. (관리자 화면에서는 쓰지 않는다) */
    private String currentPassword;

    @Pattern(regexp = "^$|^.{6,30}$", message = "비밀번호는 6~30자입니다.")
    private String newPassword;

    private String newPasswordConfirm;

    public static MemberEditForm of(Member member) {
        MemberEditForm form = new MemberEditForm();
        form.setNickname(member.getNickname());
        form.setMainGame(member.getMainGame());
        return form;
    }

    public boolean wantsNewPassword() {
        return newPassword != null && !newPassword.isEmpty();
    }
}
