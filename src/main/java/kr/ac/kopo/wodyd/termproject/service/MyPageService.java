package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.dto.ApplicationView;
import kr.ac.kopo.wodyd.termproject.dto.MyPageView;
import kr.ac.kopo.wodyd.termproject.dto.MyPartyView;
import kr.ac.kopo.wodyd.termproject.dto.ReceivedReviewView;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 로그인한 회원 본인의 활동(만든 파티, 신청 현황, 받은 리뷰)을 모아 보여준다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyReviewRepository partyReviewRepository;

    public MyPageView getMyPage(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        List<MyPartyView> hostedParties = partyRepository.findByHostOrderByCreatedAtDesc(member).stream()
                .map(this::toMyParty)
                .toList();

        // 내가 방장인 파티는 '만든 파티'에 이미 나오므로 신청 현황에서는 뺀다.
        List<ApplicationView> applications = partyMemberRepository.findByMemberOrderByAppliedAtDesc(member).stream()
                .filter(pm -> !pm.getParty().getHost().getId().equals(member.getId()))
                .map(this::toApplication)
                .toList();

        List<ReceivedReviewView> receivedReviews = partyReviewRepository.findByTargetOrderByCreatedAtDesc(member)
                .stream()
                .map(this::toReceivedReview)
                .toList();

        return MyPageView.builder()
                .username(member.getUsername())
                .nickname(member.getNickname())
                .mainGame(member.getMainGame())
                .mannerScore(member.getMannerScore())
                .reviewCount(member.getReviewCount())
                .joinedAt(member.getCreatedAt())
                .hostedParties(hostedParties)
                .applications(applications)
                .receivedReviews(receivedReviews)
                .build();
    }

    private MyPartyView toMyParty(Party party) {
        return MyPartyView.builder()
                .id(party.getId())
                .title(party.getTitle())
                .gameName(party.getGame().getName())
                .status(party.getStatus())
                .currentMembers(partyMemberRepository.countByPartyAndStatus(party, ApplyStatus.APPROVED))
                .maxMembers(party.getMaxMembers())
                .pendingCount(partyMemberRepository.countByPartyAndStatus(party, ApplyStatus.PENDING))
                .createdAt(party.getCreatedAt())
                .build();
    }

    private ApplicationView toApplication(PartyMember pm) {
        Party party = pm.getParty();
        return ApplicationView.builder()
                .partyId(party.getId())
                .partyTitle(party.getTitle())
                .gameName(party.getGame().getName())
                .partyStatus(party.getStatus())
                .applyStatus(pm.getStatus())
                .appliedAt(pm.getAppliedAt())
                .build();
    }

    private ReceivedReviewView toReceivedReview(PartyReview review) {
        return ReceivedReviewView.builder()
                .partyId(review.getParty().getId())
                .partyTitle(review.getParty().getTitle())
                .reviewerNickname(review.getReviewer().getNickname())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
