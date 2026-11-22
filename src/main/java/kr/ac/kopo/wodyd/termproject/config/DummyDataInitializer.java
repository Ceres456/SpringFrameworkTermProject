package kr.ac.kopo.wodyd.termproject.config;

import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 개발/시연용 더미 데이터. 평소에는 꺼져 있고 {@code --app.seed-dummy=true} 로 실행했을 때만 동작한다.
 * <ul>
 *   <li>더미 회원 12명 (아이디 dummy01 ~ dummy12, 비밀번호는 {@link #DUMMY_PASSWORD})</li>
 *   <li>모집 중 / 마감 / 종료 상태의 파티 여러 개</li>
 *   <li>종료된 파티 일부에는 실제로 가입한 회원이 승인된 참가자로 들어가 있어, 리뷰 작성 기능을 바로 써 볼 수 있다</li>
 * </ul>
 * 이미 dummy01 이 있으면 아무것도 하지 않는다. 지우려면 dummy 회원과 그 파티·리뷰를 삭제하면 된다.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-dummy", havingValue = "true")
public class DummyDataInitializer implements CommandLineRunner {

    static final String DUMMY_PASSWORD = "dummy1234!";

    private record Spec(String game, String title, String content, int max, int joined,
                        boolean closed, boolean withRealMembers) {
    }

    private static final List<String> NICKNAMES = List.of(
            "새벽감성", "치킨먹고갈게", "탱커장인", "힐러는나", "뚝배기깨짐", "캐리해줄게",
            "오늘만산다", "야식요정", "쫄보원딜", "스위치온", "꿀잼러", "무지개빛탄창");

    private static final List<String> COMMENTS = List.of(
            "말이 잘 통하고 매너가 좋았어요!", "끝까지 같이 해줘서 고마웠어요.", "분위기 메이커였습니다.",
            "약속 시간을 잘 지켜요.", "초보에게도 친절하게 알려줬어요.", "다음에 또 같이 하고 싶어요.");

    private static final List<Spec> SPECS = List.of(
            // ---- 모집 중 ----
            new Spec("발로란트", "다이아 이상 랭크 5인큐 같이 돌리실 분", "저녁 9시~12시, 마이크 필수입니다. 분위기 좋게 해요.", 5, 2, false, false),
            new Spec("리그 오브 레전드", "듀오 랭크 서폿 구합니다 (골드~플래)", "원딜 메인이에요. 소통 잘 되는 분이면 좋겠어요.", 2, 1, false, false),
            new Spec("오버워치 2", "빠대 돌릴 파티원 구해요", "경쟁 말고 빠른 대전 가볍게 즐겨요.", 5, 3, false, false),
            new Spec("배틀그라운드", "스쿼드 치킨 먹으러 가실 분", "솔직히 실력은 중수입니다. 즐겜 위주!", 4, 2, false, false),
            new Spec("로스트아크", "금요일 군단장 레이드 팟 모집", "1580 이상, 디스코드 필수.", 8, 5, false, false),
            new Spec("메이플스토리", "보스 파티 같이 도실 분 (주간 보스)", "매주 목요일 저녁 진행합니다.", 6, 3, false, false),
            new Spec("마인크래프트", "야생 서버 건축 같이 하실 분", "마을 크게 만들고 싶어요. 바닐라 서버입니다.", 8, 2, false, false),
            new Spec("엘든 링", "협동 보스 레이드 소환 도와주세요", "레벨 100 전후, 초행길 안내해드려요.", 3, 1, false, false),
            new Spec("헬다이버즈 2", "민주주의 전파하러 가실 분", "난이도 7~9 위주로 돌아요.", 4, 3, false, false),
            new Spec("몬스터 헌터 와일즈", "고랭크 사냥 같이 가요", "무기 상관없어요. 초보도 환영!", 4, 1, false, false),
            new Spec("에이펙스 레전드", "트리오 랭크 같이 올려요", "다이아 목표로 하는 중입니다.", 3, 2, false, false),
            new Spec("카운터 스트라이크 2", "프리미어 5스택 구함", "마이크 가능하신 분, 평일 저녁.", 5, 3, false, false),
            new Spec("리썰 컴퍼니", "밤에 같이 공포 협동하실 분", "소리 지르며 즐기실 분 환영해요.", 4, 2, false, false),
            new Spec("팰월드", "기지 같이 키우실 분", "서버 열어 놓을게요. 초보 환영.", 6, 1, false, false),
            new Spec("스트리트 파이터 6", "대전 상대 구합니다 (플래티넘)", "친선전 위주로 연습해요.", 2, 1, false, false),
            new Spec("던전앤파이터", "레기온 레이드 공대원 모집", "지식 있으신 분 우대합니다.", 8, 4, false, false),
            new Spec("파스모포비아", "유령 탐지 같이 하실 분", "프로페셔널 난이도 도전 중!", 4, 2, false, false),
            new Spec("스타듀 밸리", "농장 협동 플레이 하실 분", "느긋하게 힐링해요.", 4, 1, false, false),
            new Spec("델타 포스", "작전 모드 스쿼드 모집", "마이크 필수, 즐겜 지향.", 4, 2, false, false),
            new Spec("FC 온라인", "클럽 친선전 상대 구합니다", "주 2회 친선전 진행해요.", 6, 3, false, false),
            // ---- 모집 마감 (정원 가득) ----
            new Spec("발로란트", "[마감] 스크림 5대5 하실 팀 구함", "정원이 다 찼어요. 다음 모집을 기다려 주세요.", 5, 5, false, false),
            new Spec("배틀그라운드", "[마감] 듀오 에란겔 한 판", "자리가 다 찼습니다.", 2, 2, false, false),
            new Spec("로스트아크", "[마감] 발탄 레이드 공대", "공대 구성 완료!", 4, 4, false, false),
            // ---- 종료됨: 실제 가입 회원이 참가자로 들어가 있는 파티 (리뷰 작성 체험용) ----
            new Spec("리그 오브 레전드", "[종료] 자랭 5인큐 같이 한 판 했어요", "게임이 끝났어요. 서로 매너 평가를 남겨 주세요.", 8, 3, true, true),
            new Spec("오버워치 2", "[종료] 주말 빠대 파티", "오늘 즐거웠습니다! 리뷰 남겨 주세요.", 8, 3, true, true),
            new Spec("마인크래프트", "[종료] 건축 서버 1주차 프로젝트", "1주차 마무리! 수고하셨습니다.", 8, 2, true, true),
            new Spec("헬다이버즈 2", "[종료] 난이도 9 클리어 도전", "다 같이 클리어했어요!", 8, 3, true, true),
            new Spec("엘든 링", "[종료] 보스 협동 도와드렸어요", "도움이 되었길 바랍니다.", 8, 2, true, true),
            // ---- 종료됨: 구경용 ----
            new Spec("카운터 스트라이크 2", "[종료] 프리미어 팀 연습", "연습 끝!", 5, 4, true, false),
            new Spec("로스트아크", "[종료] 쿠르잔 전선 클리어", "모두 고생하셨습니다.", 4, 4, true, false));

    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyReviewRepository partyReviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 실제로 가입한 회원 (더미·테스트 계정은 제외)
        List<Member> realMembers = memberRepository.findAll().stream()
                .filter(m -> !m.getUsername().startsWith("dummy") && !m.getUsername().startsWith("cctest"))
                .toList();

        List<Member> dummies;
        if (memberRepository.existsByUsername("dummy01")) {
            // 이미 시딩된 DB: 더미 회원과 파티는 건드리지 않고, 아래 신청자 채우기만 다시 한다.
            dummies = new ArrayList<>();
            for (int i = 0; i < NICKNAMES.size(); i++) {
                memberRepository.findByUsername(String.format("dummy%02d", i + 1)).ifPresent(dummies::add);
            }
        } else {
            String encoded = passwordEncoder.encode(DUMMY_PASSWORD);
            dummies = new ArrayList<>();
            for (int i = 0; i < NICKNAMES.size(); i++) {
                dummies.add(memberRepository.save(Member.builder()
                        .username(String.format("dummy%02d", i + 1))
                        .password(encoded)
                        .nickname(NICKNAMES.get(i))
                        .mainGame(SPECS.get(i % SPECS.size()).game())
                        .build()));
            }
            for (int i = 0; i < SPECS.size(); i++) {
                createParty(SPECS.get(i), i, dummies, realMembers);
            }
        }

        addDummyApplicants(dummies, realMembers);
    }

    /**
     * 실제 회원이 만든 모집 중 파티마다 더미 신청자를 몇 명 붙인다 (승인/거절 체험용).
     * 시딩 뒤에 새로 만든 파티도 같은 옵션으로 다시 실행하면 채워지고, 이미 신청한 더미는 건너뛰므로 여러 번 돌려도 안전하다.
     */
    private void addDummyApplicants(List<Member> dummies, List<Member> realMembers) {
        if (dummies.isEmpty()) {
            return;
        }
        for (Member real : realMembers) {
            int k = 0;
            for (Party party : partyRepository.findByHostOrderByCreatedAtDesc(real)) {
                if (party.getStatus() != PartyStatus.RECRUITING) {
                    continue;
                }
                int added = 0;
                // 이미 더미 신청이 있는 파티는 그대로 두고, 신청이 하나도 없을 때만 2명을 붙인다
                boolean hasApplicants = !partyMemberRepository.findByPartyOrderByAppliedAtAsc(party).stream()
                        .allMatch(pm -> pm.getMember().getId().equals(real.getId()));
                if (hasApplicants) {
                    continue;
                }
                while (added < Math.min(2, dummies.size())) {
                    Member applicant = dummies.get((k++) % dummies.size());
                    if (!partyMemberRepository.existsByPartyAndMember(party, applicant)) {
                        partyMemberRepository.save(PartyMember.builder().party(party).member(applicant).build());
                        added++;
                    }
                }
            }
        }
    }

    private void createParty(Spec spec, int index, List<Member> dummies, List<Member> realMembers) {
        Game game = gameRepository.findByName(spec.game()).orElse(null);
        if (game == null) {
            return;
        }
        Member host = dummies.get(index % dummies.size());
        Party party = partyRepository.save(Party.builder()
                .title(spec.title())
                .content(spec.content())
                .game(game)
                .host(host)
                .maxMembers(spec.max())
                .contactLink("https://discord.gg/dummy" + (index + 1))
                .build());

        // 방장 + 더미 참가자 (spec.joined 는 방장 포함 인원)
        List<Member> approved = new ArrayList<>();
        approved.add(host);
        for (int n = 1; n < spec.joined(); n++) {
            approved.add(dummies.get((index + n) % dummies.size()));
        }
        for (Member m : approved) {
            PartyMember pm = PartyMember.builder().party(party).member(m).build();
            pm.joinQuietly();
            partyMemberRepository.save(pm);
        }

        if (spec.withRealMembers()) {
            for (Member real : realMembers) {
                PartyMember pm = PartyMember.builder().party(party).member(real).build();
                pm.joinQuietly();
                partyMemberRepository.save(pm);
            }
        }

        if (spec.closed()) {
            party.changeStatus(PartyStatus.CLOSED);
            writeDummyReviews(party, approved, spec.withRealMembers() ? realMembers : List.of());
        } else if (approved.size() >= spec.max()) {
            party.changeStatus(PartyStatus.FULL);
        }
    }

    /** 더미 참가자끼리 서로 한 명씩 평가하고, 실제 회원에게도 한 건 남긴다. (실제 회원이 쓸 리뷰는 남겨 둔다) */
    private void writeDummyReviews(Party party, List<Member> dummyParticipants, List<Member> realMembers) {
        int size = dummyParticipants.size();
        if (size >= 2) {
            for (int i = 0; i < size; i++) {
                review(party, dummyParticipants.get(i), dummyParticipants.get((i + 1) % size), 4 + (i % 2), i);
            }
        }
        for (Member real : realMembers) {
            review(party, dummyParticipants.get(0), real, 5, 0);
        }
    }

    private void review(Party party, Member reviewer, Member target, int rating, int commentIndex) {
        if (reviewer.getId().equals(target.getId())
                || partyReviewRepository.existsByPartyAndReviewerAndTarget(party, reviewer, target)) {
            return;
        }
        partyReviewRepository.save(PartyReview.builder()
                .party(party)
                .reviewer(reviewer)
                .target(target)
                .rating(rating)
                .comment(COMMENTS.get(commentIndex % COMMENTS.size()))
                .build());
        target.applyReview(rating);
    }
}
