package kr.ac.kopo.wodyd.termproject.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.dto.MemberEditForm;
import kr.ac.kopo.wodyd.termproject.service.MemberEditValidator;
import kr.ac.kopo.wodyd.termproject.service.MemberService;
import kr.ac.kopo.wodyd.termproject.service.MyPageService;
import kr.ac.kopo.wodyd.termproject.service.NotificationService;
import kr.ac.kopo.wodyd.termproject.service.PartyService;
import kr.ac.kopo.wodyd.termproject.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final MemberService memberService;
    private final MemberEditValidator memberEditValidator;
    private final PartyService partyService;
    private final WithdrawalService withdrawalService;
    private final NotificationService notificationService;

    /** 로그인한 사용자만 접근 (SecurityConfig 의 anyRequest().authenticated()) */
    @GetMapping("/mypage")
    public String myPage(Principal principal, Model model) {
        model.addAttribute("me", myPageService.getMyPage(principal.getName()));
        model.addAttribute("notices", notificationService.list(principal.getName()));
        return "mypage";
    }

    /** '새 소식'의 승인/거절 결과를 모두 읽음 처리한다 */
    @PostMapping("/mypage/notifications/read")
    public String readNotifications(Principal principal) {
        notificationService.markResultsSeen(principal.getName());
        return "redirect:/mypage";
    }

    /** 회원 탈퇴 화면: 무엇이 함께 지워지는지 알려 주고 비밀번호를 한 번 더 확인한다 */
    @GetMapping("/mypage/withdraw")
    public String withdrawForm(Principal principal, Model model) {
        model.addAttribute("me", myPageService.getMyPage(principal.getName()));
        return "mypage-withdraw";
    }

    @PostMapping("/mypage/withdraw")
    public String withdraw(@RequestParam(required = false) String password,
                           Authentication authentication,
                           HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            withdrawalService.withdraw(authentication.getName(), password);
        } catch (IllegalStateException | IllegalArgumentException e) {
            model.addAttribute("me", myPageService.getMyPage(authentication.getName()));
            model.addAttribute("error", e.getMessage());
            return "mypage-withdraw";
        }
        // 회원이 사라졌으니 로그인 상태(세션)도 끝낸다
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?withdrawn";
    }

    /** 내 정보 수정 화면: 닉네임, 주 게임, 비밀번호 */
    @GetMapping("/mypage/edit")
    public String editForm(Principal principal, Model model) {
        Member member = memberService.getByUsername(principal.getName());
        model.addAttribute("memberEditForm", MemberEditForm.of(member));
        populate(member, model);
        return "mypage-edit";
    }

    @PostMapping("/mypage/edit")
    public String edit(@Valid @ModelAttribute("memberEditForm") MemberEditForm form, BindingResult bindingResult,
                       Principal principal, Model model, RedirectAttributes redirectAttributes) {
        Member member = memberService.getByUsername(principal.getName());
        // 비밀번호를 바꿀 때는 현재 비밀번호를 확인한다
        memberEditValidator.validate(form, member.getId(), true, bindingResult);
        if (bindingResult.hasErrors()) {
            populate(member, model);
            return "mypage-edit";
        }
        memberService.updateProfile(member.getId(), form);
        redirectAttributes.addFlashAttribute("message",
                form.wantsNewPassword() ? "내 정보를 수정하고 비밀번호를 바꿨습니다." : "내 정보를 수정했습니다.");
        return "redirect:/mypage";
    }

    private void populate(Member member, Model model) {
        model.addAttribute("username", member.getUsername());
        model.addAttribute("joinedAt", member.getCreatedAt());
        model.addAttribute("games", partyService.findGames());
    }
}
