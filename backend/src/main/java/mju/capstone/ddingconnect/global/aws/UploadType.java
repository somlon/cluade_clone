package mju.capstone.ddingconnect.global.aws;

import java.util.Set;

/**
 * [범용 파일 업로드 용도 구분]
 *
 * presigned PUT 업로드 엔드포인트({@code POST /api/v1/files/presigned-url})가 받는 업로드 용도.
 * 용도별 content-type 화이트리스트를 단일 정의로 보유한다 — 회원 도메인의
 * 프로필/명함/포트폴리오 업로드 검증도 이 값을 참조해 중복·하드코딩을 막는다.
 *
 * <ul>
 *   <li>{@link #IMAGE} — 프로필 사진·명함 이미지 등: png/jpeg/webp</li>
 *   <li>{@link #PORTFOLIO} — 포트폴리오 문서: pdf</li>
 * </ul>
 */
public enum UploadType {

    IMAGE(Set.of("image/png", "image/jpeg", "image/webp")),
    PORTFOLIO(Set.of("application/pdf"));

    private final Set<String> allowedContentTypes;

    UploadType(Set<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    /** 이 용도가 허용하는 content-type 집합. */
    public Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    /** 주어진 content-type 이 이 용도의 화이트리스트에 속하는지 여부. */
    public boolean allows(String contentType) {
        return contentType != null && allowedContentTypes.contains(contentType);
    }
}
