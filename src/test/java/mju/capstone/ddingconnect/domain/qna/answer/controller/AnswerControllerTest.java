package mju.capstone.ddingconnect.domain.qna.answer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;
import mju.capstone.ddingconnect.domain.qna.answer.service.AnswerService;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
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

@WebMvcTest(AnswerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("AnswerController 슬라이스 테스트")
class AnswerControllerTest {

    private static final String BASE_URL = "/api/v1/questions/10/answers";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AnswerService answerService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsGraduate(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/questions/{qid}/answers - 답변 등록")
    void 답변_등록() throws Exception {
        CreateAnswerRequest req = new CreateAnswerRequest("답변");
        given(answerService.create(any(), eq(10L), any()))
                .willReturn(new AnswerResponse(1L, 10L, 1L, "답변", 0L, false));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/questions/{qid}/answers - 답변 목록")
    void 답변_목록() throws Exception {
        given(answerService.getList(any(), eq(10L))).willReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/questions/{qid}/answers/{aid} - 답변 수정")
    void 답변_수정() throws Exception {
        UpdateAnswerRequest req = new UpdateAnswerRequest("수정된 답변");
        given(answerService.update(any(), eq(20L), any()))
                .willReturn(new AnswerResponse(20L, 10L, 1L, "수정된 답변", 0L, false));

        mockMvc.perform(patch(BASE_URL + "/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").value("수정된 답변"));
    }

    @Test
    @DisplayName("DELETE /api/v1/questions/{qid}/answers/{aid} - 답변 삭제")
    void 답변_삭제() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.ANSWER_DELETED));
        verify(answerService).delete(any(), eq(20L));
    }

    @Test
    @DisplayName("POST /api/v1/questions/{qid}/answers/{aid}/like - 답변 좋아요 응답에 liked/likeCount 포함")
    void 답변_좋아요() throws Exception {
        given(answerService.toggleLike(any(), eq(20L)))
                .willReturn(new LikeToggleResponse(true, 3L));

        mockMvc.perform(post(BASE_URL + "/20/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.liked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(3));
    }
}
