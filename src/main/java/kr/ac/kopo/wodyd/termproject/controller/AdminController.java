package kr.ac.kopo.wodyd.termproject.controller;

import jakarta.validation.Valid;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.dto.MemberEditForm;
import kr.ac.kopo.wodyd.termproject.service.AdminService;
import kr.ac.kopo.wodyd.termproject.service.MemberEditValidator;
import kr.ac.kopo.wodyd.termproject.service.MemberService;
import kr.ac.kopo.wodyd.termproject.service.MyPageService;
import kr.ac.kopo.wodyd.termproject.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.function.Supplier;

/**
 * 관리자 페이지. /admin/** 은 SecurityConfig 에서 ADMIN 권한만 들어올 수 있게 막는다.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PartyService partyService;
    private final MyPageService myPageService;
    private final MemberService memberService;
    private final MemberEditValidator memberEditValidator;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.stats());
        model.addAttribute("recentMembers", adminService.recentMembers());
        model.addAttribute("recentParties", partyService.search(null, "", 0).getContent().stream().limit(5).toList());
        return "admin/dashboard";
    }

    // ---------- 회원 ----------

    @GetMapping("/members")
    public String members(@RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "0") int page,
                          Principal principal, Model model) {
        model.addAttribute("members", adminService.members(keyword, page));
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("me", principal.getName());
        return "admin/members";
    }

    /** 회원 한 명의 정보: 가입 정보, 만든 파티, 신청 내역, 받은 리뷰 */
    @GetMapping("/members/{id}")
    public String memberDetail(@PathVariable Long id, Principal principal, Model model) {
        Member member = adminService.member(id);
        model.addAttribute("member", member);
        model.addAttribute("profile", myPageService.getMyPage(member.getUsername()));
        model.addAttribute("writtenCount", adminService.writtenReviewCount(member));
        model.addAttribute("self", member.getUsername().equals(principal.getName()));
        return "admin/member-detail";
    }

    /** 관리자가 회원 정보(닉네임, 주 게임, 비밀번호 재설정)를 수정하는 화면 */
    @GetMapping("/members/{id}/edit")
    public String memberEditForm(@PathVariable Long id, Model model) {
        Member member = memberService.getById(id);
        model.addAttribute("memberEditForm", MemberEditForm.of(member));
        populateEdit(member, model);
        return "admin/member-edit";
    }

    @PostMapping("/members/{id}/edit")
    public String memberEdit(@PathVariable Long id,
                             @Valid @ModelAttribute("memberEditForm") MemberEditForm form, BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes) {
        Member member = memberService.getById(id);
        // 관리자는 현재 비밀번호를 모르는 회원의 비밀번호도 새로 정해 줄 수 있다
        memberEditValidator.validate(form, id, false, bindingResult);
        if (bindingResult.hasErrors()) {
            populateEdit(member, model);
            return "admin/member-edit";
        }
        memberService.updateProfile(id, form);
        redirectAttributes.addFlashAttribute("message",
                form.getNickname().trim() + "님의 정보를 수정했습니다."
                        + (form.wantsNewPassword() ? " 비밀번호도 새로 정했어요." : ""));
        return "redirect:/admin/members/" + id;
    }

    private void populateEdit(Member member, Model model) {
        model.addAttribute("member", member);
        model.addAttribute("games", partyService.findGames());
    }

    @PostMapping("/members/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String back,
                             Principal principal, RedirectAttributes redirectAttributes) {
        // 상세 화면에서 눌렀다면 상세 화면으로 돌아간다
        String listPath = "detail".equals(back) ? "/admin/members/" + id : "/admin/members";
        return act(redirectAttributes, listPath, keyword, page, () -> {
            var member = adminService.changeRole(id, role, principal.getName());
            return member.getNickname() + "님을 " + (role == Role.ADMIN ? "관리자" : "일반 회원") + "(으)로 바꿨습니다.";
        });
    }

    @PostMapping("/members/{id}/delete")
    public String deleteMember(@PathVariable Long id,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               Principal principal, RedirectAttributes redirectAttributes) {
        return act(redirectAttributes, "/admin/members", keyword, page,
                () -> adminService.deleteMember(id, principal.getName()) + "님을 삭제했습니다.");
    }

    // ---------- 파티 ----------

    @GetMapping("/parties")
    public String parties(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) PartyStatus status,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        model.addAttribute("parties", partyService.searchByStatus(status, kw, page));
        model.addAttribute("keyword", kw == null ? "" : kw);
        model.addAttribute("status", status);        // null 이면 "전체"
        model.addAttribute("stats", adminService.stats());   // 상태 버튼에 붙일 개수
        return "admin/parties";
    }

    @PostMapping("/parties/{id}/close")
    public String closeParty(@PathVariable Long id,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) PartyStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             RedirectAttributes redirectAttributes) {
        keepStatus(redirectAttributes, status);
        return act(redirectAttributes, "/admin/parties", keyword, page,
                () -> "'" + adminService.forceClose(id) + "' 파티를 종료했습니다.");
    }

    @PostMapping("/parties/{id}/delete")
    public String deleteParty(@PathVariable Long id,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) PartyStatus status,
                              @RequestParam(defaultValue = "0") int page,
                              RedirectAttributes redirectAttributes) {
        keepStatus(redirectAttributes, status);
        return act(redirectAttributes, "/admin/parties", keyword, page,
                () -> "'" + adminService.deleteParty(id) + "' 파티를 삭제했습니다.");
    }

    /** 작업 뒤에도 보고 있던 상태 탭이 유지되도록 돌아갈 주소에 status 를 붙인다. */
    private void keepStatus(RedirectAttributes redirectAttributes, PartyStatus status) {
        if (status != null) {
            redirectAttributes.addAttribute("status", status.name());
        }
    }

    // ---------- 리뷰 ----------

    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reviews", adminService.reviews(page));
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes redirectAttributes) {
        return act(redirectAttributes, "/admin/reviews", null, page,
                () -> adminService.deleteReview(id) + "님이 받은 리뷰를 삭제했습니다. 매너 점수를 다시 계산했어요.");
    }

    /** 작업을 실행하고 결과(성공/업무 규칙 위반)를 안내 메시지로 담아 원래 목록으로 돌려보낸다. */
    private String act(RedirectAttributes redirectAttributes, String listPath, String keyword, int page,
                       Supplier<String> action) {
        try {
            redirectAttributes.addFlashAttribute("message", action.get());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (keyword != null && !keyword.isBlank()) {
            redirectAttributes.addAttribute("keyword", keyword);
        }
        if (page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
        return "redirect:" + listPath;
    }
}
