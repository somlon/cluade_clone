package mju.capstone.ddingconnect.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MemberService memberService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("GET /api/v1/members/test - JWT 인증 테스트")
    void JWT_테스트() throws Exception {
        mockMvc.perform(get("/api/v1/members/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("test@mju.ac.kr"));
    }

    @Test
    @DisplayName("GET /api/v1/members/me - 내 프로필 조회")
    void 내프로필_조회() throws Exception {
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null);
        given(memberService.getMyProfile(any())).willReturn(res);

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.grade").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me - 회원 정보 수정")
    void 회원정보_수정() throws Exception {
        UpdateMemberRequest req = new UpdateMemberRequest("새닉네임", null, null,
                null, null, null, null, null, null, null, null);
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", "새닉네임",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null);
        given(memberService.updateMyProfile(any(), any())).willReturn(res);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("DELETE /api/v1/members/me - 회원 탈퇴")
    void 회원_탈퇴() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("회원 탈퇴가 완료되었습니다."));
        verify(memberService).withdraw(any());
    }
}
