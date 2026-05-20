package mju.capstone.ddingconnect.domain.coffeechat.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [매칭 알고리즘 클라이언트 구현체 — RestClient]
 *
 * base URL 은 설정값 {@code matching.algorithm.base-url}, 상위 N 은
 * {@code matching.algorithm.top-n} 에서 주입한다(하드코딩 금지).
 * 엔드포인트 경로는 데이터 파트({@code ddingconnect-data}) 라우트
 * {@code POST /api/data/coffeechat/match} 에 결합한다.
 *
 * <p>흐름 (학생 → 졸업생 매칭 한정):
 * <ol>
 *   <li>{@code MemberRepository.findAllByRole(GRADUATE)} 로 후보 풀 조회 — 신청자 본인 제외,
 *       매칭용 데이터가 비어 있는 후보({@code jobType} null, {@code TechStack} 0건,
 *       {@code company} blank) 는 제외.</li>
 *   <li>요청자(폼)·후보(DB) 를 각각 {@link UserInfo} 로 매핑 후
 *       후보 N 명만큼 {@code POST /api/data/coffeechat/match} 1대1 페어 호출.</li>
 *   <li>응답 {@code totalMatchRate} DESC 정렬, 상위 {@code top-n} 후보 ID 반환.</li>
 * </ol>
 *
 * <p>호출 실패·타임아웃·비정상 응답은 모두 {@link ErrorStatus#MATCHING_ALGORITHM_FAILED}.
 */
@Slf4j
@Component
public class MatchingAlgorithmClientImpl implements MatchingAlgorithmClient {

    private static final String MATCH_ENDPOINT_PATH = "/api/data/coffeechat/match";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final int topN;
    private final MemberRepository memberRepository;
    private final GraduateRepository graduateRepository;
    private final TechStackRepository techStackRepository;

    public MatchingAlgorithmClientImpl(
            @Value("${matching.algorithm.base-url}") String baseUrl,
            @Value("${matching.algorithm.top-n}") int topN,
            MemberRepository memberRepository,
            GraduateRepository graduateRepository,
            TechStackRepository techStackRepository) {
        this(buildDefaultRestClient(baseUrl), topN,
                memberRepository, graduateRepository, techStackRepository);
    }

    /** 테스트용 — {@link org.springframework.test.web.client.MockRestServiceServer} 바인딩된 RestClient 주입. */
    MatchingAlgorithmClientImpl(
            RestClient restClient,
            int topN,
            MemberRepository memberRepository,
            GraduateRepository graduateRepository,
            TechStackRepository techStackRepository) {
        this.restClient = restClient;
        this.topN = topN;
        this.memberRepository = memberRepository;
        this.graduateRepository = graduateRepository;
        this.techStackRepository = techStackRepository;
    }

    private static RestClient buildDefaultRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<Long> topMatches(MatchingRequest form, Long requesterId) {
        List<UserInfo> candidates = buildCandidates(requesterId);
        if (candidates.isEmpty()) {
            return List.of();
        }
        UserInfo requester = buildRequester(form, requesterId);
        return candidates.stream()
                .map(candidate -> new AbstractMap.SimpleEntry<>(
                        candidate.userId(),
                        callAlgorithm(requester, candidate, requesterId)))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double callAlgorithm(UserInfo requester, UserInfo receiver, Long requesterId) {
        AlgorithmMatchResponse response;
        try {
            response = restClient.post()
                    .uri(MATCH_ENDPOINT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new AlgorithmMatchRequest(requester, receiver))
                    .retrieve()
                    .body(AlgorithmMatchResponse.class);
        } catch (RestClientException e) {
            log.error("매칭 알고리즘 호출 실패: requesterId={}, candidateId={}, {}",
                    requesterId, receiver.userId(), e.getMessage());
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }
        if (response == null || response.matchResults() == null) {
            log.error("매칭 알고리즘 응답이 비어 있습니다: requesterId={}, candidateId={}",
                    requesterId, receiver.userId());
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }
        return response.matchResults().totalMatchRate();
    }

    private UserInfo buildRequester(MatchingRequest form, Long requesterId) {
        return new UserInfo(
                requesterId,
                form.interestedJob(),
                normalizeTechStacks(form.capability()),
                form.targetCompany()
        );
    }

    private List<UserInfo> buildCandidates(Long requesterId) {
        return memberRepository.findAllByRole(MemberRole.GRADUATE).stream()
                .filter(member -> !member.getId().equals(requesterId))
                .map(this::toCandidateUserInfo)
                .filter(info -> info != null)
                .toList();
    }

    private UserInfo toCandidateUserInfo(Member member) {
        Graduate graduate = graduateRepository.findByMemberId(member.getId()).orElse(null);
        if (graduate == null || graduate.getJobType() == null) {
            return null;
        }
        if (graduate.getCompany() == null || graduate.getCompany().isBlank()) {
            return null;
        }
        List<String> techStacks = techStackRepository.findByMemberId(member.getId()).stream()
                .map(TechStack::getName)
                .map(TechStackName::name)
                .toList();
        if (techStacks.isEmpty()) {
            return null;
        }
        return new UserInfo(
                member.getId(),
                graduate.getJobType().name(),
                techStacks,
                graduate.getCompany()
        );
    }

    /**
     * 폼 {@code capability}(콤마 구분 자유 텍스트)를 {@link TechStackName} 화이트리스트 토큰 리스트로 정규화한다.
     * 토큰 trim → 대문자 → {@code TechStackName.valueOf} 매칭 안 되는 항목은 버린다. 중복 제거.
     */
    private List<String> normalizeTechStacks(String capability) {
        if (capability == null || capability.isBlank()) {
            return List.of();
        }
        return Arrays.stream(capability.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toUpperCase(Locale.ROOT))
                .filter(MatchingAlgorithmClientImpl::isKnownTechStack)
                .distinct()
                .toList();
    }

    private static boolean isKnownTechStack(String name) {
        try {
            TechStackName.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 알고리즘 페어 요청의 단일 회원 정보.
     * 데이터 파트({@code ddingconnect-data}) 의 {@code UserInfo} 스키마와 정합:
     * {@code user_id, job, tech_stacks, goal} (snake_case).
     */
    record UserInfo(
            @JsonProperty("user_id") Long userId,
            String job,
            @JsonProperty("tech_stacks") List<String> techStacks,
            String goal
    ) {}

    /** 알고리즘 요청 본문 — 양방향 페어 {@code {requester, receiver}}. */
    record AlgorithmMatchRequest(UserInfo requester, UserInfo receiver) {}

    /**
     * 알고리즘 응답 본문 — 한 페어의 점수 dict.
     * 데이터 파트 응답 키 네이밍 혼용 대응:
     * {@code jobScore}·{@code totalMatchRate} 는 camelCase,
     * {@code ability}·{@code goal} 은 소문자.
     */
    record MatchScores(
            double jobScore,
            double ability,
            double goal,
            double totalMatchRate
    ) {}

    /** 알고리즘 응답 envelope — {@code status} + {@code match_results}. */
    record AlgorithmMatchResponse(
            String status,
            @JsonProperty("match_results") MatchScores matchResults
    ) {}
}
