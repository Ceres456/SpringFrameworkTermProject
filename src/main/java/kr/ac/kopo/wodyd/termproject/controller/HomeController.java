package kr.ac.kopo.wodyd.termproject.controller;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.dto.PartySummary;
import kr.ac.kopo.wodyd.termproject.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequiredArgsConstructor
public class HomeController {

    /** 화면에 고정된 흐름의 줄 수. 첫 화면에서는 전체가, 두 번째 화면부터는 양쪽 끝만 보인다. */
    private static final int STRIP_ROWS = 5;
    private static final int OPEN_PARTY_LIMIT = 3;

    private final PartyService partyService;

    @GetMapping("/")
    public String home(Model model) {
        List<Game> games = partyService.findGames();
        model.addAttribute("games", games);
        model.addAttribute("gameCount", games.size());
        model.addAttribute("edgeRows", driftRows(games, STRIP_ROWS));
        model.addAttribute("openParties", recentOpenParties());
        return "index";
    }

    /** 마무리 화면에 보여줄, 지금 모집 중인 최신 파티 (없으면 빈 목록) */
    private List<PartySummary> recentOpenParties() {
        return partyService.search(null, "", 0).getContent().stream()
                .filter(p -> p.getStatus() == PartyStatus.RECRUITING)
                .limit(OPEN_PARTY_LIMIT)
                .toList();
    }

    /**
     * 흐르는 게임 이미지 줄. 전체 게임을 한 번 섞은 뒤 줄에 번갈아 나눠 담는다.
     * 각 게임은 한 줄에만 들어가므로 같은 게임이 두 줄에 동시에 보이지 않고,
     * 줄마다 담기는 게임과 순서가 모두 다르다. (방문할 때마다 새로 섞인다.)
     */
    private List<List<Game>> driftRows(List<Game> games, int rowCount) {
        List<List<Game>> rows = new ArrayList<>();
        if (games.isEmpty()) {
            return rows;
        }
        List<Game> shuffled = new ArrayList<>(games);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        for (int i = 0; i < rowCount; i++) {
            rows.add(new ArrayList<>());
        }
        for (int i = 0; i < shuffled.size(); i++) {
            rows.get(i % rowCount).add(shuffled.get(i));
        }
        // 게임이 줄 수보다 적어 빈 줄이 생기면 화면이 비므로, 빈 줄은 앞 줄을 그대로 쓴다.
        for (int i = 0; i < rowCount; i++) {
            if (rows.get(i).isEmpty()) {
                rows.set(i, new ArrayList<>(rows.get(0)));
            }
        }
        return rows;
    }
}
