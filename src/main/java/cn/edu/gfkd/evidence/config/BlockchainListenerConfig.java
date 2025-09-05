package cn.edu.gfkd.evidence.config;

import cn.edu.gfkd.evidence.service.EventOrchestratorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BlockchainListenerConfig {

    private final EventOrchestratorService eventOrchestratorService;

    @PostConstruct
    public void initBlockchainListener() {
        try {
            log.info("Initializing blockchain event listener...");
            eventOrchestratorService.init();
            log.info("Blockchain event listener initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize blockchain event listener", e);
            // Don't throw the exception to allow the application to start
            // The listener will retry initialization
        }
    }
}