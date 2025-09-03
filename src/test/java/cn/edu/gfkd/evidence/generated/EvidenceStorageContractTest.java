package cn.edu.gfkd.evidence.generated;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import cn.edu.gfkd.evidence.generated.EvidenceStorageContract.Evidence;

@SpringBootTest
public class EvidenceStorageContractTest {

        @Autowired
        private Credentials credentials;

        @Autowired
        private EvidenceStorageContract evidenceStorage;

        @Test
        @DisplayName("Test getEvidenceByHash function")
        void testGetEvidence() throws Exception {
                // Generate random file name for testing
                String testFileName = "test_file_" + System.currentTimeMillis() + ".txt";

                // Create test evidence data
                String userAddress = credentials.getAddress();

                // Create random HashInfo to avoid hash conflicts in repeated tests
                byte[] testHash = new byte[32];
                Random random = new Random(System.currentTimeMillis());
                random.nextBytes(testHash);
                EvidenceStorageContract.HashInfo hashInfo = new EvidenceStorageContract.HashInfo("SHA-256", testHash);

                // First verify hash doesn't exist
                Boolean hashExistsBefore = evidenceStorage.doesHashExist(testHash).send();
                assertFalse(hashExistsBefore, "Hash should not exist before creation");

                // Submit evidence using submitHashEvidence
                TransactionReceipt receipt = evidenceStorage.submitHashEvidence(
                                testFileName,
                                hashInfo,
                                "Test evidence for unit testing").send();

                assertNotNull(receipt, "Transaction receipt should not be null");
                assertTrue(receipt.isStatusOK(), "Transaction should be successful");

                // Verify hash exists after creation
                Boolean hashExistsAfter = evidenceStorage.doesHashExist(testHash).send();
                assertTrue(hashExistsAfter, "Hash should exist after creation");

                // Test getEvidenceByHash function
                EvidenceStorageContract.Evidence evidence = evidenceStorage.getEvidenceByHash(testHash).send();
                assertNotNull(evidence, "Retrieved evidence should not be null");

                // Get the generated evidenceId from the event
                List<EvidenceStorageContract.EvidenceSubmittedEventResponse> events = EvidenceStorageContract
                                .getEvidenceSubmittedEvents(receipt);
                assertEquals(1, events.size(), "Should have one EvidenceSubmitted event");

                EvidenceStorageContract.EvidenceSubmittedEventResponse event = events.get(0);
                String generatedEvidenceId = event.evidenceId;

                // Verify evidence properties
                assertEquals(generatedEvidenceId, evidence.evidenceId, "Evidence ID should match");
                assertEquals(userAddress.toLowerCase(), evidence.userId.toLowerCase(), "User address should match");
                assertEquals(testFileName, evidence.metadata.fileName, "File name should match");
                assertEquals("", evidence.metadata.mimeType, "MIME type should be empty for submitHashEvidence");
                assertEquals(BigInteger.ZERO, evidence.metadata.size, "File size should be 0 for submitHashEvidence");
                assertEquals("SHA-256", evidence.hash.algorithm, "Hash algorithm should match");
                assertArrayEquals(testHash, evidence.hash.value, "Hash value should match");
                assertTrue(evidence.exists, "Evidence should exist");
                assertEquals("Test evidence for unit testing", evidence.memo, "Memo should match");

                // Verify event data
                assertEquals(userAddress.toLowerCase(), event.user.toLowerCase(), "Event user should match");
                assertEquals(generatedEvidenceId, event.evidenceId, "Event evidence ID should match");
                assertArrayEquals(testHash, event.hashValue, "Event hash should match");

                // Also verify that we can get evidence by the generated ID using getEvidence
                EvidenceStorageContract.Evidence evidenceById = evidenceStorage.getEvidence(generatedEvidenceId).send();
                assertNotNull(evidenceById, "Should be able to get evidence by ID");
                assertEquals(evidence.evidenceId, evidenceById.evidenceId, "Same evidence should be returned");
                assertArrayEquals(evidence.hash.value, evidenceById.hash.value, "Same hash should be returned");

                Evidence evidence2 = evidenceStorage.getEvidence(generatedEvidenceId).send();
                assertNotNull(evidence2, "Should be able to get evidence by ID");
                assertEquals(evidence.evidenceId, evidence2.evidenceId, "Same evidence should be returned");
                assertArrayEquals(evidence.hash.value, evidence2.hash.value, "Same hash should be returned");

                System.out.println("Successfully created and retrieved evidence by hash");
                System.out.println("Generated Evidence ID: " + generatedEvidenceId);
                System.out.println("User: " + evidence.userId);
                System.out.println("File: " + evidence.metadata.fileName);
                System.out.println("Size: " + evidence.metadata.size + " bytes");
                System.out.println("Hash: " + evidence.hash.algorithm);
        }

        @Test
        @DisplayName("Test getTotalEvidenceCount function")
        void testGetTotalEvidenceCount() throws Exception {
                // Get initial evidence count
                BigInteger initialCount = evidenceStorage.getTotalEvidenceCount().send();
                assertNotNull(initialCount, "Initial count should not be null");
                assertTrue(initialCount.compareTo(BigInteger.ZERO) >= 0,
                                "Initial count should be non-negative");

                System.out.println("Initial evidence count: " + initialCount);

                // Test multiple calls to ensure consistency
                BigInteger secondCount = evidenceStorage.getTotalEvidenceCount().send();
                assertEquals(initialCount, secondCount,
                                "Multiple calls should return the same count");

                // Create test evidence to verify count increases
                String testFileName = "count_test_file_" + System.currentTimeMillis() + ".txt";

                // Create random test hash to avoid hash conflicts in repeated tests
                byte[] testHash = new byte[32];
                Random random = new Random(System.currentTimeMillis() + 1); // Use different seed
                random.nextBytes(testHash);
                EvidenceStorageContract.HashInfo hashInfo = new EvidenceStorageContract.HashInfo("SHA-256", testHash);

                // Verify hash doesn't exist before creation
                Boolean hashExistsBefore = evidenceStorage.doesHashExist(testHash).send();
                assertFalse(hashExistsBefore, "Hash should not exist before creation");

                // Submit new evidence
                TransactionReceipt receipt = evidenceStorage.submitHashEvidence(
                                testFileName,
                                hashInfo,
                                "Test evidence for count verification").send();

                assertNotNull(receipt, "Transaction receipt should not be null");
                assertTrue(receipt.isStatusOK(), "Transaction should be successful");

                // Verify count increased by 1
                BigInteger newCount = evidenceStorage.getTotalEvidenceCount().send();
                assertEquals(initialCount.add(BigInteger.ONE), newCount,
                                "Count should increase by 1 after creating evidence");

                // Verify the hash was actually created
                Boolean hashExistsAfter = evidenceStorage.doesHashExist(testHash).send();
                assertTrue(hashExistsAfter, "Hash should exist after creation");

                // Get the generated evidenceId from the event
                List<EvidenceStorageContract.EvidenceSubmittedEventResponse> events = EvidenceStorageContract
                                .getEvidenceSubmittedEvents(receipt);
                assertEquals(1, events.size(), "Should have one EvidenceSubmitted event");

                EvidenceStorageContract.EvidenceSubmittedEventResponse event = events.get(0);
                String generatedEvidenceId = event.evidenceId;

                // Retrieve and verify the created evidence by hash
                EvidenceStorageContract.Evidence evidence = evidenceStorage.getEvidenceByHash(testHash).send();
                assertNotNull(evidence, "Created evidence should be retrievable by hash");
                assertEquals(generatedEvidenceId, evidence.evidenceId, "Evidence ID should match");
                assertTrue(evidence.exists, "Evidence should exist");

                // Also verify we can get it by the generated ID
                EvidenceStorageContract.Evidence evidenceById = evidenceStorage.getEvidence(generatedEvidenceId).send();
                assertNotNull(evidenceById, "Created evidence should be retrievable by ID");
                assertEquals(generatedEvidenceId, evidenceById.evidenceId, "Evidence ID should match");

                System.out.println("Evidence count before creation: " + initialCount);
                System.out.println("Evidence count after creation: " + newCount);
                System.out.println("Successfully verified count increase for evidence: " + generatedEvidenceId);

                // Test that count remains consistent
                BigInteger finalCount = evidenceStorage.getTotalEvidenceCount().send();
                assertEquals(newCount, finalCount, "Count should remain consistent");

                System.out.println("Verified count consistency: " + finalCount);
        }
}
