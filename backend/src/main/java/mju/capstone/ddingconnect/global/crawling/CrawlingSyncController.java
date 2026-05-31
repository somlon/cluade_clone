package mju.capstone.ddingconnect.global.crawling;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crawling")
@RequiredArgsConstructor
public class CrawlingSyncController implements CrawlingSyncSwagger {

    private final CrawlingSyncClient crawlingSyncClient;

    @PostMapping("/sync")
    public ApiResponse<String> triggerSync() {
        crawlingSyncClient.sync();
        return ApiResponse.onSuccess(SuccessMessage.CRAWLING_SYNC_TRIGGERED);
    }
}
