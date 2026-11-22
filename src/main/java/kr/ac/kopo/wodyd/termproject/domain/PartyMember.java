package kr.ac.kopo.wodyd.termproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Member 와 Party 의 N:M 관계를 풀어낸 참가 신청 엔티티 */
@Entity
@Table(name = "party_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApplyStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    /** 방장이 승인/거절한 시각 (알림용). 방장 처리를 거치지 않은 행은 null */
    @Column
    private LocalDateTime decidedAt;

    /**
     * 신청자가 처리 결과를 확인했는지. false 면 '새 소식'으로 보인다.
     * 방장 처리를 거치지 않은 행(기존 데이터 포함)은 null 이라 알림에 나오지 않는다.
     */
    @Column
    private Boolean resultSeen;

    @Builder
    private PartyMember(Party party, Member member) {
        this.party = party;
        this.member = member;
        this.status = ApplyStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.appliedAt = LocalDateTime.now();
    }

    /** 방장이 신청을 승인한다. 신청자에게 알림이 간다. */
    public void approve() {
        this.status = ApplyStatus.APPROVED;
        markDecided();
    }

    /** 방장이 신청을 거절한다. 신청자에게 알림이 간다. */
    public void reject() {
        this.status = ApplyStatus.REJECTED;
        markDecided();
    }

    /** 알림 없이 참가 처리한다. (방장 본인의 자동 참가, 더미 데이터) */
    public void joinQuietly() {
        this.status = ApplyStatus.APPROVED;
    }

    private void markDecided() {
        this.decidedAt = LocalDateTime.now();
        this.resultSeen = false;
    }
}
