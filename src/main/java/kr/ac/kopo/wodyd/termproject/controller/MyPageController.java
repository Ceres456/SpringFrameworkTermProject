package kr.ac.kopo.wodyd.termproject.controller;

import kr.ac.kopo.wodyd.termproject.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /** 로그인한 사용자만 접근 (SecurityConfig 의 anyRequest().authenticated()) */
    @GetMapping("/mypage")
    public String myPage(Principal principal, Model model) {
        model.addAttribute("me", myPageService.getMyPage(principal.getName()));
        return "mypage";
    }
}
