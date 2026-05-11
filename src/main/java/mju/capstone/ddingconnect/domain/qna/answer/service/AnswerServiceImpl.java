package mju.capstone.ddingconnect.domain.qna.answer.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLike;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLikeId;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AnswerHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.QuestionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerLikeRepository answerLikeRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public AnswerResponse create(Member member, Long questionId, CreateAnswerRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        Answer answer = Answer.builder()
                .question(question)
                .member(member)
                .content(request.content())
                .build();

        return AnswerResponse.from(answerRepository.save(answer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getList(Long questionId) {
        return answerRepository.findByQuestionId(questionId)
                .stream().map(AnswerResponse::from).toList();
    }

    @Override
    @Transactional
    public AnswerResponse update(Member member, Long answerId, UpdateAnswerRequest request) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

        if (!answer.getMember().getId().equals(member.getId())) {
            throw new AnswerHandler(ErrorStatus.ANSWER_UNAUTHORIZED);
        }

        Answer updated = Answer.builder()
                .id(answer.getId())
                .question(answer.getQuestion())
                .member(answer.getMember())
                .content(request.content() != null ? request.content() : answer.getContent())
                .build();

        return AnswerResponse.from(answerRepository.save(updated));
    }

    @Override
    @Transactional
    public void delete(Member member, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

        if (!answer.getMember().getId().equals(member.getId())) {
            throw new AnswerHandler(ErrorStatus.ANSWER_UNAUTHORIZED);
        }

        answerRepository.delete(answer);
    }

    @Override
    @Transactional
    public void toggleLike(Member member, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

        AnswerLikeId likeId = new AnswerLikeId(answerId, member.getId());

        answerLikeRepository.findById(likeId)
                .ifPresentOrElse(
                        answerLikeRepository::delete,
                        () -> answerLikeRepository.save(
                                AnswerLike.builder().answer(answer).member(member).build())
                );
    }
}
