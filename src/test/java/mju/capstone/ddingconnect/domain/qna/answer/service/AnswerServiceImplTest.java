package mju.capstone.ddingconnect.domain.qna.answer.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerAlarm;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLike;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLikeId;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.AnswerHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerServiceImpl 단위 테스트")
class AnswerServiceImplTest {

    @Mock AnswerRepository answerRepository;
    @Mock AnswerLikeRepository answerLikeRepository;
    @Mock AnswerAlarmRepository answerAlarmRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AnswerServiceImpl answerService;

    private Member questioner;
    private Member answerer;
    private Member other;
    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        // questioner 는 STUDENT (질문 작성자)
        questioner = Member.builder().id(1L).nickname("질문자").role(MemberRole.STUDENT).build();
        // answerer 는 GRADUATE (답변 작성자 — TODO #11 로 GRADUATE 만 답변 가능)
        answerer = Member.builder().id(2L).nickname("답변자").role(MemberRole.GRADUATE).build();
        other = Member.builder().id(3L).nickname("타인").role(MemberRole.GRADUATE).build();
        question = Question.builder().id(10L).member(questioner)
                .category(QuestionCategory.STUDY).title("질문").content("내용").viewCount(0).build();
        answer = Answer.builder().id(20L).question(question).member(answerer).content("답변내용").build();
    }

    @Test
    @DisplayName("create - 졸업생이 다른 사람 질문에 답변 시 AnswerAlarm 1건 발행한다")
    void create_정상등록_타인질문_알람발행() {
        CreateAnswerRequest req = new CreateAnswerRequest("답변내용");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        AnswerResponse response = answerService.create(answerer, 10L, req);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.questionId()).isEqualTo(10L);
        assertThat(response.likeCount()).isZero();
        assertThat(response.likedByMe()).isFalse();
        verify(answerAlarmRepository).save(any(AnswerAlarm.class));

        // 커밋 후 SSE 푸시용 이벤트가 질문 작성자 대상으로 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AlarmNotificationEvent event = eventCaptor.getValue();
        assertThat(event.receiver().getId()).isEqualTo(questioner.getId());
        assertThat(event.type()).isEqualTo(AlarmType.ANSWER);
        assertThat(event.content()).isEqualTo("내 질문에 새로운 답변이 달렸습니다.");
    }

    @Test
    @DisplayName("create - 졸업생이 본인 질문에 답변하면 AnswerAlarm 미발행")
    void create_본인질문_본인답변_알람미발행() {
        // graduate 가 자기 질문에 직접 답변
        Member graduateAuthor = Member.builder().id(7L).nickname("졸업생작성자").role(MemberRole.GRADUATE).build();
        Question myQuestion = Question.builder().id(11L).member(graduateAuthor)
                .category(QuestionCategory.STUDY).title("내질문").content("내용").viewCount(0).build();
        Answer selfAnswer = Answer.builder().id(21L).question(myQuestion).member(graduateAuthor).content("자답").build();
        CreateAnswerRequest req = new CreateAnswerRequest("자답");
        when(questionRepository.findById(11L)).thenReturn(Optional.of(myQuestion));
        when(answerRepository.save(any(Answer.class))).thenReturn(selfAnswer);

        answerService.create(graduateAuthor, 11L, req);

        verify(answerAlarmRepository, never()).save(any(AnswerAlarm.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("create - 학생이 답변 시도 시 ANSWER_NOT_GRADUATE 예외")
    void create_학생답변_예외() {
        CreateAnswerRequest req = new CreateAnswerRequest("답변");

        assertThatThrownBy(() -> answerService.create(questioner, 10L, req))
                .isInstanceOf(AnswerHandler.class);
        verify(questionRepository, never()).findById(any());
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - UNKNOWN 역할은 답변 불가")
    void create_UNKNOWN답변_예외() {
        Member unknown = Member.builder().id(99L).role(MemberRole.UNKNOWN).build();
        CreateAnswerRequest req = new CreateAnswerRequest("답변");

        assertThatThrownBy(() -> answerService.create(unknown, 10L, req))
                .isInstanceOf(AnswerHandler.class);
    }

    @Test
    @DisplayName("create - 질문이 없으면 QUESTION_NOT_FOUND 예외")
    void create_질문없음_예외() {
        CreateAnswerRequest req = new CreateAnswerRequest("답변");
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.create(answerer, 999L, req))
                .isInstanceOf(QuestionHandler.class);
        verify(answerAlarmRepository, never()).save(any(AnswerAlarm.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getList - 특정 질문의 답변 목록을 카운트/likedByMe와 함께 반환한다")
    void getList_카운트_likedByMe_포함() {
        when(answerRepository.findByQuestionId(10L)).thenReturn(List.of(answer));
        when(answerLikeRepository.countByAnswerId(20L)).thenReturn(4L);
        when(answerLikeRepository.existsByMemberIdAndAnswerId(other.getId(), 20L)).thenReturn(true);

        List<AnswerResponse> result = answerService.getList(other, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("답변내용");
        assertThat(result.get(0).likeCount()).isEqualTo(4L);
        assertThat(result.get(0).likedByMe()).isTrue();
    }

    @Test
    @DisplayName("update - 작성자가 답변을 정상 수정한다")
    void update_정상수정() {
        UpdateAnswerRequest req = new UpdateAnswerRequest("수정된 답변");
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(answerRepository.save(any(Answer.class))).thenAnswer(inv -> inv.getArgument(0));

        AnswerResponse response = answerService.update(answerer, 20L, req);

        assertThat(response.content()).isEqualTo("수정된 답변");
    }

    @Test
    @DisplayName("update - 작성자가 아니면 UNAUTHORIZED 예외")
    void update_권한없음_예외() {
        UpdateAnswerRequest req = new UpdateAnswerRequest("수정");
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.update(other, 20L, req))
                .isInstanceOf(AnswerHandler.class);
    }

    @Test
    @DisplayName("update - 존재하지 않으면 ANSWER_NOT_FOUND 예외")
    void update_없음_예외() {
        UpdateAnswerRequest req = new UpdateAnswerRequest("수정");
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.update(answerer, 999L, req))
                .isInstanceOf(AnswerHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제하면 AnswerAlarm/AnswerLike 먼저 삭제 후 Answer 삭제")
    void delete_정상삭제() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        answerService.delete(answerer, 20L);

        InOrder inOrder = inOrder(answerAlarmRepository, answerLikeRepository, answerRepository);
        inOrder.verify(answerAlarmRepository).deleteByAnswerId(20L);
        inOrder.verify(answerLikeRepository).deleteByAnswerId(20L);
        inOrder.verify(answerRepository).delete(answer);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void delete_권한없음_예외() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.delete(other, 20L))
                .isInstanceOf(AnswerHandler.class);
        verify(answerAlarmRepository, never()).deleteByAnswerId(any());
        verify(answerLikeRepository, never()).deleteByAnswerId(any());
        verify(answerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 ANSWER_NOT_FOUND 예외")
    void delete_없음_예외() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.delete(answerer, 999L))
                .isInstanceOf(AnswerHandler.class);
    }

    @Test
    @DisplayName("toggleLike - 좋아요가 없으면 새로 추가하고 liked=true/likeCount 반환")
    void toggleLike_좋아요추가() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(answerLikeRepository.findById(any(AnswerLikeId.class))).thenReturn(Optional.empty());
        when(answerLikeRepository.existsByMemberIdAndAnswerId(other.getId(), 20L)).thenReturn(true);
        when(answerLikeRepository.countByAnswerId(20L)).thenReturn(1L);

        LikeToggleResponse response = answerService.toggleLike(other, 20L);

        verify(answerLikeRepository).save(any(AnswerLike.class));
        verify(answerLikeRepository, never()).delete(any());
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("toggleLike - 이미 좋아요가 있으면 삭제하고 liked=false/likeCount 반환")
    void toggleLike_좋아요취소() {
        AnswerLike existing = AnswerLike.builder().answer(answer).member(other).build();
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(answerLikeRepository.findById(any(AnswerLikeId.class))).thenReturn(Optional.of(existing));
        when(answerLikeRepository.existsByMemberIdAndAnswerId(other.getId(), 20L)).thenReturn(false);
        when(answerLikeRepository.countByAnswerId(20L)).thenReturn(0L);

        LikeToggleResponse response = answerService.toggleLike(other, 20L);

        verify(answerLikeRepository).delete(existing);
        verify(answerLikeRepository, never()).save(any());
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
    }

    @Test
    @DisplayName("toggleLike - 본인 답변이면 ANSWER_SELF_LIKE 예외")
    void toggleLike_본인답변_예외() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.toggleLike(answerer, 20L))
                .isInstanceOf(AnswerHandler.class);
        verify(answerLikeRepository, never()).save(any());
        verify(answerLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("toggleLike - 답변이 없으면 ANSWER_NOT_FOUND 예외")
    void toggleLike_없음_예외() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.toggleLike(other, 999L))
                .isInstanceOf(AnswerHandler.class);
    }
}
