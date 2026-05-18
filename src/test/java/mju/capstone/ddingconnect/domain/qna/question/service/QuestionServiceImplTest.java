package mju.capstone.ddingconnect.domain.qna.question.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionLike;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionLikeRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionServiceImpl 단위 테스트")
class QuestionServiceImplTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionLikeRepository questionLikeRepository;
    @Mock AnswerRepository answerRepository;
    @Mock AnswerAlarmRepository answerAlarmRepository;
    @Mock AnswerLikeRepository answerLikeRepository;
    @InjectMocks QuestionServiceImpl questionService;

    private Member author;
    private Member other;
    private Question question;

    @BeforeEach
    void setUp() {
        author = Member.builder().id(1L).email("a@mju.ac.kr").nickname("작성자").build();
        other = Member.builder().id(2L).email("o@mju.ac.kr").nickname("타인").build();
        question = Question.builder().id(10L).member(author)
                .category(QuestionCategory.TECHNICAL)
                .title("제목").content("내용").viewCount(5).build();
    }

    @Test
    @DisplayName("create - 질문을 정상 등록한다")
    void create_정상등록() {
        CreateQuestionRequest req = new CreateQuestionRequest(QuestionCategory.CAREER, "제목", "내용");
        when(questionRepository.save(any(Question.class))).thenReturn(question);

        QuestionResponse response = questionService.create(author, req);

        assertThat(response.id()).isEqualTo(10L);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("getList - 질문 목록을 카운트/likedByMe와 함께 반환한다")
    void getList_카운트_likedByMe_포함() {
        when(questionRepository.findAll()).thenReturn(List.of(question));
        when(questionLikeRepository.countByQuestionId(10L)).thenReturn(7L);
        when(answerRepository.countByQuestionId(10L)).thenReturn(3L);
        when(questionLikeRepository.existsByMemberIdAndQuestionId(other.getId(), 10L)).thenReturn(true);

        List<QuestionResponse> result = questionService.getList(other);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("제목");
        assertThat(result.get(0).likeCount()).isEqualTo(7L);
        assertThat(result.get(0).answerCount()).isEqualTo(3L);
        assertThat(result.get(0).likedByMe()).isTrue();
    }

    @Test
    @DisplayName("getOne - 상세 조회 시 조회수가 +1 증가하고 카운트/likedByMe가 반환된다")
    void getOne_조회수증가_카운트반환() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        when(questionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(questionLikeRepository.countByQuestionId(10L)).thenReturn(2L);
        when(answerRepository.countByQuestionId(10L)).thenReturn(3L);
        when(questionLikeRepository.existsByMemberIdAndQuestionId(other.getId(), 10L)).thenReturn(true);

        QuestionResponse response = questionService.getOne(other, 10L);

        assertThat(captor.getValue().getViewCount()).isEqualTo(6);
        assertThat(response.likeCount()).isEqualTo(2L);
        assertThat(response.answerCount()).isEqualTo(3L);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 NOT_FOUND 예외")
    void getOne_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getOne(other, 999L))
                .isInstanceOf(QuestionHandler.class);
    }

    @Test
    @DisplayName("update - 작성자가 질문을 정상 수정한다")
    void update_정상수정() {
        UpdateQuestionRequest req = new UpdateQuestionRequest(null, "수정 제목", null);
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionResponse response = questionService.update(author, 10L, req);

        assertThat(response.title()).isEqualTo("수정 제목");
    }

    @Test
    @DisplayName("update - 작성자가 아니면 UNAUTHORIZED 예외")
    void update_권한없음_예외() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        UpdateQuestionRequest req = new UpdateQuestionRequest(null, "수정", null);

        assertThatThrownBy(() -> questionService.update(other, 10L, req))
                .isInstanceOf(QuestionHandler.class);
    }

    @Test
    @DisplayName("update - 존재하지 않으면 NOT_FOUND 예외")
    void update_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateQuestionRequest req = new UpdateQuestionRequest(null, "수정", null);

        assertThatThrownBy(() -> questionService.update(author, 999L, req))
                .isInstanceOf(QuestionHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제하면 손자 자식 순서로 모두 삭제한다")
    void delete_정상삭제() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        questionService.delete(author, 10L);

        // 손자(AnswerAlarm, AnswerLike) → Answer → QuestionLike → Question
        InOrder inOrder = inOrder(answerAlarmRepository, answerLikeRepository,
                answerRepository, questionLikeRepository, questionRepository);
        inOrder.verify(answerAlarmRepository).deleteByQuestionId(10L);
        inOrder.verify(answerLikeRepository).deleteByQuestionId(10L);
        inOrder.verify(answerRepository).deleteByQuestionId(10L);
        inOrder.verify(questionLikeRepository).deleteByQuestionId(10L);
        inOrder.verify(questionRepository).delete(question);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void delete_권한없음_예외() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.delete(other, 10L))
                .isInstanceOf(QuestionHandler.class);
        verify(answerAlarmRepository, never()).deleteByQuestionId(any());
        verify(answerLikeRepository, never()).deleteByQuestionId(any());
        verify(answerRepository, never()).deleteByQuestionId(any());
        verify(questionLikeRepository, never()).deleteByQuestionId(any());
        verify(questionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.delete(author, 999L))
                .isInstanceOf(QuestionHandler.class);
    }

    @Test
    @DisplayName("toggleLike - 좋아요가 없으면 새로 추가하고 liked=true/likeCount 반환")
    void toggleLike_좋아요추가() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionLikeRepository.findAll()).thenReturn(List.of());
        when(questionLikeRepository.existsByMemberIdAndQuestionId(other.getId(), 10L)).thenReturn(true);
        when(questionLikeRepository.countByQuestionId(10L)).thenReturn(1L);

        LikeToggleResponse response = questionService.toggleLike(other, 10L);

        verify(questionLikeRepository).save(any(QuestionLike.class));
        verify(questionLikeRepository, never()).delete(any());
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("toggleLike - 이미 좋아요가 있으면 삭제하고 liked=false/likeCount 반환")
    void toggleLike_좋아요취소() {
        QuestionLike existing = QuestionLike.builder().question(question).member(other).build();
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionLikeRepository.findAll()).thenReturn(List.of(existing));
        when(questionLikeRepository.existsByMemberIdAndQuestionId(other.getId(), 10L)).thenReturn(false);
        when(questionLikeRepository.countByQuestionId(10L)).thenReturn(0L);

        LikeToggleResponse response = questionService.toggleLike(other, 10L);

        verify(questionLikeRepository).delete(existing);
        verify(questionLikeRepository, never()).save(any());
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
    }

    @Test
    @DisplayName("toggleLike - 본인 질문이면 QUESTION_SELF_LIKE 예외")
    void toggleLike_본인질문_예외() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.toggleLike(author, 10L))
                .isInstanceOf(QuestionHandler.class);
        verify(questionLikeRepository, never()).save(any());
        verify(questionLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("toggleLike - 존재하지 않는 질문이면 NOT_FOUND 예외")
    void toggleLike_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.toggleLike(other, 999L))
                .isInstanceOf(QuestionHandler.class);
    }

    @Test
    @DisplayName("countMyQuestions - 본인이 작성한 질문 수를 반환한다")
    void countMyQuestions_정상반환() {
        when(questionRepository.countByMemberId(author.getId())).thenReturn(6L);

        long result = questionService.countMyQuestions(author);

        assertThat(result).isEqualTo(6L);
    }
}
