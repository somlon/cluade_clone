package mju.capstone.ddingconnect.domain.roadmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
import mju.capstone.ddingconnect.global.config.WebMvcConfig;
import mju.capstone.ddingconnect.support.WithMockLoginMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoadmapController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("RoadmapController 슬라이스 테스트")
class RoadmapControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoadmapService roadmapService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/roadmaps - 로드맵 등록")
    void 로드맵_등록() throws Exception {
        CreateRoadmapRequest req = new CreateRoadmapRequest("{}");
        given(roadmapService.create(any(), any())).willReturn(new RoadmapResponse(1L, 1L, "{}"));

        mockMvc.perform(post("/api/v1/roadmaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps - 로드맵 목록")
    void 로드맵_목록() throws Exception {
        given(roadmapService.getList()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/roadmaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps/{id} - 로드맵 상세")
    void 로드맵_상세() throws Exception {
        given(roadmapService.getOne(1L)).willReturn(new RoadmapResponse(1L, 1L, "{}"));

        mockMvc.perform(get("/api/v1/roadmaps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/v1/roadmaps/{id} - 로드맵 삭제")
    void 로드맵_삭제() throws Exception {
        mockMvc.perform(delete("/api/v1/roadmaps/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("로드맵이 삭제되었습니다."));
        verify(roadmapService).delete(any(), eq(1L));
    }
}
