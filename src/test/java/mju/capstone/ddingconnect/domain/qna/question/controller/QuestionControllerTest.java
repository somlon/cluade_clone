package mju.capstone.ddingconnect.domain.qna.question.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
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

@WebMvcTest(QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("QuestionController 슬라이스 테스트")
class QuestionControllerTest {

    private static final String BASE_URL = "/api/v1/questions";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean QuestionService questionService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/questions - 질문 등록")
    void 질문_등록() throws Exception {
        CreateQuestionRequest req = new CreateQuestionRequest(QuestionCategory.TECHNICAL, "제목", "내용");
        QuestionResponse res = new QuestionResponse(1L, 1L, QuestionCategory.TECHNICAL, "제목", "내용", 0, 0L, 0L, false);
        given(questionService.create(any(), any())).willReturn(res);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/questions - 질문 목록")
    void 질문_목록() throws Exception {
        given(questionService.getList(any())).willReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/questions/{id} - 질문 상세")
    void 질문_상세() throws Exception {
        QuestionResponse res = new QuestionResponse(1L, 1L, QuestionCategory.STUDY, "t", "c", 1, 2L, 3L, true);
        given(questionService.getOne(any(), eq(1L))).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(1))
                .andExpect(jsonPath("$.result.likeCount").value(2))
                .andExpect(jsonPath("$.result.answerCount").value(3))
                .andExpect(jsonPath("$.result.likedByMe").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/questions/{id} - 질문 수정")
    void 질문_수정() throws Exception {
        UpdateQuestionRequest req = new UpdateQuestionRequest(null, "수정", null);
        QuestionResponse res = new QuestionResponse(1L, 1L, QuestionCategory.STUDY, "수정", "c", 0, 0L, 0L, false);
        given(questionService.update(any(), eq(1L), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("수정"));
    }

    @Test
    @DisplayName("DELETE /api/v1/questions/{id} - 질문 삭제")
    void 질문_삭제() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.QUESTION_DELETED));
        verify(questionService).delete(any(), eq(1L));
    }

    @Test
    @DisplayName("POST /api/v1/questions/{id}/like - 좋아요 토글 응답에 liked/likeCount 포함")
    void 질문_좋아요() throws Exception {
        given(questionService.toggleLike(any(), eq(1L)))
                .willReturn(new LikeToggleResponse(true, 5L));

        mockMvc.perform(post(BASE_URL + "/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.liked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(5));
    }
}
