package mju.capstone.ddingconnect.domain.qna.question.dto.response;

/**
 * 좋아요 토글 응답.
 * 토글 직후의 새로운 상태(liked)와 갱신된 카운트(likeCount)를 함께 반환해
 * 프론트가 추가 GET 없이 UI 를 동기화할 수 있게 한다.
 */
public record LikeToggleResponse(Boolean liked, Long likeCount) {}
