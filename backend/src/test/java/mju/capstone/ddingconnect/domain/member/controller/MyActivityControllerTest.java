package mju.capstone.ddingconnect.domain.member.controller;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatPartnerResponse;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("MyActivityController 슬라이스 테스트")
class MyActivityControllerTest {

    private static final String URL = "/api/v1/members/me/activity/coffeechats";

    @Autowired MockMvc mockMvc;
    @MockitoBean CoffeeChatService coffeeChatService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("GET /api/v1/members/me/activity/coffeechats - CoffeeChatService.getMyActivities 위임")
    void getMyCoffeeChats() throws Exception {
        given(coffeeChatService.getMyActivities(any()))
                .willReturn(List.of(
                        new CoffeeChatPartnerResponse(
                                101L, CoffeeChatStatus.ACCEPTED,
                                2L, "이선배", "응용소프트웨어학과",
                                List.of(TargetJobCategory.BACKEND),
                                List.of(TechStackName.JAVA, TechStackName.SPRING))));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].coffeeChatId").value(101L))
                .andExpect(jsonPath("$.result[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.result[0].partnerId").value(2L))
                .andExpect(jsonPath("$.result[0].partnerNickname").value("이선배"))
                .andExpect(jsonPath("$.result[0].partnerJobs[0]").value("BACKEND"))
                .andExpect(jsonPath("$.result[0].partnerTechStacks[0]").value("JAVA"));

        verify(coffeeChatService).getMyActivities(any());
    }
}
