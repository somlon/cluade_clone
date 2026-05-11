package mju.capstone.ddingconnect.domain.qna.answer.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
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
import mju.capstone.ddingconnect.global.response.exception.handler.AnswerHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerServiceImpl 단위 테스트")
class AnswerServiceImplTest {

    @Mock AnswerRepository answerRepository;
    @Mock AnswerLikeRepository answerLikeRepository;
    @Mock AnswerAlarmRepository answerAlarmRepository;
    @Mock QuestionRepository questionRepository;
    @InjectMocks AnswerServiceImpl answerService;

    private Member questioner;
    private Member answerer;
    private Member other;
    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        questioner = Member.builder().id(1L).nickname("질문자").build();
        answerer = Member.builder().id(2L).nickname("답변자").build();
        other = Member.builder().id(3L).nickname("타인").build();
        question = Question.builder().id(10L).member(questioner)
                .category(QuestionCategory.STUDY).title("질문").content("내용").viewCount(0).build();
        answer = Answer.builder().id(20L).question(question).member(answerer).content("답변내용").build();
    }

    @Test
    @DisplayName("create - 다른 사람 질문에 답변 시 AnswerAlarm 1건 발행한다")
    void create_정상등록_타인질문_알람발행() {
        CreateAnswerRequest req = new CreateAnswerRequest("답변내용");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        AnswerResponse response = answerService.create(answerer, 10L, req);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.questionId()).isEqualTo(10L);
        verify(answerAlarmRepository).save(any(AnswerAlarm.class));
    }

    @Test
    @DisplayName("create - 본인이 본인 질문에 답변하면 AnswerAlarm 미발행")
    void create_본인질문_본인답변_알람미발행() {
        // questioner 가 자기 질문에 직접 답변
        Answer selfAnswer = Answer.builder().id(21L).question(question).member(questioner).content("자답").build();
        CreateAnswerRequest req = new CreateAnswerRequest("자답");
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class))).thenReturn(selfAnswer);

        answerService.create(questioner, 10L, req);

        verify(answerAlarmRepository, never()).save(any(AnswerAlarm.class));
    }

    @Test
    @DisplayName("create - 질문이 없으면 QUESTION_NOT_FOUND 예외")
    void create_질문없음_예외() {
        CreateAnswerRequest req = new CreateAnswerRequest("답변");
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.create(answerer, 999L, req))
                .isInstanceOf(QuestionHandler.class);
        verify(answerAlarmRepository, never()).save(any(AnswerAlarm.class));
    }

    @Test
    @DisplayName("getList - 특정 질문의 답변 목록을 반환한다")
    void getList_정상반환() {
        when(answerRepository.findByQuestionId(10L)).thenReturn(List.of(answer));

        List<AnswerResponse> result = answerService.getList(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("답변내용");
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
    @DisplayName("delete - 작성자가 답변을 정상 삭제한다")
    void delete_정상삭제() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        answerService.delete(answerer, 20L);

        verify(answerRepository).delete(answer);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.delete(other, 20L))
                .isInstanceOf(AnswerHandler.class);
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
    @DisplayName("toggleLike - 좋아요가 없으면 새로 추가한다")
    void toggleLike_좋아요추가() {
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(answerLikeRepository.findById(any(AnswerLikeId.class))).thenReturn(Optional.empty());

        answerService.toggleLike(other, 20L);

        verify(answerLikeRepository).save(any(AnswerLike.class));
        verify(answerLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("toggleLike - 이미 좋아요가 있으면 삭제한다")
    void toggleLike_좋아요취소() {
        AnswerLike existing = AnswerLike.builder().answer(answer).member(other).build();
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(answerLikeRepository.findById(any(AnswerLikeId.class))).thenReturn(Optional.of(existing));

        answerService.toggleLike(other, 20L);

        verify(answerLikeRepository).delete(existing);
        verify(answerLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleLike - 답변이 없으면 ANSWER_NOT_FOUND 예외")
    void toggleLike_없음_예외() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.toggleLike(other, 999L))
                .isInstanceOf(AnswerHandler.class);
    }
}
