package mju.capstone.ddingconnect.global.response.exception.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mju.capstone.ddingconnect.global.response.code.status.SuccessStatus;

@Getter
@AllArgsConstructor //모든 요소를 담은 생성자 자동 생성
@JsonPropertyOrder({"isSuccess", "code", "message", "result"}) //json의 순서
//Json을 직렬화 할 때 프로퍼티의 순서를 명시하기 위한 에노테이션
public class ApiResponse<T> {
    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    //해당 에노테이션을 붙이면, null인 경우, 노출하지 않는다.
    private T result;

    //성공한 경우 응답 생성
    //입력받은 결과를 포함시켜서 응답을 생성한 후, 반환한다.
    public static <T> ApiResponse<T> onSuccess(T result) {
        //자동 생성자를 통해 생성, + 미리 준비된 ENUM 타입을 이용한다.
        return new ApiResponse<>(true, SuccessStatus._OK.getCode(), SuccessStatus._OK.getMessage(), result);
    }

    //실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
        //생성자 생섲으로, false를 담고 있는 response를 생성한다.

    }
}
