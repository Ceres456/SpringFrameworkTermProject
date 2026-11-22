package kr.ac.kopo.wodyd.termproject.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.kopo.wodyd.termproject.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** 모든 화면의 헤더에서 쓰는 '새 소식' 개수를 모델에 넣는다. 로그인하지 않았으면 넣지 않는다. */
@ControllerAdvice
@RequiredArgsConstructor
public class NotificationAdvice {

    private final NotificationService notificationService;

    @ModelAttribute("notificationCount")
    public Long notificationCount(Principal principal, HttpServletRequest request) {
        // 화면을 그리는 GET 요청에서만 센다 (POST 는 곧 리다이렉트되므로 낭비)
        if (principal == null || !"GET".equals(request.getMethod())) {
            return null;
        }
        return notificationService.count(principal.getName());
    }
}
