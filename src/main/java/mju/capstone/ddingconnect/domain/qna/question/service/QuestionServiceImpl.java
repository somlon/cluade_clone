package mju.capstone.ddingconnect.domain.qna.question.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionLike;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionLikeRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionLikeRepository questionLikeRepository;
    private final AnswerRepository answerRepository;
    private final AnswerAlarmRepository answerAlarmRepository;
    private final AnswerLikeRepository answerLikeRepository;

    @Override
    @Transactional
    public QuestionResponse create(Member member, CreateQuestionRequest request) {
        Question question = Question.builder()
                .member(member)
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .viewCount(0)
                .build();
        Question saved = questionRepository.save(question);
        return toResponse(saved, member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getList(Member member) {
        return questionRepository.findAll()
                .stream().map(q -> toResponse(q, member)).toList();
    }

    @Override
    @Transactional
    public QuestionResponse getOne(Member member, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        // 조회수 증가
        Question updated = Question.builder()
                .id(question.getId())
                .member(question.getMember())
                .category(question.getCategory())
                .title(question.getTitle())
                .content(question.getContent())
                .viewCount(question.getViewCount() + 1)
                .build();

        Question saved = questionRepository.save(updated);
        return toResponse(saved, member);
    }

    @Override
    @Transactional
    public QuestionResponse update(Member member, Long questionId, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        if (!question.getMember().getId().equals(member.getId())) {
            throw new QuestionHandler(ErrorStatus.QUESTION_UNAUTHORIZED);
        }

        Question updated = Question.builder()
                .id(question.getId())
                .member(question.getMember())
                .category(request.category() != null ? request.category() : question.getCategory())
                .title(request.title() != null ? request.title() : question.getTitle())
                .content(request.content() != null ? request.content() : question.getContent())
                .viewCount(question.getViewCount())
                .build();

        Question saved = questionRepository.save(updated);
        return toResponse(saved, member);
    }

    @Override
    @Transactional
    public void delete(Member member, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        if (!question.getMember().getId().equals(member.getId())) {
            throw new QuestionHandler(ErrorStatus.QUESTION_UNAUTHORIZED);
        }

        // 계층 자식 정리 (FK 제약 방지):
        // Answer 의 손자(AnswerAlarm, AnswerLike) → Answer → QuestionLike → Question 순.
        // bulk @Modifying 쿼리이므로 영속성 컨텍스트에 자식이 로드되지 않은 상태에서 직접 DB 삭제.
        answerAlarmRepository.deleteByQuestionId(questionId);
        answerLikeRepository.deleteByQuestionId(questionId);
        answerRepository.deleteByQuestionId(questionId);
        questionLikeRepository.deleteByQuestionId(questionId);
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public LikeToggleResponse toggleLike(Member member, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        // 본인이 작성한 질문에는 좋아요 불가
        if (question.getMember().getId().equals(member.getId())) {
            throw new QuestionHandler(ErrorStatus.QUESTION_SELF_LIKE);
        }

        questionLikeRepository.findAll().stream()
                .filter(like -> like.getQuestion().getId().equals(questionId)
                        && like.getMember().getId().equals(member.getId()))
                .findFirst()
                .ifPresentOrElse(
                        questionLikeRepository::delete,
                        () -> questionLikeRepository.save(
                                QuestionLike.builder().question(question).member(member).build())
                );

        boolean liked = questionLikeRepository.existsByMemberIdAndQuestionId(member.getId(), questionId);
        long likeCount = questionLikeRepository.countByQuestionId(questionId);
        return new LikeToggleResponse(liked, likeCount);
    }

    private QuestionResponse toResponse(Question question, Member member) {
        long likeCount = questionLikeRepository.countByQuestionId(question.getId());
        long answerCount = answerRepository.countByQuestionId(question.getId());
        boolean likedByMe = member != null
                && questionLikeRepository.existsByMemberIdAndQuestionId(member.getId(), question.getId());
        return QuestionResponse.from(question, likeCount, answerCount, likedByMe);
    }
}
