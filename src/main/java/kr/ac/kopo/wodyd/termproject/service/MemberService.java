package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.dto.MemberEditForm;
import kr.ac.kopo.wodyd.termproject.dto.SignupForm;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isUsernameTaken(String username) {
        return memberRepository.existsByUsername(username);
    }

    public boolean isNicknameTaken(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional
    public Long signup(SignupForm form) {
        Member member = Member.builder()
                .username(form.getUsername())
                .password(passwordEncoder.encode(form.getPassword()))
                .nickname(form.getNickname())
                .mainGame(form.getMainGame() == null || form.getMainGame().isBlank() ? null : form.getMainGame())
                .build();
        return memberRepository.save(member).getId();
    }

    // ---------- 정보 수정 (마이페이지 / 관리자 공용) ----------

    public Member getByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    /** 내가 아닌 다른 회원이 이미 쓰는 닉네임인가 (내 닉네임을 그대로 두는 것은 괜찮다) */
    public boolean isNicknameTakenByOther(String nickname, Long memberId) {
        return memberRepository.existsByNicknameAndIdNot(nickname, memberId);
    }

    /** 등록된 게임 이름인가 */
    public boolean isKnownGame(String gameName) {
        return gameRepository.findByName(gameName).isPresent();
    }

    /** 입력한 비밀번호가 그 회원의 현재 비밀번호와 같은가 */
    public boolean passwordMatches(Long memberId, String rawPassword) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, getById(memberId).getPassword());
    }

    /**
     * 닉네임, 주 게임, (입력했다면) 비밀번호를 바꾼다. 검증(중복 닉네임, 현재 비밀번호 확인, 새 비밀번호 일치)은
     * 호출하는 쪽에서 끝낸 뒤 부른다. 주 게임을 비우면 등록하지 않은 상태가 된다.
     */
    @Transactional
    public void updateProfile(Long memberId, MemberEditForm form) {
        Member member = getById(memberId);
        member.changeNickname(form.getNickname().trim());

        String gameName = form.getMainGame();
        if (gameName == null || gameName.isBlank()) {
            member.changeMainGame(null);
        } else {
            Game game = gameRepository.findByName(gameName)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임입니다."));
            member.changeMainGame(game.getName());
        }

        if (form.wantsNewPassword()) {
            member.changePassword(passwordEncoder.encode(form.getNewPassword()));
        }
    }
}
