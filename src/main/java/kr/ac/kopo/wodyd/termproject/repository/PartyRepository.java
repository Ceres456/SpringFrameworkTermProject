package kr.ac.kopo.wodyd.termproject.repository;

import jakarta.persistence.LockModeType;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
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
     * 파티 찾기용. status 가 null 이면 모든 상태, 아니면 그 상태만. 정렬은 pageable 의 Sort 를 따른다(최신순).
     */
    @EntityGraph(attributePaths = {"game", "host"})
    @Query("select p from Party p " +
            "where (:gameId is null or p.game.id = :gameId) " +
            "and (:status is null or p.status = :status) " +
            "and p.title like concat('%', :keyword, '%')")
    Page<Party> searchFiltered(@Param("gameId") Long gameId, @Param("status") PartyStatus status,
                               @Param("keyword") String keyword, Pageable pageable);

    /**
     * 남은 자리(정원 - 승인된 인원)가 많은 순. 모집 중인 파티를 먼저, 같으면 최신순.
     * pageable 은 정렬 없이 넘긴다.
     */
    @EntityGraph(attributePaths = {"game", "host"})
    @Query(value = "select p from Party p " +
            "where (:gameId is null or p.game.id = :gameId) " +
            "and (:status is null or p.status = :status) " +
            "and p.title like concat('%', :keyword, '%') " +
            "order by case when p.status = kr.ac.kopo.wodyd.termproject.domain.PartyStatus.RECRUITING then 0 else 1 end, " +
            "(p.maxMembers - (select count(pm) from PartyMember pm " +
            "where pm.party = p and pm.status = kr.ac.kopo.wodyd.termproject.domain.ApplyStatus.APPROVED)) desc, " +
            "p.createdAt desc",
            countQuery = "select count(p) from Party p " +
                    "where (:gameId is null or p.game.id = :gameId) " +
                    "and (:status is null or p.status = :status) " +
                    "and p.title like concat('%', :keyword, '%')")
    Page<Party> searchByMostSlots(@Param("gameId") Long gameId, @Param("status") PartyStatus status,
                                  @Param("keyword") String keyword, Pageable pageable);

    /**
     * 마감 임박순: 모집 중인 파티를 먼저, 그 안에서 남은 자리가 적은 순. 같으면 최신순.
     * (인원 마감/종료 파티는 뒤로 간다.)
     */
    @EntityGraph(attributePaths = {"game", "host"})
    @Query(value = "select p from Party p " +
            "where (:gameId is null or p.game.id = :gameId) " +
            "and (:status is null or p.status = :status) " +
            "and p.title like concat('%', :keyword, '%') " +
            "order by case when p.status = kr.ac.kopo.wodyd.termproject.domain.PartyStatus.RECRUITING then 0 else 1 end, " +
            "(p.maxMembers - (select count(pm) from PartyMember pm " +
            "where pm.party = p and pm.status = kr.ac.kopo.wodyd.termproject.domain.ApplyStatus.APPROVED)) asc, " +
            "p.createdAt desc",
            countQuery = "select count(p) from Party p " +
                    "where (:gameId is null or p.game.id = :gameId) " +
                    "and (:status is null or p.status = :status) " +
                    "and p.title like concat('%', :keyword, '%')")
    Page<Party> searchByEndingSoon(@Param("gameId") Long gameId, @Param("status") PartyStatus status,
                                   @Param("keyword") String keyword, Pageable pageable);

    /** 관리자: 상태(선택)까지 걸러서 검색. status 가 null 이면 모든 상태. */
    @EntityGraph(attributePaths = {"game", "host"})
    @Query("select p from Party p " +
            "where (:status is null or p.status = :status) " +
            "and p.title like concat('%', :keyword, '%')")
    Page<Party> searchByStatus(@Param("status") PartyStatus status, @Param("keyword") String keyword, Pageable pageable);

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

    /** 관리자 대시보드: 상태별 파티 수 */
    long countByStatus(PartyStatus status);
}
