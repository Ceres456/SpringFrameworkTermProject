package kr.ac.kopo.wodyd.termproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    /** 주로 하는 게임 (자기소개용, 선택) */
    @Column(length = 50)
    private String mainGame;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    /** 파티원 리뷰로 계산되는 평균 매너 점수 (0.0 ~ 5.0) */
    @Column(nullable = false)
    private double mannerScore;

    @Column(nullable = false)
    private int reviewCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Member(String username, String password, String nickname, String mainGame, Role role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.mainGame = mainGame;
        this.role = role == null ? Role.USER : role;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 주로 하는 게임을 바꾼다. null 이면 등록하지 않은 상태로 돌린다. */
    public void changeMainGame(String mainGame) {
        this.mainGame = mainGame;
    }

    /** 닉네임을 바꾼다. */
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 암호화된 비밀번호로 바꾼다. (관리자 계정을 설정 파일의 비밀번호에 맞출 때 쓴다) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 관리자 기능: 권한을 바꾼다. */
    public void changeRole(Role role) {
        this.role = role;
    }

    /** 리뷰가 지워졌을 때 남은 리뷰 기준으로 매너 점수를 다시 정한다. */
    public void resetReviewStats(double mannerScore, int reviewCount) {
        this.mannerScore = mannerScore;
        this.reviewCount = reviewCount;
    }

    /** 리뷰 1건이 등록될 때 평균 매너 점수를 갱신한다. */
    public void applyReview(int rating) {
        double total = this.mannerScore * this.reviewCount + rating;
        this.reviewCount++;
        this.mannerScore = Math.round(total / this.reviewCount * 10) / 10.0;
    }
}
