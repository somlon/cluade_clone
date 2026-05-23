package mju.capstone.ddingconnect.domain;

import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerAlarm;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLike;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatAlarm;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.*;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.PostContentsRepository;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionLike;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionLikeRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ExtendWith(EntityIntegrationTest.ResultPrinter.class)
class EntityIntegrationTest {

    private static final String LINE = "═".repeat(60);
    private static final String KAKAO_OPEN_CHAT_LINK = "https://open.kakao.com/o/test";
    private static final String COFFEE_CHAT_ACCEPTED_CONTENT =
            "커피챗 요청이 수락되었습니다. 카카오톡 오픈채팅 링크: " + KAKAO_OPEN_CHAT_LINK;

    // ─── 테스트 결과 출력기 ──────────────────────────────────────────────────
    static class ResultPrinter implements TestWatcher {

        @Override public void testSuccessful(ExtensionContext ctx) {
            System.out.println("  └─ ✅ PASS");
            System.out.println(LINE);
        }
        @Override public void testFailed(ExtensionContext ctx, Throwable cause) {
            System.out.println("  └─ ❌ FAIL: " + cause.getMessage());
            System.out.println(LINE);
        }
        @Override public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
            System.out.println("  └─ ⏭ SKIP: " + reason.orElse("사유 없음"));
            System.out.println(LINE);
        }
        @Override public void testAborted(ExtensionContext ctx, Throwable cause) {
            System.out.println("  └─ ⚠ ABORT: " + cause.getMessage());
            System.out.println(LINE);
        }
    }

    // ─── Repository 주입 ─────────────────────────────────────────────────────
    @Autowired MemberRepository memberRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired GraduateRepository graduateRepository;
    @Autowired TechStackRepository techStackRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired QuestionLikeRepository questionLikeRepository;
    @Autowired AnswerRepository answerRepository;
    @Autowired AnswerLikeRepository answerLikeRepository;
    @Autowired AnswerAlarmRepository answerAlarmRepository;
    @Autowired CoffeeChatRepository coffeeChatRepository;
    @Autowired CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    @Autowired RoadmapRepository roadmapRepository;
    @Autowired RoadmapAlarmRepository roadmapAlarmRepository;
    @Autowired PostContentsRepository postContentsRepository;
    @Autowired GraduateJobPostRepository graduateJobPostRepository;
    @Autowired JobAlarmRepository jobAlarmRepository;
    @Autowired TargetJobRepository targetJobRepository;

    // ─── 공통 헬퍼 ───────────────────────────────────────────────────────────
    private Member saveMember(String email, String nickname) {
        return memberRepository.save(Member.builder()
                .email(email).nickname(nickname).password("pw")
                .studentNumber("60200001").department("컴퓨터공학과")
                .point(0L).isDeleted(false).build());
    }

    /** 테이블 이름 헤더 출력 */
    private static void printHeader(String tableName) {
        System.out.println("\n" + LINE);
        System.out.println("  📋 " + tableName);
    }

    /** 저장된 필드 값을 한 줄로 출력. 인자는 "키", 값, "키", 값, ... 순서 */
    private static void printSaved(Object... keyValues) {
        StringBuilder sb = new StringBuilder("  저장  │");
        for (int i = 0; i < keyValues.length; i += 2) {
            sb.append("  ").append(keyValues[i]).append("=").append(keyValues[i + 1]);
        }
        System.out.println(sb);
    }

    /** FK 검증 결과 출력 */
    private static void printFK(String fkExpr, Long expected, Long actual) {
        boolean ok = expected.equals(actual);
        System.out.printf("  FK    │  %-50s %s%n",
                fkExpr + " = " + actual,
                ok ? "✅" : "❌ (기대=" + expected + ")");
    }

    /** 통합 시나리오 검증 결과 출력 */
    private static void printCheck(String label, Object result) {
        System.out.printf("  검증  │  %-30s %s%n", label, result);
    }

    // ─── 회원 (Member) ───────────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '회원' 테이블 - 전체 컬럼 저장 및 조회")
    void persistsMemberWithAllColumns() {
        printHeader("회원 (Member)");

        Member saved = memberRepository.save(Member.builder()
                .email("test@mju.ac.kr").nickname("테스터").password("pw1234")
                .studentNumber("60201234").department("컴퓨터공학과")
                .githubLink("https://github.com/test")
                .linkedinLink("https://linkedin.com/in/test")
                .portfolio("https://portfolio.com")
                .profileImage("https://img.com/profile.jpg")
                .point(100L).certificate("정보처리기사").isDeleted(false).build());

        Member found = memberRepository.findById(saved.getId()).orElseThrow();
        printSaved(
                "id", found.getId(), "email", found.getEmail(), "nickname", found.getNickname(),
                "studentNumber", found.getStudentNumber(), "point", found.getPoint(),
                "certificate", found.getCertificate(), "isDeleted", found.getIsDeleted(),
                "createdAt", found.getCreatedAt()
        );

        assertThat(found.getEmail()).isEqualTo("test@mju.ac.kr");
        assertThat(found.getNickname()).isEqualTo("테스터");
        assertThat(found.getStudentNumber()).isEqualTo("60201234");
        assertThat(found.getPoint()).isEqualTo(100L);
        assertThat(found.getCertificate()).isEqualTo("정보처리기사");
        assertThat(found.getIsDeleted()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    // ─── 재학생 (Student) ────────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '재학생' 테이블 - FK(회원), 학년 저장")
    void persistsStudent() {
        printHeader("재학생 (Student)");

        Member member = saveMember("s@mju.ac.kr", "재학생");
        studentRepository.save(Student.builder().member(member).grade(3).build());

        Student found = studentRepository.findByMemberId(member.getId()).orElseThrow();
        printSaved("id", found.getId(), "grade", found.getGrade());
        printFK("student.member_id → member.id", member.getId(), found.getMember().getId());

        assertThat(found.getGrade()).isEqualTo(3);
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
    }

    // ─── 졸업생 (Graduate) ───────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '졸업생' 테이블 - FK(회원), 명함이미지, 회사명, 경력 저장")
    void persistsGraduate() {
        printHeader("졸업생 (Graduate)");

        Member member = saveMember("g@mju.ac.kr", "졸업생");
        graduateRepository.save(Graduate.builder()
                .member(member).businessCardImage("https://img.com/card.jpg")
                .company("네이버").careerYear(5).build());

        Graduate found = graduateRepository.findByMemberId(member.getId()).orElseThrow();
        printSaved("id", found.getId(), "company", found.getCompany(), "careerYear", found.getCareerYear());
        printFK("graduate.member_id → member.id", member.getId(), found.getMember().getId());

        assertThat(found.getCompany()).isEqualTo("네이버");
        assertThat(found.getCareerYear()).isEqualTo(5);
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
    }

    // ─── 기술 스택 (TechStack) ───────────────────────────────────────────────
    @Test
    @DisplayName("ERD '기술 스텍' 테이블 - FK(회원), 이름(ENUM) 저장")
    void persistsTechStack() {
        printHeader("기술스택 (TechStack)");

        Member member = saveMember("dev@mju.ac.kr", "개발자");
        techStackRepository.save(TechStack.builder().member(member).name(TechStackName.JAVA).build());
        techStackRepository.save(TechStack.builder().member(member).name(TechStackName.SPRING).build());

        List<TechStack> stacks = techStackRepository.findByMemberId(member.getId());
        printSaved("count", stacks.size(), "names", stacks.stream().map(s -> s.getName().name()).toList());
        stacks.forEach(s -> printFK("techStack.member_id → member.id", member.getId(), s.getMember().getId()));

        assertThat(stacks).hasSize(2);
        assertThat(stacks).extracting(TechStack::getName)
                .containsExactlyInAnyOrder(TechStackName.JAVA, TechStackName.SPRING);
        stacks.forEach(s -> assertThat(s.getMember().getId()).isEqualTo(member.getId()));
    }

    // ─── 질문 (Question) ─────────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '질문' 테이블 - FK(회원), 카테고리ENUM, 제목, 본문, 조회수 저장")
    void persistsQuestion() {
        printHeader("질문 (Question)");

        Member member = saveMember("q@mju.ac.kr", "질문자");
        questionRepository.save(Question.builder()
                .member(member).category(QuestionCategory.TECHNICAL)
                .title("JPA 질문").content("@OneToMany 사용법?").viewCount(0).build());

        Question found = questionRepository.findByMemberId(member.getId()).get(0);
        printSaved("id", found.getId(), "category", found.getCategory(), "title", found.getTitle(), "viewCount", found.getViewCount());
        printFK("question.member_id → member.id", member.getId(), found.getMember().getId());

        assertThat(found.getCategory()).isEqualTo(QuestionCategory.TECHNICAL);
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
    }

    // ─── 질문 좋아요 (QuestionLike) ──────────────────────────────────────────
    @Test
    @DisplayName("ERD '질문 좋아요' 테이블 - FK2(질문), FK(회원) 저장")
    void persistsQuestionLike() {
        printHeader("질문 좋아요 (QuestionLike)");

        Member liker  = saveMember("lk@mju.ac.kr", "좋아요누르는사람");
        Member author = saveMember("au@mju.ac.kr", "작성자");
        Question question = questionRepository.save(Question.builder()
                .member(author).category(QuestionCategory.CAREER)
                .title("커리어 질문").content("내용").viewCount(0).build());

        QuestionLike like = questionLikeRepository.save(
                QuestionLike.builder().question(question).member(liker).build());
        QuestionLike found = questionLikeRepository.findById(like.getId()).orElseThrow();

        printSaved("id", found.getId(), "좋아요수", questionLikeRepository.countByQuestionId(question.getId()));
        printFK("questionLike.member_id → member.id",     liker.getId(),    found.getMember().getId());
        printFK("questionLike.question_id → question.id", question.getId(), found.getQuestion().getId());

        assertThat(questionLikeRepository.existsByMemberIdAndQuestionId(liker.getId(), question.getId())).isTrue();
        assertThat(questionLikeRepository.countByQuestionId(question.getId())).isEqualTo(1L);
        assertThat(found.getMember().getId()).isEqualTo(liker.getId());
        assertThat(found.getQuestion().getId()).isEqualTo(question.getId());
    }

    // ─── 답변 (Answer) ───────────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '답변' 테이블 - FK(질문), PK2(회원), 본문 저장")
    void persistsAnswer() {
        printHeader("답변 (Answer)");

        Member q = saveMember("q2@mju.ac.kr", "질문자2");
        Member a = saveMember("a2@mju.ac.kr", "답변자");
        Question question = questionRepository.save(Question.builder()
                .member(q).category(QuestionCategory.STUDY).title("공부").content("내용").viewCount(0).build());

        answerRepository.save(Answer.builder().question(question).member(a).content("이렇게 하세요.").build());
        Answer found = answerRepository.findByQuestionId(question.getId()).get(0);

        printSaved("id", found.getId(), "content", found.getContent());
        printFK("answer.question_id → question.id", question.getId(), found.getQuestion().getId());
        printFK("answer.member_id → member.id",     a.getId(),        found.getMember().getId());

        assertThat(found.getContent()).isEqualTo("이렇게 하세요.");
        assertThat(found.getQuestion().getId()).isEqualTo(question.getId());
        assertThat(found.getMember().getId()).isEqualTo(a.getId());
    }

    // ─── 답변 좋아요 (AnswerLike) ────────────────────────────────────────────
    @Test
    @DisplayName("ERD '답변 좋아요' 테이블 - PK(answer_id), PK2(member_id) 복합키 저장")
    void persistsAnswerLike() {
        printHeader("답변 좋아요 (AnswerLike) - 복합키");

        Member q = saveMember("q3@mju.ac.kr", "q3");
        Member a = saveMember("a3@mju.ac.kr", "a3");
        Question question = questionRepository.save(Question.builder()
                .member(q).category(QuestionCategory.ETC).title("t").content("c").viewCount(0).build());
        Answer answer = answerRepository.save(Answer.builder()
                .question(question).member(a).content("답변").build());

        answerLikeRepository.save(AnswerLike.builder().answer(answer).member(a).build());

        printSaved("좋아요수", answerLikeRepository.countByAnswerId(answer.getId()));
        printFK("answerLike.answer_id (복합PK/FK)", answer.getId(), answer.getId());
        printFK("answerLike.member_id (복합PK/FK)", a.getId(),      a.getId());

        assertThat(answerLikeRepository.countByAnswerId(answer.getId())).isEqualTo(1L);
        assertThat(answerLikeRepository.existsByMemberIdAndAnswerId(a.getId(), answer.getId())).isTrue();
    }

    // ─── 답변 알람 (AnswerAlarm) ─────────────────────────────────────────────
    @Test
    @DisplayName("ERD '답변 알람' 테이블 - FK(answer_id), 알람내용, 읽음여부 저장")
    void persistsAnswerAlarm() {
        printHeader("답변 알람 (AnswerAlarm)");

        Member q = saveMember("q_al@mju.ac.kr", "질문자");
        Member a = saveMember("al@mju.ac.kr", "답변자");
        Question question = questionRepository.save(Question.builder()
                .member(q).category(QuestionCategory.ETC).title("질문").content("내용").viewCount(0).build());
        Answer answer = answerRepository.save(Answer.builder()
                .question(question).member(a).content("답변내용").build());

        answerAlarmRepository.save(AnswerAlarm.builder()
                .answer(answer).content("새 답변이 달렸습니다.").isRead(false).build());

        AnswerAlarm found = answerAlarmRepository.findAll().get(0);
        printSaved("id", found.getId(), "content", found.getContent(), "isRead", found.getIsRead());
        printFK("answerAlarm.answer_id → answer.id", answer.getId(), found.getAnswer().getId());

        assertThat(answerAlarmRepository.countByAnswerIdAndIsRead(answer.getId(), false)).isEqualTo(1L);
        assertThat(found.getAnswer().getId()).isEqualTo(answer.getId());
    }

    // ─── 커피챗 (CoffeeChat) ─────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '커피챗' 테이블 - 요청자/수신자(회원FK), 점수들, 수략여부ENUM, 카카오링크 저장")
    void persistsCoffeeChat() {
        printHeader("커피챗 (CoffeeChat)");

        Member requester = saveMember("req@mju.ac.kr", "요청자");
        Member receiver  = saveMember("rec@mju.ac.kr", "수신자");

        coffeeChatRepository.save(CoffeeChat.builder()
                .requester(requester).receiver(receiver)
                .jobScore(80).ability(90).goal(70)
                .status(CoffeeChatStatus.PENDING)
                .kakaoOpenChatLink(KAKAO_OPEN_CHAT_LINK).build());

        CoffeeChat found = coffeeChatRepository.findByRequesterId(requester.getId()).get(0);
        printSaved("id", found.getId(), "jobScore", found.getJobScore(), "ability", found.getAbility(), "goal", found.getGoal(), "status", found.getStatus());
        printFK("coffeeChat.requester_id → member.id", requester.getId(), found.getRequester().getId());
        printFK("coffeeChat.receiver_id  → member.id", receiver.getId(),  found.getReceiver().getId());

        assertThat(found.getStatus()).isEqualTo(CoffeeChatStatus.PENDING);
        assertThat(found.getRequester().getId()).isEqualTo(requester.getId());
        assertThat(found.getReceiver().getId()).isEqualTo(receiver.getId());
    }

    // ─── 커피챗 알람 (CoffeeChatAlarm) ──────────────────────────────────────
    @Test
    @DisplayName("ERD '커피챗 알람' 테이블 - FK(커피챗), 알람내용, 읽음여부 저장")
    void persistsCoffeeChatAlarm() {
        printHeader("커피챗 알람 (CoffeeChatAlarm)");

        Member req = saveMember("req2@mju.ac.kr", "요청자2");
        Member rec = saveMember("rec2@mju.ac.kr", "수신자2");
        CoffeeChat chat = coffeeChatRepository.save(CoffeeChat.builder()
                .requester(req).receiver(rec).status(CoffeeChatStatus.ACCEPTED).build());

        // [수락 시 흐름] 양쪽 모두에게 카카오 링크 포함 알람 2건 발행
        coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                .coffeeChat(chat).member(req)
                .content(COFFEE_CHAT_ACCEPTED_CONTENT)
                .isRead(false).build());
        coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                .coffeeChat(chat).member(rec)
                .content(COFFEE_CHAT_ACCEPTED_CONTENT)
                .isRead(false).build());

        List<CoffeeChatAlarm> alarms = coffeeChatAlarmRepository.findByCoffeeChatId(chat.getId());
        CoffeeChatAlarm found = alarms.get(0);
        printSaved("alarmCount", alarms.size(), "content", found.getContent(), "isRead", found.getIsRead());
        printFK("coffeeChatAlarm.coffee_chat_id → coffeeChat.id", chat.getId(), found.getCoffeeChat().getId());

        // 수락 시 양쪽 모두에게 알람이 발행됨 (총 2건)
        assertThat(alarms).hasSize(2);
        assertThat(alarms).extracting(a -> a.getMember().getId())
                .containsExactlyInAnyOrder(req.getId(), rec.getId());
        assertThat(alarms).allSatisfy(a -> {
            assertThat(a.getIsRead()).isFalse();
            assertThat(a.getCoffeeChat().getId()).isEqualTo(chat.getId());
            assertThat(a.getContent()).contains(KAKAO_OPEN_CHAT_LINK);
        });
    }

    // ─── 로드맵 (Roadmap) ────────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '로드맵' 테이블 - FK(회원), 내용(JSON) 저장")
    void persistsRoadmap() {
        printHeader("로드맵 (Roadmap)");

        Member m = saveMember("roadmap@mju.ac.kr", "로드맵작성자");
        roadmapRepository.save(Roadmap.builder()
                .member(m).content("{\"steps\":[\"CS기초\",\"알고리즘\",\"프로젝트\"]}").build());

        Roadmap found = roadmapRepository.findByMemberId(m.getId()).get(0);
        printSaved("id", found.getId(), "content", found.getContent());
        printFK("roadmap.member_id → member.id", m.getId(), found.getMember().getId());

        assertThat(found.getContent()).contains("CS기초");
        assertThat(found.getMember().getId()).isEqualTo(m.getId());
    }

    // ─── 로드맵 알람 (RoadmapAlarm) ──────────────────────────────────────────
    @Test
    @DisplayName("ERD '로드맵 알람' 테이블 - PK2(로드맵), 알람내용, 읽음여부 저장")
    void persistsRoadmapAlarm() {
        printHeader("로드맵 알람 (RoadmapAlarm)");

        Member m = saveMember("recv@mju.ac.kr", "알람수신자");
        Roadmap roadmap = roadmapRepository.save(Roadmap.builder()
                .member(m).content("{\"steps\":[\"학습\"]}").build());

        roadmapAlarmRepository.save(RoadmapAlarm.builder()
                .roadmap(roadmap).content("새 로드맵이 등록되었습니다.").isRead(false).build());

        RoadmapAlarm found = roadmapAlarmRepository.findAll().get(0);
        printSaved("id", found.getId(), "content", found.getContent(), "isRead", found.getIsRead());
        printFK("roadmapAlarm.roadmap_id → roadmap.id", roadmap.getId(), found.getRoadmap().getId());

        assertThat(roadmapAlarmRepository.countByRoadmapIdAndIsRead(roadmap.getId(), false)).isEqualTo(1L);
        assertThat(found.getRoadmap().getId()).isEqualTo(roadmap.getId());
    }

    // ─── 구직 공고 (PostContents) ────────────────────────────────────────────
    @Test
    @DisplayName("ERD '구직 공고' 테이블 - 전체 컬럼 저장")
    void persistsPostContents() {
        printHeader("구직 공고 (PostContents)");

        postContentsRepository.save(PostContents.builder()
                .companyImage("https://img.com/naver.jpg").companyName("네이버")
                .region("서울특별시 성남구").country("경기도").location("성남시").fullLocation("분당구")
                .careerType(CareerType.NEW_GRADUATE).jobType(JobType.BACKEND)
                .deadline(LocalDate.of(2026, 6, 30))
                .detailUrl("https://recruit.naver.com/1").preferredLanguages(List.of("Java")).build());

        PostContents found = postContentsRepository.findByJobType(JobType.BACKEND).get(0);
        printSaved("id", found.getId(), "companyName", found.getCompanyName(),
                "careerType", found.getCareerType(), "jobType", found.getJobType(), "deadline", found.getDeadline());
        // PostContents는 FK 없음 (다른 테이블로부터 참조받는 부모 엔티티)

        assertThat(found.getCompanyName()).isEqualTo("네이버");
        assertThat(found.getCareerType()).isEqualTo(CareerType.NEW_GRADUATE);
        assertThat(found.getDeadline()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    // ─── 졸업생이 생성한 구직공고 (GraduateJobPost) ──────────────────────────
    @Test
    @DisplayName("ERD '졸업생이 생성한 구직공고' 테이블 - PK2(졸업생), PK3(구직공고) 저장")
    void persistsGraduateJobPost() {
        printHeader("졸업생 구직공고 (GraduateJobPost)");

        Member m = saveMember("grd2@mju.ac.kr", "졸업생2");
        Graduate graduate = graduateRepository.save(Graduate.builder()
                .member(m).company("쿠팡").careerYear(6).build());
        PostContents postContents = postContentsRepository.save(PostContents.builder()
                .companyName("쿠팡").jobType(JobType.DEVOPS).careerType(CareerType.SENIOR).build());

        graduateJobPostRepository.save(GraduateJobPost.builder()
                .graduate(graduate).postContents(postContents).build());

        GraduateJobPost found = graduateJobPostRepository.findAll().get(0);
        printSaved("연결존재", graduateJobPostRepository.existsByGraduateIdAndPostContentsId(graduate.getId(), postContents.getId()));
        printFK("graduateJobPost.graduate_id → graduate.id",          graduate.getId(),     found.getGraduate().getId());
        printFK("graduateJobPost.post_contents_id → postContents.id", postContents.getId(), found.getPostContents().getId());

        assertThat(found.getGraduate().getId()).isEqualTo(graduate.getId());
        assertThat(found.getPostContents().getId()).isEqualTo(postContents.getId());
    }

    // ─── 구직 알람 (JobAlarm) ────────────────────────────────────────────────
    @Test
    @DisplayName("ERD '구직 알람' 테이블 - PK2(회원), PK3(구직공고), 알람내용, 읽음여부 저장")
    void persistsJobAlarm() {
        printHeader("구직 알람 (JobAlarm)");

        Member member = saveMember("jal@mju.ac.kr", "구직알람수신자");
        PostContents postContents = postContentsRepository.save(PostContents.builder()
                .companyName("토스").jobType(JobType.FRONTEND).careerType(CareerType.JUNIOR).build());

        jobAlarmRepository.save(JobAlarm.builder()
                .member(member).postContents(postContents).content("새 채용 공고").isRead(false).build());

        JobAlarm found = jobAlarmRepository.findAll().get(0);
        printSaved("id", found.getId(), "content", found.getContent(), "isRead", found.getIsRead());
        printFK("jobAlarm.member_id → member.id",              member.getId(),       found.getMember().getId());
        printFK("jobAlarm.post_contents_id → postContents.id", postContents.getId(), found.getPostContents().getId());

        assertThat(jobAlarmRepository.countByMemberIdAndIsRead(member.getId(), false)).isEqualTo(1L);
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getPostContents().getId()).isEqualTo(postContents.getId());
    }

    // ─── 관심 직군 (TargetJob) ───────────────────────────────────────────────
    @Test
    @DisplayName("ERD '관심 직군' 테이블 - FK(회원), 직군ENUM 저장")
    void persistsTargetJob() {
        printHeader("관심 직군 (TargetJob)");

        Member member = saveMember("int@mju.ac.kr", "관심등록자");

        targetJobRepository.save(TargetJob.builder()
                .member(member)
                .interestedJob(TargetJobCategory.BACKEND).build());

        TargetJob found = targetJobRepository.findByMemberId(member.getId()).get(0);
        printSaved("id", found.getId(), "interestedJob", found.getInterestedJob());
        printFK("targetJob.member_id → member.id", member.getId(), found.getMember().getId());

        assertThat(found.getInterestedJob()).isEqualTo(TargetJobCategory.BACKEND);
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
    }

    // ─── 전체 시나리오 통합 테스트 ───────────────────────────────────────────
    @Test
    @DisplayName("통합 시나리오: 재학생 질문 → 졸업생 답변 → 커피챗 요청 전체 흐름")
    void fullScenarioIntegration() {
        printHeader("통합 시나리오: 재학생 질문 → 졸업생 답변 → 커피챗");

        // 1. 회원
        Member studentMember = saveMember("stu@mju.ac.kr", "재학생Kim");
        Member gradMember    = saveMember("grd@mju.ac.kr", "졸업생Lee");

        // 2. 프로필
        Student  student  = studentRepository.save(Student.builder().member(studentMember).grade(3).build());
        Graduate graduate = graduateRepository.save(Graduate.builder().member(gradMember).company("삼성").careerYear(5).build());

        // 3. 기술스택
        TechStack stack = techStackRepository.save(TechStack.builder().member(gradMember).name(TechStackName.JAVA).build());

        // 4. 질문 & 좋아요
        Question question = questionRepository.save(Question.builder()
                .member(studentMember).category(QuestionCategory.CAREER)
                .title("백엔드 취업 준비").content("어떻게 준비할까요?").viewCount(0).build());
        QuestionLike questionLike = questionLikeRepository.save(
                QuestionLike.builder().question(question).member(gradMember).build());

        // 5. 답변 & 좋아요 & 알람
        Answer answer = answerRepository.save(Answer.builder()
                .question(question).member(gradMember).content("알고리즘과 프로젝트를 쌓으세요.").build());
        answerLikeRepository.save(AnswerLike.builder().answer(answer).member(gradMember).build());
        AnswerAlarm answerAlarm = answerAlarmRepository.save(AnswerAlarm.builder()
                .answer(answer).content("답변이 달렸습니다.").isRead(false).build());

        // 6. 커피챗 & 알람
        CoffeeChat coffeeChat = coffeeChatRepository.save(CoffeeChat.builder()
                .requester(studentMember).receiver(gradMember)
                .jobScore(75).ability(85).goal(60).status(CoffeeChatStatus.PENDING).build());
        // [요청 발행 시 흐름] 수신자(gradMember)에게 "요청 도착" 알람 1건
        CoffeeChatAlarm coffeeChatAlarm = coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                .coffeeChat(coffeeChat).member(gradMember)
                .content("새로운 커피챗 요청이 도착했습니다.").isRead(false).build());

        // 7. 로드맵 & 알람
        Roadmap roadmap = roadmapRepository.save(Roadmap.builder()
                .member(gradMember).content("{\"steps\":[\"CS\",\"코테\",\"프로젝트\"]}").build());
        RoadmapAlarm roadmapAlarm = roadmapAlarmRepository.save(RoadmapAlarm.builder()
                .roadmap(roadmap).content("새 로드맵이 등록되었습니다.").isRead(false).build());

        // 8. 구직공고 & 관심직군 & 알람
        PostContents postContents = postContentsRepository.save(PostContents.builder()
                .companyName("카카오").jobType(JobType.BACKEND).careerType(CareerType.NEW_GRADUATE)
                .deadline(LocalDate.of(2026, 8, 31)).build());
        GraduateJobPost graduateJobPost = graduateJobPostRepository.save(GraduateJobPost.builder()
                .graduate(graduate).postContents(postContents).build());
        TargetJob targetJob = targetJobRepository.save(TargetJob.builder()
                .member(studentMember)
                .interestedJob(TargetJobCategory.BACKEND).build());
        JobAlarm jobAlarm = jobAlarmRepository.save(JobAlarm.builder()
                .member(studentMember).postContents(postContents)
                .content("관심 공고 업데이트").isRead(false).build());

        // ── FK 검증 ───────────────────────────────────────────────────────
        printFK("student.member_id            → studentMember.id",    studentMember.getId(), student.getMember().getId());
        printFK("graduate.member_id           → gradMember.id",       gradMember.getId(),    graduate.getMember().getId());
        printFK("techStack.member_id          → gradMember.id",       gradMember.getId(),    stack.getMember().getId());
        printFK("question.member_id           → studentMember.id",    studentMember.getId(), question.getMember().getId());
        printFK("questionLike.question_id     → question.id",         question.getId(),      questionLike.getQuestion().getId());
        printFK("questionLike.member_id       → gradMember.id",       gradMember.getId(),    questionLike.getMember().getId());
        printFK("answer.question_id           → question.id",         question.getId(),      answer.getQuestion().getId());
        printFK("answer.member_id             → gradMember.id",       gradMember.getId(),    answer.getMember().getId());
        printFK("answerAlarm.answer_id        → answer.id",           answer.getId(),        answerAlarm.getAnswer().getId());
        printFK("coffeeChat.requester_id      → studentMember.id",    studentMember.getId(), coffeeChat.getRequester().getId());
        printFK("coffeeChat.receiver_id       → gradMember.id",       gradMember.getId(),    coffeeChat.getReceiver().getId());
        printFK("coffeeChatAlarm.coffee_chat_id → coffeeChat.id",     coffeeChat.getId(),    coffeeChatAlarm.getCoffeeChat().getId());
        printFK("roadmap.member_id            → gradMember.id",       gradMember.getId(),    roadmap.getMember().getId());
        printFK("roadmapAlarm.roadmap_id      → roadmap.id",          roadmap.getId(),       roadmapAlarm.getRoadmap().getId());
        printFK("graduateJobPost.graduate_id  → graduate.id",         graduate.getId(),      graduateJobPost.getGraduate().getId());
        printFK("graduateJobPost.post_contents_id → postContents.id", postContents.getId(),  graduateJobPost.getPostContents().getId());
        printFK("targetJob.member_id          → studentMember.id",    studentMember.getId(), targetJob.getMember().getId());
        printFK("jobAlarm.member_id           → studentMember.id",    studentMember.getId(), jobAlarm.getMember().getId());
        printFK("jobAlarm.post_contents_id    → postContents.id",     postContents.getId(),  jobAlarm.getPostContents().getId());

        // ── 데이터 검증 ───────────────────────────────────────────────────
        assertThat(studentRepository.findByMemberId(studentMember.getId())).isPresent();
        printCheck("재학생 프로필 저장", "✅");
        assertThat(graduateRepository.findByMemberId(gradMember.getId())).isPresent();
        printCheck("졸업생 프로필 저장", "✅");
        assertThat(techStackRepository.findByMemberId(gradMember.getId())).hasSize(1);
        printCheck("기술스택 수 = 1", "✅");
        assertThat(questionLikeRepository.countByQuestionId(question.getId())).isEqualTo(1L);
        printCheck("질문 좋아요 수 = 1", "✅");
        assertThat(answerRepository.findByQuestionId(question.getId())).hasSize(1);
        printCheck("답변 수 = 1", "✅");
        assertThat(answerLikeRepository.countByAnswerId(answer.getId())).isEqualTo(1L);
        printCheck("답변 좋아요 수 = 1", "✅");
        assertThat(answerAlarmRepository.countByAnswerIdAndIsRead(answer.getId(), false)).isEqualTo(1L);
        printCheck("답변 알람(미읽음) 수 = 1", "✅");
        assertThat(coffeeChatRepository.findByReceiverIdAndStatus(gradMember.getId(), CoffeeChatStatus.PENDING)).hasSize(1);
        printCheck("PENDING 커피챗 수 = 1", "✅");
        assertThat(coffeeChatAlarmRepository.findByCoffeeChatId(coffeeChat.getId())).hasSize(1);
        printCheck("커피챗 알람 수 = 1", "✅");
        assertThat(roadmapRepository.findByMemberId(gradMember.getId())).hasSize(1);
        printCheck("로드맵 수 = 1", "✅");
        assertThat(roadmapAlarmRepository.countByRoadmapIdAndIsRead(roadmap.getId(), false)).isEqualTo(1L);
        printCheck("로드맵 알람(미읽음) 수 = 1", "✅");
        assertThat(graduateJobPostRepository.existsByGraduateIdAndPostContentsId(graduate.getId(), postContents.getId())).isTrue();
        printCheck("졸업생-구직공고 연결", "✅");
        assertThat(targetJobRepository.findByMemberId(studentMember.getId())).hasSize(1);
        printCheck("관심직군 수 = 1", "✅");
        assertThat(jobAlarmRepository.countByMemberIdAndIsRead(studentMember.getId(), false)).isEqualTo(1L);
        printCheck("구직 알람(미읽음) 수 = 1", "✅");
    }
}
