package mju.capstone.ddingconnect.domain.coffeechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatMatchingService;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static mju.capstone.ddingconnect.domain.coffeechat.CoffeeChatMatchingTestConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoffeeChatMatchingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("CoffeeChatMatchingController 슬라이스 테스트")
class CoffeeChatMatchingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CoffeeChatMatchingService coffeeChatMatchingService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/coffeechat/matching - 매칭 후보 카드 리스트 반환")
    void getMatchingCandidates() throws Exception {
        given(coffeeChatMatchingService.match(any(), any()))
                .willReturn(List.of(candidateCard(CANDIDATE_ID_1), candidateCard(CANDIDATE_ID_2)));

        mockMvc.perform(post(MATCHING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleForm())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].memberId").value(CANDIDATE_ID_1.intValue()));
    }

    @Test
    @DisplayName("GET /api/v1/coffeechat/matching/{memberId} - 매칭 상대 상세 반환")
    void getMatchingDetail() throws Exception {
        given(coffeeChatMatchingService.getCandidateDetail(eq(CANDIDATE_ID_1)))
                .willReturn(candidateDetail(CANDIDATE_ID_1));

        mockMvc.perform(get(MATCHING_URL + "/" + CANDIDATE_ID_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.memberId").value(CANDIDATE_ID_1.intValue()));
    }
}
