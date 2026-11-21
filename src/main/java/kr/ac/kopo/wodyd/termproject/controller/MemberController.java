package kr.ac.kopo.wodyd.termproject.controller;

import jakarta.validation.Valid;
import kr.ac.kopo.wodyd.termproject.dto.SignupForm;
import kr.ac.kopo.wodyd.termproject.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid SignupForm signupForm, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("passwordConfirm")
                && !signupForm.getPassword().equals(signupForm.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (!bindingResult.hasFieldErrors("username") && memberService.isUsernameTaken(signupForm.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
        }
        if (!bindingResult.hasFieldErrors("nickname") && memberService.isNicknameTaken(signupForm.getNickname())) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
        }
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        memberService.signup(signupForm);
        return "redirect:/login?signup";
    }
}
