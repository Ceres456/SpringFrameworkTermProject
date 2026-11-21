package kr.ac.kopo.wodyd.termproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 파티 종료 후 파티원끼리 남기는 매너 평가 (같은 파티·같은 대상에 1회) */
@Entity
@Table(name = "party_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "reviewer_id", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    /** 평가하는 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id")
    private Member reviewer;

    /** 평가받는 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private Member target;

    /** 매너 점수 1~5 */
    @Column(nullable = false)
    private int rating;

    @Column(length = 200)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PartyReview(Party party, Member reviewer, Member target, int rating, String comment) {
        this.party = party;
        this.reviewer = reviewer;
        this.target = target;
        this.rating = rating;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
