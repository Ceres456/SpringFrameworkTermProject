package kr.ac.kopo.wodyd.termproject.config;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정을 보장한다. 실행할 때마다 확인하고, 없으면 만든다.
 * <ul>
 *   <li>app.admin.username (기본 admin), app.admin.nickname (기본 "관리자")</li>
 *   <li>app.admin.password : 비밀번호는 소스에 두지 않는다. Git 에서 제외된 local-db.properties
 *       또는 환경변수 APP_ADMIN_PASSWORD 로 지정한다. 비어 있으면 관리자 계정을 만들지 않는다.</li>
 * </ul>
 * 이미 같은 아이디의 회원이 있으면 관리자 권한을 보장하고, 설정 파일의 비밀번호와 다르면 그 값으로 맞춘다.
 * (그래서 설정 파일의 app.admin.password 를 고치고 다시 실행하면 관리자 비밀번호가 바뀐다.)
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String username;

    @Value("${app.admin.nickname:관리자}")
    private String nickname;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        var existing = memberRepository.findByUsername(username);
        if (existing.isPresent()) {
            Member member = existing.get();
            if (member.getRole() != Role.ADMIN) {
                member.changeRole(Role.ADMIN);
                log.info("기존 회원 '{}' 에게 관리자 권한을 부여했습니다.", username);
            }
            // 설정 파일(local-db.properties)의 비밀번호가 곧 관리자 비밀번호다. 바꿨다면 다음 실행 때 반영한다.
            if (password != null && !password.isBlank() && !passwordEncoder.matches(password, member.getPassword())) {
                member.changePassword(passwordEncoder.encode(password));
                log.info("관리자 계정 '{}' 의 비밀번호를 설정 파일의 값으로 맞췄습니다.", username);
            }
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("app.admin.password 가 없어 관리자 계정('{}')을 만들지 않았습니다. "
                    + "local-db.properties 에 app.admin.password 를 넣으세요.", username);
            return;
        }
        if (memberRepository.existsByNickname(nickname)) {
            log.warn("닉네임 '{}' 이 이미 사용 중이라 관리자 계정을 만들지 못했습니다. app.admin.nickname 을 바꿔 주세요.", nickname);
            return;
        }
        memberRepository.save(Member.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .role(Role.ADMIN)
                .build());
        log.info("관리자 계정 '{}' 을 만들었습니다.", username);
    }
}
