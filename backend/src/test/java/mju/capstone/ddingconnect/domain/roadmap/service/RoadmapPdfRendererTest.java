package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoadmapPdfRenderer 단위 테스트 — 한글 폰트 임베딩 + AI JSON 매핑")
class RoadmapPdfRendererTest {

    private RoadmapPdfRenderer renderer;

    private static final String SAMPLE_JSON = """
            {
              "roadmap_title": "백엔드 개발자 로드맵",
              "steps": [
                {
                  "phase_badge": "1-2학년 기초",
                  "title": "프로그래밍 기초 & CS 이론",
                  "details": [
                    {"category": "학습", "content": "자료구조, 알고리즘"},
                    {"category": "프로젝트", "content": "토이 프로젝트 2개"}
                  ]
                },
                {
                  "phase_badge": "3학년 실전",
                  "title": "백엔드 핵심 역량",
                  "details": [
                    {"category": "학습", "content": "Spring, JPA, DB"}
                  ]
                },
                {
                  "phase_badge": "4학년 취업",
                  "title": "취업 준비",
                  "details": [
                    {"category": "포트폴리오", "content": "깃허브 정리"}
                  ]
                }
              ],
              "recommended_certifications": ["정보처리기사", "SQLD", "AWS SAA"],
              "recommended_activities": ["오픈소스 기여", "공모전 참여"],
              "summary_advice": "꾸준함이 가장 강력한 무기입니다."
            }
            """;

    @BeforeEach
    void setUp() {
        renderer = new RoadmapPdfRenderer();
        renderer.loadFont(); // @PostConstruct 직접 호출 (테스트에선 컨테이너 미가동)
    }

    @Test
    @DisplayName("render - 정상 JSON 이면 PDF magic 'PDF-' 으로 시작하는 비어있지 않은 바이트 반환")
    void rendersValidPdf() {
        byte[] pdf = renderer.render(SAMPLE_JSON);

        assertThat(pdf).isNotEmpty();
        // PDF 파일은 "%PDF-" 매직으로 시작한다
        String head = new String(pdf, 0, Math.min(5, pdf.length));
        assertThat(head).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("render - 잘못된 JSON 이면 RoadmapHandler(INVALID_CONTENT)")
    void throwsOnMalformedJson() {
        assertThatThrownBy(() -> renderer.render("not-a-json-object"))
                .isInstanceOf(RoadmapHandler.class);
    }

    @Test
    @DisplayName("render - 일부 필드 null/빈 리스트여도 PDF 생성 성공 (최소 표지만이라도)")
    void rendersWithMinimalFields() {
        String minimal = "{\"roadmap_title\":\"최소 로드맵\",\"steps\":[],\"recommended_certifications\":[],\"recommended_activities\":[],\"summary_advice\":\"\"}";

        byte[] pdf = renderer.render(minimal);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
