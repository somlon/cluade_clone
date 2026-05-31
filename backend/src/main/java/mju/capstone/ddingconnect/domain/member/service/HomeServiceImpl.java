package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.member.dto.response.HomeResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [홈 화면 서비스 구현체]
 * 항목별 도메인 서비스/레포지토리를 in-process 로 조합하는 애그리게이터
 * (MyPageServiceImpl 과 동일 패턴 — 활동 카운트 메서드를 그대로 재사용).
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final StudentRepository studentRepository;
    private final GraduateRepository graduateRepository;
    private final CoffeeChatService coffeeChatService;
    private final RoadmapService roadmapService;
    private final QuestionService questionService;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHome(Member member) {
        // 역할별 학과/학년 자리 표시 필드
        //   STUDENT  : department + grade        채움, company/careerYear 는 null
        //   GRADUATE : company   + careerYear    채움, department/grade   는 null
        String department = null;
        String company = null;
        Integer grade = null;
        Integer careerYear = null;
        if (member.getRole() == MemberRole.STUDENT) {
            department = member.getDepartment();
            Student student = studentRepository.findByMemberId(member.getId()).orElse(null);
            grade = student != null ? student.getGrade() : null;
        } else if (member.getRole() == MemberRole.GRADUATE) {
            Graduate graduate = graduateRepository.findByMemberId(member.getId()).orElse(null);
            if (graduate != null) {
                company = graduate.getCompany();
                careerYear = graduate.getCareerYear();
            }
        }

        // 활동 카운트 — 마이페이지와 동일 집계 기준 재사용
        // 로드맵은 STUDENT 전용 — GRADUATE/UNKNOWN 은 null → 응답에서 키 자체 제외
        Long roadmapCount = member.getRole() == MemberRole.STUDENT
                ? roadmapService.countMyRoadmaps(member)
                : null;

        HomeResponse.ActivityCounts activity = new HomeResponse.ActivityCounts(
                coffeeChatService.countMyAcceptedCoffeeChats(member),
                roadmapCount,
                questionService.countMyQuestions(member)
        );

        return new HomeResponse(
                member.getPoint(),
                member.getNickname(),
                department,
                company,
                member.getRole(),
                grade,
                careerYear,
                activity
        );
    }
}
