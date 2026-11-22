package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.dto.MemberEditForm;
import kr.ac.kopo.wodyd.termproject.dto.SignupForm;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원가입 · 정보 수정")
class MemberServiceTest extends ServiceTestBase {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberEditValidator validator;

    private SignupForm signupForm(String username, String nickname) {
        SignupForm form = new SignupForm();
        form.setUsername(username);
        form.setPassword(PASSWORD);
        form.setPasswordConfirm(PASSWORD);
        form.setNickname(nickname);
        return form;
    }

    private MemberEditForm editForm(Member member) {
        return MemberEditForm.of(member);
    }

    private BindingResult validate(MemberEditForm form, Member member, boolean requireCurrentPassword) {
        BindingResult result = new BeanPropertyBindingResult(form, "memberEditForm");
        validator.validate(form, member.getId(), requireCurrentPassword, result);
        return result;
    }

    @Test
    @DisplayName("회원가입하면 일반 회원으로 저장되고, 비밀번호는 암호화된다")
    void signupStoresEncodedPassword() {
        String username = uniq("user");
        Long id = memberService.signup(signupForm(username, username));

        Member saved = memberService.getById(id);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2"); // BCrypt
        assertThat(memberService.passwordMatches(id, PASSWORD)).isTrue();
        assertThat(memberService.passwordMatches(id, "wrong")).isFalse();
        assertThat(memberService.isUsernameTaken(username)).isTrue();
        assertThat(memberService.isNicknameTaken(username)).isTrue();
        assertThat(memberService.isUsernameTaken("nobody-" + username)).isFalse();
    }

    @Test
    @DisplayName("주 게임을 비워서 가입하면 '등록하지 않음'(null)으로 저장된다")
    void blankMainGameBecomesNull() {
        SignupForm form = signupForm(uniq("user"), uniq("nick"));
        form.setMainGame("  ");

        Member saved = memberService.getById(memberService.signup(form));

        assertThat(saved.getMainGame()).isNull();
    }

    @Test
    @DisplayName("정보 수정: 닉네임·주 게임·비밀번호를 바꾼다")
    void updateProfileChangesFields() {
        Member user = member("user");
        Game game = game();
        MemberEditForm form = editForm(user);
        form.setNickname("  " + uniq("new") + " ");
        form.setMainGame(game.getName());
        form.setNewPassword("brand-new1");
        form.setNewPasswordConfirm("brand-new1");

        memberService.updateProfile(user.getId(), form);

        assertThat(user.getNickname()).isEqualTo(form.getNickname().trim());
        assertThat(user.getMainGame()).isEqualTo(game.getName());
        assertThat(memberService.passwordMatches(user.getId(), "brand-new1")).isTrue();
    }

    @Test
    @DisplayName("정보 수정: 비밀번호 칸을 비우면 비밀번호는 그대로, 주 게임을 비우면 등록 해제")
    void blankFieldsKeepPasswordAndClearGame() {
        Member user = member("user");
        user.changeMainGame("something");
        MemberEditForm form = editForm(user);
        form.setMainGame("");

        memberService.updateProfile(user.getId(), form);

        assertThat(user.getMainGame()).isNull();
        assertThat(memberService.passwordMatches(user.getId(), PASSWORD)).isTrue();
    }

    @Test
    @DisplayName("검증: 다른 사람이 쓰는 닉네임은 안 되지만, 내 닉네임을 그대로 두는 것은 된다")
    void nicknameMustBeUniqueExceptOwn() {
        Member me = member("me");
        Member other = member("other");

        MemberEditForm keep = editForm(me);
        assertThat(validate(keep, me, true).hasErrors()).isFalse();

        MemberEditForm steal = editForm(me);
        steal.setNickname(other.getNickname());
        BindingResult result = validate(steal, me, true);
        assertThat(result.hasFieldErrors("nickname")).isTrue();
    }

    @Test
    @DisplayName("검증: 등록되지 않은 게임은 주 게임으로 고를 수 없다")
    void mainGameMustExist() {
        Member me = member("me");
        MemberEditForm form = editForm(me);
        form.setMainGame("없는게임-" + uniq("x"));

        assertThat(validate(form, me, true).hasFieldErrors("mainGame")).isTrue();

        form.setMainGame(game().getName());
        assertThat(validate(form, me, true).hasFieldErrors("mainGame")).isFalse();
    }

    @Test
    @DisplayName("검증: 비밀번호를 바꾸려면 새 비밀번호 확인이 같아야 하고, 현재 비밀번호가 맞아야 한다")
    void passwordChangeRules() {
        Member me = member("me");

        MemberEditForm mismatch = editForm(me);
        mismatch.setCurrentPassword(PASSWORD);
        mismatch.setNewPassword("brand-new1");
        mismatch.setNewPasswordConfirm("different1");
        assertThat(validate(mismatch, me, true).hasFieldErrors("newPasswordConfirm")).isTrue();

        MemberEditForm noCurrent = editForm(me);
        noCurrent.setNewPassword("brand-new1");
        noCurrent.setNewPasswordConfirm("brand-new1");
        assertThat(validate(noCurrent, me, true).hasFieldErrors("currentPassword")).isTrue();

        MemberEditForm wrongCurrent = editForm(me);
        wrongCurrent.setCurrentPassword("wrong");
        wrongCurrent.setNewPassword("brand-new1");
        wrongCurrent.setNewPasswordConfirm("brand-new1");
        assertThat(validate(wrongCurrent, me, true).hasFieldErrors("currentPassword")).isTrue();

        MemberEditForm ok = editForm(me);
        ok.setCurrentPassword(PASSWORD);
        ok.setNewPassword("brand-new1");
        ok.setNewPasswordConfirm("brand-new1");
        assertThat(validate(ok, me, true).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("검증: 관리자가 다른 회원의 비밀번호를 재설정할 때는 현재 비밀번호를 묻지 않는다")
    void adminResetSkipsCurrentPassword() {
        Member target = member("target");
        MemberEditForm form = editForm(target);
        form.setNewPassword("reset-pass1");
        form.setNewPasswordConfirm("reset-pass1");

        assertThat(validate(form, target, false).hasErrors()).isFalse();
    }
}
