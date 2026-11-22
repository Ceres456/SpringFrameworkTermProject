package kr.ac.kopo.wodyd.termproject.web;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.service.NotificationService;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 화면 요청 단위의 시험: 접근 권한(Spring Security), 파티 찾기 파라미터, 알림 배지. */
@AutoConfigureMockMvc
@DisplayName("웹 요청 흐름")
class WebFlowTest extends ServiceTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("로그인하지 않아도 파티 찾기는 볼 수 있다")
    void partyListIsPublic() throws Exception {
        mvc.perform(get("/parties")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인이 필요한 화면은 로그인 화면으로 보낸다")
    void protectedPagesRedirectToLogin() throws Exception {
        mvc.perform(get("/mypage")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/parties/new")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("관리자 화면: 일반 회원은 403, 관리자는 열린다")
    void adminPagesNeedAdminRole() throws Exception {
        Member user = member("user");
        Member admin = member("admin", Role.ADMIN);

        mvc.perform(get("/admin").with(user(user.getUsername()).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/members").with(user(user.getUsername()).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/parties").with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST 요청은 CSRF 토큰이 없으면 거부된다")
    void postRequiresCsrf() throws Exception {
        Member host = member("host");
        Party party = party(host, 4);
        Member guest = member("guest");

        mvc.perform(post("/parties/" + party.getId() + "/apply").with(user(guest.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        assertThat(partyMemberRepository.existsByPartyAndMember(party, guest)).isFalse();
    }

    @Test
    @DisplayName("없는 파티는 404 화면으로 안내한다")
    void unknownPartyIs404() throws Exception {
        mvc.perform(get("/parties/99999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("파티 찾기: 모집중만 보기와 정렬 값을 화면에 그대로 전달한다")
    void partyListKeepsFilterAndSort() throws Exception {
        mvc.perform(get("/parties").param("open", "true").param("sort", "ending"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("open", true))
                .andExpect(model().attribute("sort", "ending"));

        // 최신순은 기본값이라 주소에 붙이지 않는다 / 모르는 값은 최신순
        mvc.perform(get("/parties").param("sort", "bogus"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("open", false))
                .andExpect(model().attribute("sort", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("신청하면 방장에게, 승인되면 신청자에게 헤더 배지가 보이고 읽음 처리하면 사라진다")
    void notificationBadgeLifecycle() throws Exception {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);

        // 1) 신청 (화면 요청으로)
        mvc.perform(post("/parties/" + party.getId() + "/apply").with(csrf()).with(user(guest.getUsername()).roles("USER")))
                .andExpect(status().is3xxRedirection());
        assertThat(entry(party, guest).getStatus()).isEqualTo(ApplyStatus.PENDING);

        // 2) 방장 화면 헤더에 배지가 보인다
        mvc.perform(get("/parties").with(user(host.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notif-badge")));
        // 신청자는 아직 알림이 없다
        mvc.perform(get("/parties").with(user(guest.getUsername()).roles("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("notif-badge"))));

        // 3) 방장이 승인하면 신청자에게 배지가 생긴다
        Long applicationId = entry(party, guest).getId();
        mvc.perform(post("/parties/" + party.getId() + "/applications/" + applicationId + "/approve")
                        .with(csrf()).with(user(host.getUsername()).roles("USER")))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/parties").with(user(guest.getUsername()).roles("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("notif-badge")));
        mvc.perform(get("/mypage").with(user(guest.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("notices"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("새 소식")));

        // 4) 읽음 처리하면 사라진다
        mvc.perform(post("/mypage/notifications/read").with(csrf()).with(user(guest.getUsername()).roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));
        assertThat(notificationService.count(guest.getUsername())).isZero();
        mvc.perform(get("/parties").with(user(guest.getUsername()).roles("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("notif-badge"))));
    }
}
