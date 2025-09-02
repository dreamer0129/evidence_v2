package cn.edu.gfkd.evidence.config;

import cn.edu.gfkd.evidence.service.EvidenceEventListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BlockchainListenerConfig {

    private final EvidenceEventListener blockchainEventListener;

    @PostConstruct
    public void initBlockchainListener() {
        try {
            log.info("Initializing blockchain event listener...");
            blockchainEventListener.init();
            log.info("Blockchain event listener initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize blockchain event listener", e);
            // Don't throw the exception to allow the application to start
            // The listener will retry initialization
        }
    }
}