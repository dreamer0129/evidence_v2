package cn.edu.gfkd.evidence.integration;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import cn.edu.gfkd.evidence.service.BlockchainSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BlockchainSyncIntegrationTest {
    
    @Container
    public static GenericContainer<?> hardhatNode = new GenericContainer<>(
        DockerImageName.parse("ethereum/client-go:stable")
    ).withExposedPorts(8545);
    
    @Autowired
    private EvidenceRepository evidenceRepository;
    
    @Autowired
    private SyncStatusRepository syncStatusRepository;
    
    @Autowired
    private BlockchainSyncService blockchainSyncService;
    
    @Test
    void testBlockchainEventSyncIntegration() throws InterruptedException {
        // Given
        String contractAddress = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512";
        
        // Wait for Hardhat node to be ready
        TimeUnit.SECONDS.sleep(5);
        
        // When
        // Start the sync service
        blockchainSyncService.startEventListening();
        
        // Wait for some time to allow events to be processed
        TimeUnit.SECONDS.sleep(10);
        
        // Then
        // Check if sync status was created
        SyncStatus syncStatus = syncStatusRepository.findById(contractAddress)
            .orElse(null);
        
        assertNotNull(syncStatus);
        assertTrue(syncStatus.getLastBlockNumber().compareTo(BigInteger.ZERO) > 0);
        
        // Note: In a real test, you would deploy a test contract and emit test events
        // Then verify that the events were properly synchronized to the database
    }
    
    @Test
    void testDatabaseEntityCreation() {
        // Given
        Evidence evidence = new Evidence(
            "EVID:1234567890:CN-001",
            "0x1234567890123456789012345678901234567890",
            "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            BigInteger.valueOf(100),
            "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            BigInteger.valueOf(1234567890)
        );
        
        // When
        Evidence savedEvidence = evidenceRepository.save(evidence);
        
        // Then
        assertNotNull(savedEvidence.getId());
        assertEquals(evidence.getEvidenceId(), savedEvidence.getEvidenceId());
        assertEquals(evidence.getUserAddress(), savedEvidence.getUserAddress());
        assertEquals(evidence.getHashValue(), savedEvidence.getHashValue());
        assertEquals("effective", savedEvidence.getStatus());
    }
}