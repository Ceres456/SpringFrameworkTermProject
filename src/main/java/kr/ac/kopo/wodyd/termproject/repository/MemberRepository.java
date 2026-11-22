package kr.ac.kopo.wodyd.termproject.repository;

import jakarta.persistence.LockModeType;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    /** 정보 수정: 내가 아닌 다른 회원이 이미 쓰고 있는 닉네임인지 */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    /** 관리자: 아이디 또는 닉네임에 검색어가 들어간 회원 */
    Page<Member> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);

    /** 관리자 대시보드: 최근 가입 회원 */
    List<Member> findTop5ByOrderByCreatedAtDesc();

    long countByRole(Role role);

    /** 매너 점수 평균을 갱신할 때 두 리뷰가 겹쳐 덮어쓰지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}
