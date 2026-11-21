package kr.ac.kopo.wodyd.termproject.controller;

import jakarta.validation.Valid;
import kr.ac.kopo.wodyd.termproject.dto.PartyForm;
import kr.ac.kopo.wodyd.termproject.dto.PartySummary;
import kr.ac.kopo.wodyd.termproject.service.ParticipationService;
import kr.ac.kopo.wodyd.termproject.service.PartyService;
import kr.ac.kopo.wodyd.termproject.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;
    private final ParticipationService participationService;
    private final ReviewService reviewService;

    @GetMapping
    public String list(@RequestParam(required = false) Long gameId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<PartySummary> parties = partyService.search(gameId, kw, page);

        model.addAttribute("parties", parties);
        model.addAttribute("games", partyService.findGames());
        model.addAttribute("gameId", gameId);
        model.addAttribute("keyword", kw);
        return "parties/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        String username = principal == null ? null : principal.getName();
        model.addAttribute("party", partyService.getDetail(id, username));
        return "parties/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("partyForm", new PartyForm());
        populateFormModel(model, "파티 만들기", "/parties");
        return "parties/form";
    }

    @PostMapping
    public String create(@Valid PartyForm partyForm, BindingResult bindingResult,
                         Principal principal, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                Long id = partyService.create(partyForm, principal.getName());
                return "redirect:/parties/" + id;
            } catch (IllegalArgumentException e) {
                bindingResult.reject("invalid", e.getMessage());
            }
        }
        populateFormModel(model, "파티 만들기", "/parties");
        return "parties/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("partyForm", partyService.getForm(id, principal.getName()));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/parties/" + id;
        }
        populateFormModel(model, "파티 수정", "/parties/" + id + "/edit");
        return "parties/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid PartyForm partyForm, BindingResult bindingResult,
                       Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                partyService.update(id, partyForm, principal.getName());
                redirectAttributes.addFlashAttribute("message", "파티 정보를 수정했습니다.");
                return "redirect:/parties/" + id;
            } catch (IllegalArgumentException e) {
                bindingResult.reject("invalid", e.getMessage());
            } catch (IllegalStateException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/parties/" + id;
            }
        }
        populateFormModel(model, "파티 수정", "/parties/" + id + "/edit");
        return "parties/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            partyService.delete(id, principal.getName());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/parties/" + id;
        }
        redirectAttributes.addFlashAttribute("message", "파티를 삭제했습니다.");
        return "redirect:/parties";
    }

    // ---------- 참가 신청 ----------

    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "참가 신청을 보냈습니다. 방장의 승인을 기다려 주세요.",
                () -> participationService.apply(id, principal.getName()));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "참가 신청을 취소했습니다.",
                () -> participationService.cancel(id, principal.getName()));
    }

    @PostMapping("/{id}/applications/{applicationId}/approve")
    public String approve(@PathVariable Long id, @PathVariable Long applicationId,
                          Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "신청을 승인했습니다.",
                () -> participationService.approve(id, applicationId, principal.getName()));
    }

    @PostMapping("/{id}/applications/{applicationId}/reject")
    public String reject(@PathVariable Long id, @PathVariable Long applicationId,
                         Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "신청을 거절했습니다.",
                () -> participationService.reject(id, applicationId, principal.getName()));
    }

    // ---------- 파티 종료 / 리뷰 ----------

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "파티를 종료했습니다. 이제 파티원에게 매너 리뷰를 남길 수 있어요.",
                () -> partyService.close(id, principal.getName()));
    }

    @PostMapping("/{id}/reviews")
    public String writeReview(@PathVariable Long id,
                              @RequestParam Long participantId,
                              @RequestParam int rating,
                              @RequestParam(required = false) String comment,
                              Principal principal, RedirectAttributes redirectAttributes) {
        return handle(id, redirectAttributes, "리뷰를 남겼습니다.",
                () -> reviewService.write(id, principal.getName(), participantId, rating, comment));
    }

    /** 서비스 호출 결과(성공/업무 규칙 위반)를 안내 메시지로 담아 상세 화면으로 돌려보낸다. */
    private String handle(Long partyId, RedirectAttributes redirectAttributes,
                          String successMessage, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parties/" + partyId;
    }

    private void populateFormModel(Model model, String formTitle, String formAction) {
        model.addAttribute("games", partyService.findGames());
        model.addAttribute("formTitle", formTitle);
        model.addAttribute("formAction", formAction);
    }
}
