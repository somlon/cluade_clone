package mju.capstone.ddingconnect.global.alarm;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatAlarm;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.JobAlarm;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerAlarm;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.global.response.exception.handler.AlarmHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmServiceImpl 단위 테스트")
class AlarmServiceImplTest {

    @Mock AnswerAlarmRepository answerAlarmRepository;
    @Mock JobAlarmRepository jobAlarmRepository;
    @Mock RoadmapAlarmRepository roadmapAlarmRepository;
    @Mock CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    @InjectMocks AlarmServiceImpl alarmService;

    private Member member;
    private Member other;
    private AnswerAlarm answerAlarm;
    private JobAlarm jobAlarm;
    private RoadmapAlarm roadmapAlarm;
    private CoffeeChatAlarm coffeeChatAlarm;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).nickname("나").build();
        other = Member.builder().id(2L).nickname("타인").build();

        Question question = Question.builder().id(100L).member(member).build();
        Answer ans = Answer.builder().id(200L).question(question).member(other).content("답변").build();
        answerAlarm = AnswerAlarm.builder().id(11L).answer(ans).content("답변 알람").isRead(false).build();

        PostContents pc = PostContents.builder().id(300L).companyName("네이버").build();
        jobAlarm = JobAlarm.builder().id(12L).member(member).postContents(pc)
                .content("구직 알람").isRead(false).build();

        Roadmap rm = Roadmap.builder().id(400L).member(member).content("{}").build();
        roadmapAlarm = RoadmapAlarm.builder().id(13L).roadmap(rm).content("로드맵 알람").isRead(false).build();

        CoffeeChat cc = CoffeeChat.builder().id(500L).requester(member).receiver(other).build();
        coffeeChatAlarm = CoffeeChatAlarm.builder().id(14L).coffeeChat(cc).member(member)
                .content("커피챗 알람").isRead(false).build();
    }

    @Test
    @DisplayName("getMyAlarms - 4종 알람을 모두 합쳐 반환한다")
    void getMyAlarms_4종통합() {
        when(answerAlarmRepository.findByQuestionOwnerId(1L)).thenReturn(List.of(answerAlarm));
        when(jobAlarmRepository.findByMemberId(1L)).thenReturn(List.of(jobAlarm));
        when(roadmapAlarmRepository.findByRoadmapOwnerId(1L)).thenReturn(List.of(roadmapAlarm));
        when(coffeeChatAlarmRepository.findByMemberId(1L)).thenReturn(List.of(coffeeChatAlarm));

        List<AlarmResponse> result = alarmService.getMyAlarms(member);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(AlarmResponse::type)
                .containsExactlyInAnyOrder(AlarmType.ANSWER, AlarmType.JOB,
                        AlarmType.ROADMAP, AlarmType.COFFEE_CHAT);
    }

    @Test
    @DisplayName("getAlarmDetail - ANSWER 알람을 정상 조회한다")
    void getAlarmDetail_ANSWER() {
        when(answerAlarmRepository.findById(11L)).thenReturn(Optional.of(answerAlarm));

        AlarmResponse response = alarmService.getAlarmDetail(member, AlarmType.ANSWER, 11L);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.type()).isEqualTo(AlarmType.ANSWER);
    }

    @Test
    @DisplayName("getAlarmDetail - JOB 알람을 정상 조회한다")
    void getAlarmDetail_JOB() {
        when(jobAlarmRepository.findById(12L)).thenReturn(Optional.of(jobAlarm));

        AlarmResponse response = alarmService.getAlarmDetail(member, AlarmType.JOB, 12L);

        assertThat(response.type()).isEqualTo(AlarmType.JOB);
    }

    @Test
    @DisplayName("getAlarmDetail - ROADMAP 알람을 정상 조회한다")
    void getAlarmDetail_ROADMAP() {
        when(roadmapAlarmRepository.findById(13L)).thenReturn(Optional.of(roadmapAlarm));

        AlarmResponse response = alarmService.getAlarmDetail(member, AlarmType.ROADMAP, 13L);

        assertThat(response.type()).isEqualTo(AlarmType.ROADMAP);
    }

    @Test
    @DisplayName("getAlarmDetail - COFFEE_CHAT 알람을 정상 조회한다")
    void getAlarmDetail_COFFEE_CHAT() {
        when(coffeeChatAlarmRepository.findById(14L)).thenReturn(Optional.of(coffeeChatAlarm));

        AlarmResponse response = alarmService.getAlarmDetail(member, AlarmType.COFFEE_CHAT, 14L);

        assertThat(response.type()).isEqualTo(AlarmType.COFFEE_CHAT);
    }

    @Test
    @DisplayName("getAlarmDetail - 존재하지 않으면 ALARM_NOT_FOUND 예외")
    void getAlarmDetail_없음_예외() {
        when(answerAlarmRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                alarmService.getAlarmDetail(member, AlarmType.ANSWER, 999L))
                .isInstanceOf(AlarmHandler.class);
    }

    @Test
    @DisplayName("getAlarmDetail - 권한이 없으면 ALARM_UNAUTHORIZED 예외")
    void getAlarmDetail_권한없음_예외() {
        when(jobAlarmRepository.findById(12L)).thenReturn(Optional.of(jobAlarm));

        assertThatThrownBy(() ->
                alarmService.getAlarmDetail(other, AlarmType.JOB, 12L))
                .isInstanceOf(AlarmHandler.class);
    }

    @Test
    @DisplayName("markAsRead - ANSWER 알람을 읽음 처리한다")
    void markAsRead_ANSWER() {
        when(answerAlarmRepository.findById(11L)).thenReturn(Optional.of(answerAlarm));

        alarmService.markAsRead(member, AlarmType.ANSWER, 11L);

        ArgumentCaptor<AnswerAlarm> captor = ArgumentCaptor.forClass(AnswerAlarm.class);
        verify(answerAlarmRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - JOB 알람을 읽음 처리한다")
    void markAsRead_JOB() {
        when(jobAlarmRepository.findById(12L)).thenReturn(Optional.of(jobAlarm));

        alarmService.markAsRead(member, AlarmType.JOB, 12L);

        ArgumentCaptor<JobAlarm> captor = ArgumentCaptor.forClass(JobAlarm.class);
        verify(jobAlarmRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - ROADMAP 알람을 읽음 처리한다")
    void markAsRead_ROADMAP() {
        when(roadmapAlarmRepository.findById(13L)).thenReturn(Optional.of(roadmapAlarm));

        alarmService.markAsRead(member, AlarmType.ROADMAP, 13L);

        ArgumentCaptor<RoadmapAlarm> captor = ArgumentCaptor.forClass(RoadmapAlarm.class);
        verify(roadmapAlarmRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - COFFEE_CHAT 알람을 읽음 처리한다")
    void markAsRead_COFFEE_CHAT() {
        when(coffeeChatAlarmRepository.findById(14L)).thenReturn(Optional.of(coffeeChatAlarm));

        alarmService.markAsRead(member, AlarmType.COFFEE_CHAT, 14L);

        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - 권한이 없으면 ALARM_UNAUTHORIZED 예외")
    void markAsRead_권한없음_예외() {
        when(coffeeChatAlarmRepository.findById(14L)).thenReturn(Optional.of(coffeeChatAlarm));

        assertThatThrownBy(() ->
                alarmService.markAsRead(other, AlarmType.COFFEE_CHAT, 14L))
                .isInstanceOf(AlarmHandler.class);

        verify(coffeeChatAlarmRepository, never()).save(any());
    }
}
