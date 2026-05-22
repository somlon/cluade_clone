package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("RoadmapAiClientImpl 단위 테스트")
class RoadmapAiClientImplTest {

    private static final String GENERATE_PATH = "/api/data/generate";
    private static final Long MEMBER_ID = 1L;
    private static final String GENERATE_URI = GENERATE_PATH + "?member_id=" + MEMBER_ID;

    private MockRestServiceServer server;
    private RoadmapAiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new RoadmapAiClientImpl(restClientBuilder.build());
    }

    @Test
    @DisplayName("데이터 파트 응답 본문(AI 로드맵 JSON)을 그대로 문자열로 반환한다")
    void returnsResponseBodyAsContent() {
        String aiJson = "{\"roadmap_title\":\"백엔드 개발자 로드맵\",\"steps\":[]}";
        server.expect(requestTo(GENERATE_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(aiJson, MediaType.APPLICATION_JSON));

        String result = client.generate(sampleForm(), MEMBER_ID);

        assertThat(result).isEqualTo(aiJson);
        server.verify();
    }

    @Test
    @DisplayName("회원 ID 를 member_id URL 쿼리 파라미터로 전달한다")
    void sendsMemberIdAsQueryParam() {
        server.expect(requestTo(GENERATE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("member_id", String.valueOf(MEMBER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.generate(sampleForm(), MEMBER_ID);

        server.verify();
    }

    @Test
    @DisplayName("요청 본문은 데이터 파트 RoadmapRequest 스키마(6필드, snake_case) 와 정합")
    void requestBodyMatchesRoadmapRequestSchema() {
        server.expect(requestTo(GENERATE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grade").value(3))
                .andExpect(jsonPath("$.gpa").value(4.0))
                .andExpect(jsonPath("$.major").value("응용소프트웨어학과"))
                .andExpect(jsonPath("$.target_job").value("BACKEND"))         // enum 명으로 직렬화
                .andExpect(jsonPath("$.current_skills").isArray())
                .andExpect(jsonPath("$.current_skills[0]").value("JAVA"))
                .andExpect(jsonPath("$.current_skills[1]").value("SPRING"))
                .andExpect(jsonPath("$.target_company").value("카카오"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.generate(sampleForm(), MEMBER_ID);

        server.verify();
    }

    @Test
    @DisplayName("currentSkills 가 null 이면 current_skills 를 빈 배열로 직렬화한다")
    void serializesNullCurrentSkillsAsEmptyArray() {
        CreateRoadmapRequest form = new CreateRoadmapRequest(3, 4.0, "응용소프트웨어학과",
                TargetJobCategory.BACKEND, null, "카카오");

        server.expect(requestTo(GENERATE_URI))
                .andExpect(jsonPath("$.current_skills").isArray())
                .andExpect(jsonPath("$.current_skills").isEmpty())
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.generate(form, MEMBER_ID);

        server.verify();
    }

    @Test
    @DisplayName("데이터 파트 호출 실패(500) 시 ROADMAP_AI_GENERATION_FAILED 로 변환한다")
    void rethrowsFailureAsRoadmapHandler() {
        server.expect(requestTo(GENERATE_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(sampleForm(), MEMBER_ID))
                .isInstanceOf(RoadmapHandler.class);

        server.verify();
    }

    private CreateRoadmapRequest sampleForm() {
        return new CreateRoadmapRequest(3, 4.0, "응용소프트웨어학과",
                TargetJobCategory.BACKEND, List.of(TechStackName.JAVA, TechStackName.SPRING), "카카오");
    }
}
