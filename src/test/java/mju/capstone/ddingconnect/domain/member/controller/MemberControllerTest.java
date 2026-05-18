package mju.capstone.ddingconnect.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
import mju.capstone.ddingconnect.domain.member.service.MyPageService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("MemberController 슬라이스 테스트")
class MemberControllerTest {

    private static final String BASE_URL = "/api/v1/members";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MemberService memberService;
    @MockitoBean MyPageService myPageService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("GET /api/v1/members/test - JWT 인증 테스트")
    void JWT_테스트() throws Exception {
        mockMvc.perform(get(BASE_URL + "/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("test@mju.ac.kr"));
    }

    @Test
    @DisplayName("GET /api/v1/members/me - 내 프로필 조회")
    void 내프로필_조회() throws Exception {
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.getMyProfile(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.grade").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me - 회원 정보 수정")
    void 회원정보_수정() throws Exception {
        UpdateMemberRequest req = new UpdateMemberRequest("새이름", null, "새닉네임", null, null,
                null, null, null, null, null, null, null, null, null);
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", "새이름", "새닉네임",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.updateMyProfile(any(), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("새이름"))
                .andExpect(jsonPath("$.result.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("DELETE /api/v1/members/me - 회원 탈퇴")
    void 회원_탈퇴() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.MEMBER_WITHDRAWN));
        verify(memberService).withdraw(any());
    }

    @Test
    @DisplayName("GET /api/v1/members/mypage - 마이페이지 조회")
    void 마이페이지_조회() throws Exception {
        MemberResponse profile = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        MyPageResponse res = new MyPageResponse(
                profile,
                new MyPageResponse.ActivityStats(2L, 1L, 5L),
                List.of(), List.of(), List.of());
        given(myPageService.getMyPage(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/mypage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.activity.questionCount").value(5));
    }
}
