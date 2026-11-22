package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원 본인의 탈퇴. 비밀번호를 다시 확인한 뒤, 관리자 삭제와 같은 정리 로직으로
 * 회원과 딸린 데이터를 모두 지운다. (관리자 계정은 탈퇴할 수 없다)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;

    /**
     * @throws IllegalStateException    관리자 계정일 때
     * @throws IllegalArgumentException 비밀번호가 맞지 않을 때
     */
    public void withdraw(String username, String rawPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        if (member.getRole() == Role.ADMIN) {
            throw new IllegalStateException("관리자 계정은 탈퇴할 수 없어요. 먼저 일반 회원으로 바꿔 주세요.");
        }
        if (rawPassword == null || rawPassword.isEmpty()
                || !passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 맞지 않아요.");
        }
        adminService.purgeMember(member);
    }
}
