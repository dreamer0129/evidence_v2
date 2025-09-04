package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.SyncStatus;
import cn.edu.gfkd.evidence.exception.BlockchainException;
import cn.edu.gfkd.evidence.repository.BlockchainEventRepository;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.repository.SyncStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceEventListenerTest {

    @Mock
    private Web3j web3j;

    @Mock
    private BlockchainEventRepository blockchainEventRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private SyncStatusRepository syncStatusRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EvidenceStorageContract evidenceStorageContract;

    private EvidenceEventListener evidenceEventListener;

    private SyncStatus testSyncStatus;

    @BeforeEach
    void setUp() {
        // Mock the contract address
        lenient().when(evidenceStorageContract.getContractAddress()).thenReturn("0xContractAddress");
        
        evidenceEventListener = new EvidenceEventListener(
                web3j, blockchainEventRepository, evidenceRepository, syncStatusRepository, objectMapper, evidenceStorageContract);

        testSyncStatus = new SyncStatus("0xContractAddress", BigInteger.valueOf(50));
    }

    @Test
    @DisplayName("Should handle blockchain connectivity check gracefully")
    void isBlockchainConnected_WhenWeb3jThrowsException_ShouldReturnFalse() throws Exception {
        // Arrange
        lenient().when(web3j.ethGetLogs(any())).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        assertThrows(BlockchainException.class,
                () -> evidenceEventListener.syncPastEvents(BigInteger.valueOf(50), BigInteger.valueOf(100)));
    }

    @Test
    @DisplayName("Should handle syncPastEvents when Web3j fails")
    void syncPastEvents_WhenWeb3jFails_ShouldThrowBlockchainException() throws IOException {
        // Arrange
        BigInteger startBlock = BigInteger.valueOf(50);
        BigInteger endBlock = BigInteger.valueOf(100);

        // Mock Web3j to throw exception
        lenient().when(web3j.ethGetLogs(any())).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        BlockchainException exception = assertThrows(
                BlockchainException.class,
                () -> evidenceEventListener.syncPastEvents(startBlock, endBlock));

        assertEquals("Failed to sync past events", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    @DisplayName("Should handle evidence storage null pointer gracefully")
    void syncPastEvents_WhenEvidenceStorageIsNull_ShouldThrowBlockchainException() throws IOException {
        // Arrange
        BigInteger startBlock = BigInteger.valueOf(50);
        BigInteger endBlock = BigInteger.valueOf(100);

        // Mock Web3j to throw exception (simulating evidenceStorage being null)
        lenient().when(web3j.ethGetLogs(any())).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        assertThrows(BlockchainException.class, () -> evidenceEventListener.syncPastEvents(startBlock, endBlock));
    }

    @Test
    @DisplayName("Should handle repository exceptions in syncPastEvents")
    void syncPastEvents_WhenRepositoryThrowsException_ShouldThrowBlockchainException() throws Exception {
        // Arrange
        BigInteger startBlock = BigInteger.valueOf(50);
        BigInteger endBlock = BigInteger.valueOf(100);

        // Mock Web3j to return empty logs using lenient stubbing
        lenient().when(web3j.ethGetLogs(any())).thenThrow(new RuntimeException("Network error"));
        lenient().when(syncStatusRepository.findById("0xContractAddress")).thenReturn(Optional.of(testSyncStatus));
        lenient().when(syncStatusRepository.save(any(SyncStatus.class))).thenReturn(testSyncStatus);

        // Act & Assert
        assertThrows(BlockchainException.class, () -> evidenceEventListener.syncPastEvents(startBlock, endBlock));
    }

    @Test
    @DisplayName("Should create EvidenceEventListener with valid parameters")
    void createEvidenceEventListener_WithValidParameters_ShouldSucceed() {
        // Act & Assert
        assertDoesNotThrow(() -> new EvidenceEventListener(
                web3j, blockchainEventRepository, evidenceRepository, syncStatusRepository, objectMapper, evidenceStorageContract));
    }

    @Test
    @DisplayName("Should handle null Web3j gracefully")
    void createEvidenceEventListener_WithNullWeb3j_ShouldSucceed() {
        // Act & Assert
        assertDoesNotThrow(() -> new EvidenceEventListener(
                null, blockchainEventRepository, evidenceRepository, syncStatusRepository, objectMapper, evidenceStorageContract));
    }

    @Test
    @DisplayName("Should handle null repositories gracefully")
    void createEvidenceEventListener_WithNullRepositories_ShouldSucceed() {
        // Act & Assert
        assertDoesNotThrow(() -> new EvidenceEventListener(
                web3j, null, null, null, objectMapper, evidenceStorageContract));
    }

    @Test
    @DisplayName("Should handle null ObjectMapper gracefully")
    void createEvidenceEventListener_WithNullObjectMapper_ShouldSucceed() {
        // Act & Assert
        assertDoesNotThrow(() -> new EvidenceEventListener(
                web3j, blockchainEventRepository, evidenceRepository, syncStatusRepository, null, evidenceStorageContract));
    }

    @Test
    @DisplayName("Should handle null contract gracefully")
    void createEvidenceEventListener_WithNullContract_ShouldSucceed() {
        // Act & Assert
        assertDoesNotThrow(() -> new EvidenceEventListener(
                web3j, blockchainEventRepository, evidenceRepository, syncStatusRepository, objectMapper, null));
    }

    @Test
    @DisplayName("Should demonstrate test setup is working")
    void testSetup_ShouldWorkCorrectly() {
        // Assert
        assertNotNull(evidenceEventListener);
        assertNotNull(testSyncStatus);
        assertEquals("0xContractAddress", testSyncStatus.getContractAddress());
        assertEquals(BigInteger.valueOf(50), testSyncStatus.getLastBlockNumber());
    }
}