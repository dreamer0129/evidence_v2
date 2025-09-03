package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.Evidence;
import cn.edu.gfkd.evidence.repository.EvidenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EvidenceServiceIntegrationTest {

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Test
    void createEvidence_ValidEvidence_CreatesEvidenceSuccessfully() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );

        // When
        Evidence savedEvidence = evidenceService.createEvidence(evidence);

        // Then
        assertThat(savedEvidence).isNotNull();
        assertThat(savedEvidence.getId()).isNotNull();
        assertThat(savedEvidence.getEvidenceId()).isEqualTo("EVID:1234567890:CN-001");
        assertThat(savedEvidence.getUserAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
        assertThat(savedEvidence.getHashValue()).isEqualTo("0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");

        // Verify evidence is persisted in database
        Evidence foundEvidence = evidenceRepository.findById(savedEvidence.getId()).orElse(null);
        assertThat(foundEvidence).isNotNull();
        assertThat(foundEvidence.getEvidenceId()).isEqualTo("EVID:1234567890:CN-001");
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
        Evidence evidence = new Evidence(
                "",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evidence ID cannot be empty");
    }

    @Test
    void createEvidence_EmptyUserAddress_ThrowsException() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User address cannot be empty");
    }

    @Test
    void createEvidence_EmptyHashValue_ThrowsException() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );

        // When & Then
        assertThatThrownBy(() -> evidenceService.createEvidence(evidence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hash value cannot be empty");
    }

    @Test
    void getEvidenceById_ExistingEvidence_ReturnsEvidence() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        Evidence savedEvidence = evidenceRepository.save(evidence);

        // When
        var foundEvidence = evidenceService.getEvidenceById(savedEvidence.getId());

        // Then
        assertThat(foundEvidence).isPresent();
        assertThat(foundEvidence.get().getEvidenceId()).isEqualTo("EVID:1234567890:CN-001");
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
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        evidenceRepository.save(evidence);

        // When
        var foundEvidence = evidenceService.getEvidenceByEvidenceId("EVID:1234567890:CN-001");

        // Then
        assertThat(foundEvidence).isPresent();
        assertThat(foundEvidence.get().getUserAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
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
        
        Evidence evidence1 = new Evidence(
                "EVID:1234567890:CN-001",
                userAddress,
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        
        Evidence evidence2 = new Evidence(
                "EVID:1234567891:CN-002",
                userAddress,
                "test_image.jpg",
                "image/jpeg",
                2048L,
                BigInteger.valueOf(1234567891),
                "SHA256",
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(101),
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(1234567891),
                "Test image evidence"
        );
        
        evidenceRepository.save(evidence1);
        evidenceRepository.save(evidence2);

        // When
        List<Evidence> evidenceList = evidenceService.getEvidenceByUserAddress(userAddress);

        // Then
        assertThat(evidenceList).hasSize(2);
        assertThat(evidenceList).extracting("userAddress").containsOnly(userAddress);
    }

    @Test
    void getEvidenceByUserAddress_NonExistingUser_ReturnsEmptyList() {
        // When
        List<Evidence> evidenceList = evidenceService.getEvidenceByUserAddress("0xNonExistingAddress");

        // Then
        assertThat(evidenceList).isEmpty();
    }

    @Test
    void getEvidenceByUserAddressWithPagination_ReturnsPage() {
        // Given
        String userAddress = "0x1234567890123456789012345678901234567890";
        Pageable pageable = PageRequest.of(0, 10);
        
        Evidence evidence1 = new Evidence(
                "EVID:1234567890:CN-001",
                userAddress,
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        
        evidenceRepository.save(evidence1);

        // When
        Page<Evidence> evidencePage = evidenceService.getEvidenceByUserAddress(userAddress, pageable);

        // Then
        assertThat(evidencePage).isNotNull();
        assertThat(evidencePage.getContent()).hasSize(1);
        assertThat(evidencePage.getContent().get(0).getUserAddress()).isEqualTo(userAddress);
    }

    @Test
    void existsByEvidenceId_ExistingEvidence_ReturnsTrue() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        evidenceRepository.save(evidence);

        // When
        boolean exists = evidenceService.existsByEvidenceId("EVID:1234567890:CN-001");

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
        
        Evidence evidence1 = new Evidence(
                "EVID:1234567890:CN-001",
                userAddress,
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        
        Evidence evidence2 = new Evidence(
                "EVID:1234567891:CN-002",
                userAddress,
                "test_image.jpg",
                "image/jpeg",
                2048L,
                BigInteger.valueOf(1234567891),
                "SHA256",
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(101),
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(1234567891),
                "Test image evidence"
        );
        
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
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        Evidence savedEvidence = evidenceRepository.save(evidence);
        savedEvidence.setStatus("verified");

        // When
        Evidence updatedEvidence = evidenceService.updateEvidence(savedEvidence);

        // Then
        assertThat(updatedEvidence.getStatus()).isEqualTo("verified");
        
        // Verify in database
        Evidence foundEvidence = evidenceRepository.findById(savedEvidence.getId()).orElse(null);
        assertThat(foundEvidence).isNotNull();
        assertThat(foundEvidence.getStatus()).isEqualTo("verified");
    }

    @Test
    void deleteEvidence_ExistingEvidence_DeletesEvidence() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        Evidence savedEvidence = evidenceRepository.save(evidence);

        // When
        evidenceService.deleteEvidence(savedEvidence.getId());

        // Then
        assertThat(evidenceRepository.findById(savedEvidence.getId())).isEmpty();
    }

    @Test
    void deleteEvidence_NonExistingEvidence_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> evidenceService.deleteEvidence(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evidence not found");
    }

    @Test
    void deleteEvidenceByEvidenceId_ExistingEvidence_DeletesEvidence() {
        // Given
        Evidence evidence = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        evidenceRepository.save(evidence);

        // When
        evidenceService.deleteEvidenceByEvidenceId("EVID:1234567890:CN-001");

        // Then
        assertThat(evidenceRepository.findByEvidenceId("EVID:1234567890:CN-001")).isEmpty();
    }

    @Test
    void deleteEvidenceByEvidenceId_NonExistingEvidence_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> evidenceService.deleteEvidenceByEvidenceId("NONEXISTENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evidence not found");
    }

    @Test
    void count_ReturnsTotalCount() {
        // Given
        Evidence evidence1 = new Evidence(
                "EVID:1234567890:CN-001",
                "0x1234567890123456789012345678901234567890",
                "test_file.pdf",
                "application/pdf",
                1024L,
                BigInteger.valueOf(1234567890),
                "SHA256",
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                BigInteger.valueOf(100),
                "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                BigInteger.valueOf(1234567890),
                "Test evidence memo"
        );
        
        Evidence evidence2 = new Evidence(
                "EVID:1234567891:CN-002",
                "0x0987654321098765432109876543210987654321",
                "test_image.jpg",
                "image/jpeg",
                2048L,
                BigInteger.valueOf(1234567891),
                "SHA256",
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(101),
                "0x0987654321fedcba0987654321fedcba0987654321fedcba0987654321fedcba",
                BigInteger.valueOf(1234567891),
                "Test image evidence"
        );
        
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