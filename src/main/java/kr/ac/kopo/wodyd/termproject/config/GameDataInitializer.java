package kr.ac.kopo.wodyd.termproject.config;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기본 게임 목록을 보장한다. 이미 있는 게임은 장르/이미지 이름만 맞춰 갱신하고,
 * 없는 게임은 새로 넣는다. (이미지 파일: static/images/games/{slug}.jpg)
 */
@Component
@Order(1)
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
            new Seed("데드락", "히어로 슈터", "deadlock"),
            new Seed("에이펙스 레전드", "배틀로얄 FPS", "apexlegends"),
            new Seed("배틀필드 4", "FPS", "battlefield"),
            new Seed("콜 오브 듀티: 모던 워페어 4", "FPS", "codmodernwarfare4"),
            new Seed("카운터 스트라이크 2", "FPS", "cs2"),
            new Seed("엘든 링", "액션 RPG", "eldenring"),
            new Seed("레인보우 식스 시즈", "FPS", "r6siege"),
            new Seed("마블 라이벌즈", "히어로 슈터", "marvelrivals"),
            new Seed("헬다이버즈 2", "협동 슈터", "helldivers2"),
            new Seed("몬스터 헌터 와일즈", "협동 액션", "mhwilds"),
            new Seed("더 파이널스", "FPS", "thefinals"),
            new Seed("도타 2", "AOS", "dota2"),
            new Seed("리썰 컴퍼니", "공포 협동", "lethalcompany"),
            new Seed("팰월드", "생존 협동", "palworld"),
            new Seed("발헤임", "생존 협동", "valheim"),
            new Seed("테라리아", "샌드박스", "terraria"),
            new Seed("스트리트 파이터 6", "격투", "sf6"),
            new Seed("어몽 어스", "파티 게임", "amongus"),
            new Seed("파스모포비아", "공포 협동", "phasmophobia"),
            new Seed("R.E.P.O.", "공포 협동", "repo"),
            new Seed("델타 포스", "FPS", "deltaforce"),
            new Seed("러스트", "생존", "rust"),
            new Seed("데드 바이 데이라이트", "비대칭 공포", "dbd"),
            new Seed("레프트 4 데드 2", "협동 슈터", "l4d2"),
            new Seed("스타듀 밸리", "농장 협동", "stardew"),
            new Seed("패스 오브 엑자일 2", "액션 RPG", "poe2"),
            new Seed("철권 8", "격투", "tekken8"),
            new Seed("GTA 5", "오픈월드", "gta5")
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
