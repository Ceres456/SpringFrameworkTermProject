package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.dto.MemberEditForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

/**
 * 회원 정보 수정 폼의 업무 규칙 검증. 마이페이지(본인)와 관리자 페이지가 같은 규칙을 쓴다.
 * (칸 하나로 끝나는 형식 검사는 MemberEditForm 의 애노테이션이 맡는다.)
 */
@Component
@RequiredArgsConstructor
public class MemberEditValidator {

    private final MemberService memberService;

    /**
     * @param requireCurrentPassword true 면 비밀번호를 바꿀 때 현재 비밀번호를 확인한다 (본인 수정).
     *                               관리자가 다른 회원의 비밀번호를 재설정할 때는 false.
     */
    public void validate(MemberEditForm form, Long memberId, boolean requireCurrentPassword, BindingResult result) {
        if (!result.hasFieldErrors("nickname")
                && memberService.isNicknameTakenByOther(form.getNickname().trim(), memberId)) {
            result.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
        }

        String game = form.getMainGame();
        if (!result.hasFieldErrors("mainGame") && game != null && !game.isBlank()
                && !memberService.isKnownGame(game)) {
            result.rejectValue("mainGame", "unknown", "존재하지 않는 게임입니다.");
        }

        if (form.wantsNewPassword()) {
            if (!result.hasFieldErrors("newPassword")
                    && !form.getNewPassword().equals(form.getNewPasswordConfirm())) {
                result.rejectValue("newPasswordConfirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
            }
            if (requireCurrentPassword) {
                String current = form.getCurrentPassword();
                if (current == null || current.isEmpty()) {
                    result.rejectValue("currentPassword", "required", "현재 비밀번호를 입력하세요.");
                } else if (!memberService.passwordMatches(memberId, current)) {
                    result.rejectValue("currentPassword", "wrong", "현재 비밀번호가 맞지 않습니다.");
                }
            }
        }
    }
}
