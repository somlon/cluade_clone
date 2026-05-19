package mju.capstone.ddingconnect.domain.coffeechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatResponse;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
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

@WebMvcTest(CoffeeChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("CoffeeChatController 슬라이스 테스트")
class CoffeeChatControllerTest {

    private static final String BASE_URL = "/api/v1/coffeechat";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CoffeeChatService coffeeChatService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/coffeechat - 커피챗 요청")
    void requestCoffeeChat() throws Exception {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(2L, "https://t");
        given(coffeeChatService.create(any(), any()))
                .willReturn(new CoffeeChatResponse(1L, 1L, 2L, CoffeeChatStatus.PENDING, "https://t"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/coffeechat/sent - 보낸 목록")
    void getSentList() throws Exception {
        given(coffeeChatService.getSentList(any())).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/coffeechat/received - 받은 목록")
    void getReceivedList() throws Exception {
        given(coffeeChatService.getReceivedList(any())).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/coffeechat/{id}/status - 수락/거절")
    void updateStatus() throws Exception {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        given(coffeeChatService.updateStatus(any(), eq(1L), any()))
                .willReturn(new CoffeeChatResponse(1L, 1L, 2L, CoffeeChatStatus.ACCEPTED, "https://t"));

        mockMvc.perform(patch(BASE_URL + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/coffeechat/{id} - 커피챗 취소")
    void cancelCoffeeChat() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.COFFEE_CHAT_CANCELED));
        verify(coffeeChatService).delete(any(), eq(1L));
    }
}
