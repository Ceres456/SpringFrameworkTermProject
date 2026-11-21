package kr.ac.kopo.wodyd.termproject.config;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기본 게임 목록을 보장한다. 이미 있는 게임은 장르/이미지 이름만 맞춰 갱신하고,
 * 없는 게임은 새로 넣는다. (이미지 파일: static/images/games/{slug}.jpg)
 */
@Component
@RequiredArgsConstructor
public class GameDataInitializer implements CommandLineRunner {

    private record Seed(String name, String genre, String slug) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("리그 오브 레전드", "AOS", "lol"),
            new Seed("발로란트", "FPS", "valorant"),
            new Seed("오버워치 2", "FPS", "overwatch"),
            new Seed("배틀그라운드", "FPS", "pubg"),
            new Seed("서든어택", "FPS", "sudden-attack"),
            new Seed("로스트아크", "MMORPG", "lostark"),
            new Seed("메이플스토리", "MMORPG", "maplestory"),
            new Seed("던전앤파이터", "액션 RPG", "dnf"),
            new Seed("마인크래프트", "샌드박스", "minecraft"),
            new Seed("FC 온라인", "스포츠", "fconline"),
            new Seed("발더스 게이트 3", "RPG", "bd3"),
            new Seed("데드락", "히어로 슈터", "deadlock")
    );

    private final GameRepository gameRepository;

    @Override
    public void run(String... args) {
        for (Seed seed : SEEDS) {
            Game game = gameRepository.findByName(seed.name())
                    .orElseGet(() -> Game.builder().name(seed.name()).build());
            game.updateInfo(seed.genre(), seed.slug());
            gameRepository.save(game);
        }
    }
}
