package cn.edu.gfkd.evidence.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import cn.edu.gfkd.evidence.service.EvidenceService;

@SpringBootTest @ActiveProfiles("test") @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EvidenceServiceIntegrationTest {

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private EvidenceRepository evidenceRepository;

    private String generateUniqueTransactionHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "") + "abcdef1234567890";
    }

    private String generateUniqueEvidenceId() {
        return "EVID:" + System.currentTimeMillis() + ":CN-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    private EvidenceEntity createTestEvidence() {
        return new EvidenceEntity(generateUniqueEvidenceId(),
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
    }

    @Test
    void createEvidence_ValidEvidence_CreatesEvidenceSuccessfully() {
        // Given
        String evidenceId = generateUniqueEvidenceId();
        String transactionHash = generateUniqueTransactionHash();
        EvidenceEntity evidence = new EvidenceEntity(evidenceId,
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), transactionHash, BigInteger.valueOf(1234567890),
                "Test evidence memo");

        // When
        EvidenceEntity savedEvidence = evidenceService.createEvidence(evidence);

        // Then
        assertThat(savedEvidence).isNotNull();
        assertThat(savedEvidence.getId()).isNotNull();
        assertThat(savedEvidence.getEvidenceId()).isEqualTo(evidenceId);
        assertThat(savedEvidence.getUserAddress())
                .isEqualTo("0x1234567890123456789012345678901234567890");
        assertThat(savedEvidence.getHashValue())
                .isEqualTo("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");

        // Verify evidence is persisted in database
        EvidenceEntity foundEvidence = evidenceRepository.findById(savedEvidence.getId())
                .orElse(null);
        assertThat(foundEvidence).isNotNull();
        assertThat(foundEvidence.getEvidenceId()).isEqualTo(evidenceId);
    }

    @Test
    void createEvidence_NullEvidence_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evidence cannot be null");
    }

    @Test
    void createEvidence_EmptyEvidenceId_ThrowsException() {
        // Given
        EvidenceEntity evidence = new EvidenceEntity("",
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evidence ID cannot be empty");
    }

    @Test
    void createEvidence_EmptyUserAddress_ThrowsException() {
        // Given
        EvidenceEntity evidence = new EvidenceEntity(generateUniqueEvidenceId(), "",
                "test_file.pdf", "application/pdf", 1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User address cannot be empty");
    }

    @Test
    void createEvidence_EmptyHashValue_ThrowsException() {
        // Given
        EvidenceEntity evidence = new EvidenceEntity(generateUniqueEvidenceId(),
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256", "", BigInteger.valueOf(100),
                generateUniqueTransactionHash(), BigInteger.valueOf(1234567890),
                "Test evidence memo");

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hash value cannot be empty");
    }

    @Test
    void getEvidenceById_ExistingEvidence_ReturnsEvidence() {
        // Given
        String evidenceId = generateUniqueEvidenceId();
        String transactionHash = generateUniqueTransactionHash();
        EvidenceEntity evidence = new EvidenceEntity(evidenceId,
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), transactionHash, BigInteger.valueOf(1234567890),
                "Test evidence memo");
        EvidenceEntity savedEvidence = evidenceRepository.save(evidence);

        // When
        var foundEvidence = evidenceService.getEvidenceById(savedEvidence.getId());

        // Then
        assertThat(foundEvidence).isPresent();
        assertThat(foundEvidence.get().getEvidenceId()).isEqualTo(evidenceId);
    }

    @Test
    void getEvidenceById_NonExistingEvidence_ReturnsEmpty() {
        // When
        var foundEvidence = evidenceService.getEvidenceById(999L);

        // Then
        assertThat(foundEvidence).isEmpty();
    }

    @Test
    void getEvidenceByEvidenceId_ExistingEvidence_ReturnsEvidence() {
        // Given
        String evidenceId = generateUniqueEvidenceId();
        EvidenceEntity evidence = new EvidenceEntity(evidenceId,
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
        evidenceRepository.save(evidence);

        // When
        var foundEvidence = evidenceService.getEvidenceByEvidenceId(evidenceId);

        // Then
        assertThat(foundEvidence).isPresent();
        assertThat(foundEvidence.get().getUserAddress())
                .isEqualTo("0x1234567890123456789012345678901234567890");
    }

    @Test
    void getEvidenceByEvidenceId_NonExistingEvidence_ReturnsEmpty() {
        // When
        var foundEvidence = evidenceService.getEvidenceByEvidenceId("NONEXISTENT");

        // Then
        assertThat(foundEvidence).isEmpty();
    }

    @Test
    void getEvidenceByUserAddress_ExistingUser_ReturnsEvidenceList() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";

        EvidenceEntity evidence1 = new EvidenceEntity(generateUniqueEvidenceId(), userAddress,
                "test_file.pdf", "application/pdf", 1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        EvidenceEntity evidence2 = new EvidenceEntity(generateUniqueEvidenceId(), userAddress,
                "test_image.jpg", "image/jpeg", 2048L, BigInteger.valueOf(1234567891), "SHA256",
                generateUniqueTransactionHash(), BigInteger.valueOf(101),
                generateUniqueTransactionHash(), BigInteger.valueOf(1234567891),
                "Test image evidence");

        evidenceRepository.save(evidence1);
        evidenceRepository.save(evidence2);

        // When
        List<EvidenceEntity> evidenceList = evidenceService.getEvidenceByUserAddress(userAddress);

        // Then
        assertThat(evidenceList).hasSize(2);
        assertThat(evidenceList).extracting("userAddress").containsOnly(userAddress);
    }

    @Test
    void getEvidenceByUserAddress_NonExistingUser_ReturnsEmptyList() {
        // When
        List<EvidenceEntity> evidenceList = evidenceService
                .getEvidenceByUserAddress("0xNonExistingAddress");

        // Then
        assertThat(evidenceList).isEmpty();
    }

    @Test
    void getEvidenceByUserAddressWithPagination_ReturnsPage() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";
        Pageable pageable = PageRequest.of(0, 10);

        EvidenceEntity evidence1 = new EvidenceEntity(generateUniqueEvidenceId(), userAddress,
                "test_file.pdf", "application/pdf", 1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        evidenceRepository.save(evidence1);

        // When
        Page<EvidenceEntity> evidencePage = evidenceService.getEvidenceByUserAddress(userAddress,
                pageable);

        // Then
        assertThat(evidencePage).isNotNull();
        assertThat(evidencePage.getContent()).hasSize(1);
        assertThat(evidencePage.getContent().get(0).getUserAddress()).isEqualTo(userAddress);
    }

    @Test
    void existsByEvidenceId_ExistingEvidence_ReturnsTrue() {
        // Given
        String evidenceId = generateUniqueEvidenceId();
        EvidenceEntity evidence = new EvidenceEntity(evidenceId,
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
        evidenceRepository.save(evidence);

        // When
        boolean exists = evidenceService.existsByEvidenceId(evidenceId);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEvidenceId_NonExistingEvidence_ReturnsFalse() {
        // When
        boolean exists = evidenceService.existsByEvidenceId("NONEXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void countByUserAddress_ExistingUser_ReturnsCount() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";

        EvidenceEntity evidence1 = new EvidenceEntity(generateUniqueEvidenceId(), userAddress,
                "test_file.pdf", "application/pdf", 1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        EvidenceEntity evidence2 = new EvidenceEntity(generateUniqueEvidenceId(), userAddress,
                "test_image.jpg", "image/jpeg", 2048L, BigInteger.valueOf(1234567891), "SHA256",
                generateUniqueTransactionHash(), BigInteger.valueOf(101),
                generateUniqueTransactionHash(), BigInteger.valueOf(1234567891),
                "Test image evidence");

        evidenceRepository.save(evidence1);
        evidenceRepository.save(evidence2);

        // When
        long count = evidenceService.countByUserAddress(userAddress);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUserAddress_NonExistingUser_ReturnsZero() {
        // When
        long count = evidenceService.countByUserAddress("0xNonExistingAddress");

        // Then
        assertThat(count).isZero();
    }

    @Test
    void updateEvidence_ValidEvidence_UpdatesEvidence() {
        // Given
        EvidenceEntity evidence = new EvidenceEntity(generateUniqueEvidenceId(),
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
        EvidenceEntity savedEvidence = evidenceRepository.save(evidence);
        savedEvidence.setStatus("verified");

        // When
        EvidenceEntity updatedEvidence = evidenceService.updateEvidence(savedEvidence);

        // Then
        assertThat(updatedEvidence.getStatus()).isEqualTo("verified");

        // Verify in database
        EvidenceEntity foundEvidence = evidenceRepository.findById(savedEvidence.getId())
                .orElse(null);
        assertThat(foundEvidence).isNotNull();
        assertThat(foundEvidence.getStatus()).isEqualTo("verified");
    }

    @Test
    void deleteEvidence_ExistingEvidence_DeletesEvidence() {
        // Given
        EvidenceEntity evidence = new EvidenceEntity(generateUniqueEvidenceId(),
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
        EvidenceEntity savedEvidence = evidenceRepository.save(evidence);

        // When
        evidenceService.deleteEvidence(savedEvidence.getId());

        // Then
        assertThat(evidenceRepository.findById(savedEvidence.getId())).isEmpty();
    }

    @Test
    void deleteEvidence_NonExistingEvidence_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> evidenceService.deleteEvidence(999L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Evidence not found");
    }

    @Test
    void deleteEvidenceByEvidenceId_ExistingEvidence_DeletesEvidence() {
        // Given
        String evidenceId = generateUniqueEvidenceId();
        EvidenceEntity evidence = new EvidenceEntity(evidenceId,
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");
        evidenceRepository.save(evidence);

        // When
        evidenceService.deleteEvidenceByEvidenceId(evidenceId);

        // Then
        assertThat(evidenceRepository.findByEvidenceId(generateUniqueEvidenceId())).isEmpty();
    }

    @Test
    void deleteEvidenceByEvidenceId_NonExistingEvidence_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> evidenceService.deleteEvidenceByEvidenceId("NONEXISTENT"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Evidence not found");
    }

    @Test
    void count_ReturnsTotalCount() {
        // Given
        EvidenceEntity evidence1 = new EvidenceEntity(generateUniqueEvidenceId(),
                "0x1234567890123456789012345678901234567890", "test_file.pdf", "application/pdf",
                1024L, BigInteger.valueOf(1234567890), "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567890), "Test evidence memo");

        EvidenceEntity evidence2 = new EvidenceEntity(generateUniqueEvidenceId(),
                "0x0987654321098765432109876543210987654321", "test_image.jpg", "image/jpeg", 2048L,
                BigInteger.valueOf(1234567891), "SHA256", generateUniqueTransactionHash(),
                BigInteger.valueOf(101), generateUniqueTransactionHash(),
                BigInteger.valueOf(1234567891), "Test image evidence");

        evidenceRepository.save(evidence1);
        evidenceRepository.save(evidence2);

        // When
        long count = evidenceService.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void count_EmptyDatabase_ReturnsZero() {
        // When
        long count = evidenceService.count();

        // Then
        assertThat(count).isZero();
    }
}