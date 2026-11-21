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

    public void approve() {
        this.status = ApplyStatus.APPROVED;
    }

    public void reject() {
        this.status = ApplyStatus.REJECTED;
    }
}
