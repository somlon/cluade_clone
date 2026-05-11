package mju.capstone.ddingconnect.domain.techstack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
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

@WebMvcTest(TechStackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("TechStackController 슬라이스 테스트")
class TechStackControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TechStackService techStackService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/tech-stacks - 기술 스택 추가")
    void 기술스택_추가() throws Exception {
        CreateTechStackRequest req = new CreateTechStackRequest(TechStackName.JAVA);
        given(techStackService.add(any(), any()))
                .willReturn(new TechStackResponse(1L, TechStackName.JAVA));

        mockMvc.perform(post("/api/v1/tech-stacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("JAVA"));
    }

    @Test
    @DisplayName("GET /api/v1/tech-stacks - 내 기술 스택 목록")
    void 기술스택_목록() throws Exception {
        given(techStackService.getMyTechStacks(any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/tech-stacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/tech-stacks/{id} - 기술 스택 삭제")
    void 기술스택_삭제() throws Exception {
        mockMvc.perform(delete("/api/v1/tech-stacks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("기술 스택이 삭제되었습니다."));
        verify(techStackService).delete(any(), eq(1L));
    }
}
