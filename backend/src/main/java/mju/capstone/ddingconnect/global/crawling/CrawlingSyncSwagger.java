package mju.capstone.ddingconnect.global.crawling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "크롤링 싱크", description = "크롤링 싱크 수동 트리거 API")
public interface CrawlingSyncSwagger {

    @Operation(
            summary = "크롤링 싱크 수동 트리거",
            description = "크롤링 서버에 싱크 요청을 수동으로 발송합니다."
    )
    @PostMapping("/sync")
    ApiResponse<String> triggerSync();
}
