package mju.capstone.ddingconnect.global.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlarmController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("AlarmController 슬라이스 테스트")
class AlarmControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AlarmService alarmService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("GET /api/v1/alarms - 내 알람 목록")
    void 알람_목록() throws Exception {
        AlarmResponse alarm = new AlarmResponse(AlarmType.ANSWER, 1L, 100L,
                "답변 알람", false, LocalDateTime.now(), "방금 전");
        given(alarmService.getMyAlarms(any())).willReturn(List.of(alarm));

        mockMvc.perform(get("/api/v1/alarms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].type").value("ANSWER"));
    }

    @Test
    @DisplayName("GET /api/v1/alarms/{type}/{id} - 알람 상세")
    void 알람_상세() throws Exception {
        AlarmResponse alarm = new AlarmResponse(AlarmType.JOB, 1L, 100L,
                "구직 알람", false, LocalDateTime.now(), "방금 전");
        given(alarmService.getAlarmDetail(any(), eq(AlarmType.JOB), eq(1L))).willReturn(alarm);

        mockMvc.perform(get("/api/v1/alarms/JOB/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("JOB"));
    }

    @Test
    @DisplayName("PATCH /api/v1/alarms/{type}/{id}/read - 알람 읽음 처리")
    void 알람_읽음() throws Exception {
        mockMvc.perform(patch("/api/v1/alarms/ROADMAP/5/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("알람이 읽음 처리되었습니다."));
        verify(alarmService).markAsRead(any(), eq(AlarmType.ROADMAP), eq(5L));
    }
}
