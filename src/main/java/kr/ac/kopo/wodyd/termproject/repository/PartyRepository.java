package kr.ac.kopo.wodyd.termproject.repository;

import jakarta.persistence.LockModeType;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    /** 게임(선택) + 제목 키워드로 검색. game/host 를 함께 읽어 N+1 을 막는다. */
    @EntityGraph(attributePaths = {"game", "host"})
    @Query("select p from Party p " +
            "where (:gameId is null or p.game.id = :gameId) " +
            "and p.title like concat('%', :keyword, '%')")
    Page<Party> search(@Param("gameId") Long gameId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 승인/취소처럼 정원을 바꾸는 작업용. 행에 쓰기 잠금을 걸어서
     * 방장이 동시에 두 명을 승인해도 정원을 넘지 않게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Party p where p.id = :id")
    Optional<Party> findByIdForUpdate(@Param("id") Long id);

    /** 마이페이지: 내가 만든 파티 (최신순) */
    @EntityGraph(attributePaths = "game")
    List<Party> findByHostOrderByCreatedAtDesc(Member host);
}
