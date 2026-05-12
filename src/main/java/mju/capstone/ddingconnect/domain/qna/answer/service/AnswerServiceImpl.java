package mju.capstone.ddingconnect.domain.qna.answer.service;

import lombok.RequiredArgsConstructor;
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
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
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
    private final AnswerAlarmRepository answerAlarmRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public AnswerResponse create(Member member, Long questionId, CreateAnswerRequest request) {
        // 답변은 졸업생만 등록 가능 (도메인 정책)
        if (member.getRole() != MemberRole.GRADUATE) {
            throw new AnswerHandler(ErrorStatus.ANSWER_NOT_GRADUATE);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

        Answer answer = Answer.builder()
                .question(question)
                .member(member)
                .content(request.content())
                .build();

        Answer saved = answerRepository.save(answer);

        // [알람 발행] 본인이 본인 질문에 답변한 경우는 제외
        if (!question.getMember().getId().equals(member.getId())) {
            answerAlarmRepository.save(AnswerAlarm.builder()
                    .answer(saved)
                    .content("내 질문에 새로운 답변이 달렸습니다.")
                    .isRead(false)
                    .build());
        }

        // 새 답변이라 likeCount=0, likedByMe=false 가 자명
        return AnswerResponse.from(saved, 0L, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getList(Member member, Long questionId) {
        return answerRepository.findByQuestionId(questionId)
                .stream().map(a -> toResponse(a, member)).toList();
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

        Answer saved = answerRepository.save(updated);
        return toResponse(saved, member);
    }

    @Override
    @Transactional
    public void delete(Member member, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

        if (!answer.getMember().getId().equals(member.getId())) {
            throw new AnswerHandler(ErrorStatus.ANSWER_UNAUTHORIZED);
        }

        // Answer 를 NOT NULL FK 로 참조하는 자식 먼저 삭제 (FK 제약 방지)
        answerAlarmRepository.deleteByAnswerId(answerId);
        answerLikeRepository.deleteByAnswerId(answerId);
        answerRepository.delete(answer);
    }

    @Override
    @Transactional
    public LikeToggleResponse toggleLike(Member member, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

        // 본인이 작성한 답변에는 좋아요 불가
        if (answer.getMember().getId().equals(member.getId())) {
            throw new AnswerHandler(ErrorStatus.ANSWER_SELF_LIKE);
        }

        AnswerLikeId likeId = new AnswerLikeId(answerId, member.getId());

        answerLikeRepository.findById(likeId)
                .ifPresentOrElse(
                        answerLikeRepository::delete,
                        () -> answerLikeRepository.save(
                                AnswerLike.builder().answer(answer).member(member).build())
                );

        boolean liked = answerLikeRepository.existsByMemberIdAndAnswerId(member.getId(), answerId);
        long likeCount = answerLikeRepository.countByAnswerId(answerId);
        return new LikeToggleResponse(liked, likeCount);
    }

    private AnswerResponse toResponse(Answer answer, Member member) {
        long likeCount = answerLikeRepository.countByAnswerId(answer.getId());
        boolean likedByMe = member != null
                && answerLikeRepository.existsByMemberIdAndAnswerId(member.getId(), answer.getId());
        return AnswerResponse.from(answer, likeCount, likedByMe);
    }
}
