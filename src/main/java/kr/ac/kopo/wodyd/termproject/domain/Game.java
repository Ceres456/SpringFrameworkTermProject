package kr.ac.kopo.wodyd.termproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 30)
    private String genre;

    /**
     * 이미지 파일 이름(확장자 제외). 화면에서는 /images/games/{slug}.jpg 를 읽고,
     * 파일이 없으면 게임별 색상 카드로 대체한다.
     */
    @Column(length = 50)
    private String slug;

    @Builder
    private Game(String name, String genre, String slug) {
        this.name = name;
        this.genre = genre;
        this.slug = slug;
    }

    public void updateInfo(String genre, String slug) {
        this.genre = genre;
        this.slug = slug;
    }
}
