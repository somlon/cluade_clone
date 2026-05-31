package mju.capstone.ddingconnect.global.crawling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlingSyncScheduler {

    private final CrawlingSyncClient crawlingSyncClient;

    @Scheduled(cron = "${crawling.sync.cron}")
    public void scheduledSync() {
        log.info("크롤링 싱크 스케줄 실행");
        try {
            crawlingSyncClient.sync();
            log.info("크롤링 싱크 완료");
        } catch (Exception e) {
            log.error("크롤링 싱크 실패: {}", e.getMessage());
        }
    }
}
