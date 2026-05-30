package mju.capstone.ddingconnect.global.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * [증명서 OCR 인증 응답]
 *
 * 데이터 파트({@code ddingconnect-data}) 의 {@code POST /api/data/verify} 응답과 정합
 * (ddingconnect-data PR #13 스키마). 백엔드는 {@code is_approved} 와 {@code student_info}
 * (이름·학과·학년)만 사용하고 {@code user_id}/{@code message}/{@code details} 는 무시한다
 * ({@code @JsonIgnoreProperties(ignoreUnknown = true)} 로 미사용 필드를 안전하게 흘려보낸다).
 *
 * <pre>{@code
 * 승인: { "status":"success", "is_approved":true,
 *         "student_info":{ "type":"재학생"|"졸업생", "name":"...", "department":"...", "grade":"4" } }
 * 미승인: { "status":"fail", "is_approved":false, "details":{ ... } }   // student_info 없음
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CertificateVerifyResponse(
        String status,
        @JsonProperty("is_approved") boolean isApproved,
        @JsonProperty("student_info") StudentInfo studentInfo
) {

    /**
     * 증명서에서 추출한 학생 정보. {@code type} 은 "재학생"/"졸업생".
     * {@code grade} 는 재학생일 때만 존재하며 문자열("1"~"4")로 내려온다(졸업생은 키 자체가 없어 null).
     * {@code name}/{@code department} 은 OCR 파싱 실패 시 승인이어도 null 일 수 있다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StudentInfo(
            String type,
            String name,
            String department,
            String grade
    ) {

        /**
         * {@code grade} 문자열을 {@link Integer} 로 파싱한다.
         * null/blank/숫자 형식 오류는 모두 null 로 반환한다(가입을 막지 않는 best-effort 변환).
         */
        public Integer gradeAsInt() {
            if (grade == null || grade.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(grade.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
