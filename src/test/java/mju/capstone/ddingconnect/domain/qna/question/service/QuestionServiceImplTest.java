package mju.capstone.ddingconnect.domain.qna.question.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionLike;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionLikeRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionServiceImpl 단위 테스트")
class QuestionServiceImplTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionLikeRepository questionLikeRepository;
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
    @DisplayName("getList - 질문 목록을 반환한다")
    void getList_정상반환() {
        when(questionRepository.findAll()).thenReturn(List.of(question));

        List<QuestionResponse> result = questionService.getList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("getOne - 상세 조회 시 조회수가 +1 증가한다")
    void getOne_조회수증가() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        when(questionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        questionService.getOne(10L);

        assertThat(captor.getValue().getViewCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 NOT_FOUND 예외")
    void getOne_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getOne(999L))
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
    @DisplayName("delete - 작성자가 정상 삭제한다")
    void delete_정상삭제() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        questionService.delete(author, 10L);

        verify(questionRepository).delete(question);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.delete(other, 10L))
                .isInstanceOf(QuestionHandler.class);
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
    @DisplayName("toggleLike - 좋아요가 없으면 새로 추가한다")
    void toggleLike_좋아요추가() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionLikeRepository.findAll()).thenReturn(List.of());

        questionService.toggleLike(other, 10L);

        verify(questionLikeRepository).save(any(QuestionLike.class));
        verify(questionLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("toggleLike - 이미 좋아요가 있으면 삭제한다")
    void toggleLike_좋아요취소() {
        QuestionLike existing = QuestionLike.builder().question(question).member(other).build();
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionLikeRepository.findAll()).thenReturn(List.of(existing));

        questionService.toggleLike(other, 10L);

        verify(questionLikeRepository).delete(existing);
        verify(questionLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleLike - 존재하지 않는 질문이면 NOT_FOUND 예외")
    void toggleLike_없음_예외() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.toggleLike(other, 999L))
                .isInstanceOf(QuestionHandler.class);
    }
}
