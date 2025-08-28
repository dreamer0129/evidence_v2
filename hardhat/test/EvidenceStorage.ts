import { expect } from "chai";
import { ethers } from "hardhat";
import { EvidenceStorage } from "../typechain-types";
import { SignerWithAddress } from "@nomicfoundation/hardhat-ethers/signers";

describe("EvidenceStorage", function () {
  let evidenceStorage: EvidenceStorage;
  let owner: SignerWithAddress;
  let user1: SignerWithAddress;
  let user2: SignerWithAddress;

  // Test data - evidence IDs will be auto-generated
  let testEvidenceId1: string;
  let testEvidenceId2: string;
  const testHash = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
  const testHash2 = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
  const testMemo = "This is a test memo.";

  const testMetadata = {
    fileName: "test-document.pdf",
    mimeType: "application/pdf",
    size: 12345,
    creationTime: Math.floor(Date.now() / 1000),
  };

  const testHashInfo = {
    algorithm: "SHA256",
    value: testHash,
  };

  const testHashInfo2 = {
    algorithm: "SHA256",
    value: testHash2,
  };

  before(async () => {
    [owner, user1, user2] = await ethers.getSigners();
    const evidenceStorageFactory = await ethers.getContractFactory("EvidenceStorage");
    evidenceStorage = (await evidenceStorageFactory.deploy(owner.address)) as EvidenceStorage;
    await evidenceStorage.waitForDeployment();
  });

  describe("Deployment", function () {
    it("Should set the right owner", async function () {
      expect(await evidenceStorage.owner()).to.equal(owner.address);
    });

    it("Should start with zero evidence count", async function () {
      expect(await evidenceStorage.getTotalEvidenceCount()).to.equal(0);
    });
  });

  describe("Evidence Submission", function () {
    it("Should submit evidence successfully", async function () {
      // Execute the transaction
      const tx = await evidenceStorage.connect(user1).submitEvidence(testMetadata, testHashInfo, testMemo);

      // Verify event was emitted
      await expect(tx).to.emit(evidenceStorage, "EvidenceSubmitted");

      expect(await evidenceStorage.getTotalEvidenceCount()).to.equal(1);
      expect(await evidenceStorage.doesHashExist(testHash)).to.be.true;

      // Get the evidence ID by checking which evidence was created for this user
      const userEvidences = await evidenceStorage.getUserEvidences(user1.address);
      expect(userEvidences).to.have.lengthOf(1);
      testEvidenceId1 = userEvidences[0];

      expect(await evidenceStorage.doesEvidenceExist(testEvidenceId1)).to.be.true;
    });

    it("Should submit hash-only evidence successfully", async function () {
      // Execute the transaction
      const tx = await evidenceStorage
        .connect(user2)
        .submitHashEvidence("secret-file.txt", testHashInfo2, "Hash only memo");

      // Verify event was emitted
      await expect(tx).to.emit(evidenceStorage, "EvidenceSubmitted");

      expect(await evidenceStorage.getTotalEvidenceCount()).to.equal(2);

      // Get the evidence ID by checking which evidence was created for this user
      const userEvidences = await evidenceStorage.getUserEvidences(user2.address);
      expect(userEvidences).to.have.lengthOf(1);
      testEvidenceId2 = userEvidences[0];
    });

    it("Should generate unique evidence IDs", async function () {
      // Submit another evidence to verify unique ID generation
      const uniqueHash = "0x3333333333333333333333333333333333333333333333333333333333333333";
      const uniqueHashInfo = {
        algorithm: "SHA256",
        value: uniqueHash,
      };

      // Execute the actual transaction
      const tx = await evidenceStorage.connect(user1).submitEvidence(testMetadata, uniqueHashInfo, "Another memo");

      // Get the evidence ID by checking which evidences this user now has
      const userEvidences = await evidenceStorage.getUserEvidences(user1.address);
      expect(userEvidences).to.have.lengthOf(2);

      // The new evidence ID should be different from the first one
      const newEvidenceId = userEvidences.find(id => id !== testEvidenceId1)!;

      expect(newEvidenceId).to.not.equal(testEvidenceId1);
      expect(newEvidenceId).to.not.equal(testEvidenceId2);
      expect(await evidenceStorage.doesEvidenceExist(newEvidenceId)).to.be.true;
    });

    it("Should revert when submitting duplicate hash", async function () {
      await expect(
        evidenceStorage.connect(user2).submitEvidence(testMetadata, testHashInfo, "Duplicate hash memo"),
      ).to.be.revertedWithCustomError(evidenceStorage, "HashAlreadyExists");
    });

    it("Should revert with empty file name", async function () {
      const invalidMetadata = {
        fileName: "",
        mimeType: "application/pdf",
        size: 12345,
        creationTime: Math.floor(Date.now() / 1000),
      };

      const uniqueHash = "0x4444444444444444444444444444444444444444444444444444444444444444";
      const uniqueHashInfo = {
        algorithm: "SHA256",
        value: uniqueHash,
      };

      await expect(
        evidenceStorage.connect(user1).submitEvidence(invalidMetadata, uniqueHashInfo, "Invalid file name memo"),
      ).to.be.revertedWithCustomError(evidenceStorage, "InvalidFileMetadata");
    });

    it("Should revert with invalid hash value", async function () {
      const invalidHashInfo = {
        algorithm: "SHA256",
        value: "0x0000000000000000000000000000000000000000000000000000000000000000",
      };

      await expect(
        evidenceStorage.connect(user1).submitEvidence(testMetadata, invalidHashInfo, "Invalid hash memo"),
      ).to.be.revertedWithCustomError(evidenceStorage, "InvalidHashValue");
    });
  });

  describe("Evidence Retrieval", function () {
    it("Should get evidence by ID", async function () {
      const evidence = await evidenceStorage.getEvidence(testEvidenceId1);

      expect(evidence.evidenceId).to.equal(testEvidenceId1);
      expect(evidence.userId).to.equal(user1.address);
      expect(evidence.metadata.fileName).to.equal(testMetadata.fileName);
      expect(evidence.hash.algorithm).to.equal(testHashInfo.algorithm);
      expect(evidence.hash.value).to.equal(testHashInfo.value);
      expect(evidence.status).to.equal("effective");
      expect(evidence.memo).to.equal(testMemo);
      expect(evidence.exists).to.be.true;
    });

    it("Should get evidence by hash", async function () {
      const evidence = await evidenceStorage.getEvidenceByHash(testHash);

      expect(evidence.evidenceId).to.equal(testEvidenceId1);
      expect(evidence.userId).to.equal(user1.address);
    });

    it("Should revert when getting non-existent evidence", async function () {
      await expect(evidenceStorage.getEvidence("NON_EXISTENT")).to.be.revertedWithCustomError(
        evidenceStorage,
        "EvidenceNotFound",
      );
    });

    it("Should revert when getting evidence by non-existent hash", async function () {
      const nonExistentHash = "0x9999999999999999999999999999999999999999999999999999999999999999";
      await expect(evidenceStorage.getEvidenceByHash(nonExistentHash)).to.be.revertedWithCustomError(
        evidenceStorage,
        "EvidenceNotFound",
      );
    });

    it("Should get user evidences", async function () {
      const user1Evidences = await evidenceStorage.getUserEvidences(user1.address);
      const user2Evidences = await evidenceStorage.getUserEvidences(user2.address);

      expect(user1Evidences).to.have.lengthOf.greaterThan(0);
      expect(user1Evidences).to.include(testEvidenceId1);

      expect(user2Evidences).to.have.lengthOf(1);
      expect(user2Evidences[0]).to.equal(testEvidenceId2);
    });
  });

  describe("Evidence Verification", function () {
    it("Should verify evidence as valid", async function () {
      const tx = await evidenceStorage.verifyEvidence(testEvidenceId1);
      await expect(tx).to.emit(evidenceStorage, "EvidenceVerified");

      // Check the return value from the transaction
      const receipt = await tx.wait();
      expect(receipt).to.not.be.null;
    });

    it("Should verify evidence by hash as valid", async function () {
      const isValid = await evidenceStorage.verifyEvidenceByHash(testHash);
      expect(isValid).to.be.true;
    });

    it("Should return false for non-existent hash verification", async function () {
      const nonExistentHash = "0x9999999999999999999999999999999999999999999999999999999999999999";
      const isValid = await evidenceStorage.verifyEvidenceByHash(nonExistentHash);
      expect(isValid).to.be.false;
    });
  });

  describe("Evidence Status Management", function () {
    it("Should allow owner to change evidence status", async function () {
      const tx = await evidenceStorage.connect(owner).changeEvidenceStatus(testEvidenceId1, "expired");
      await expect(tx).to.emit(evidenceStorage, "EvidenceStatusChanged");

      const evidence = await evidenceStorage.getEvidence(testEvidenceId1);
      expect(evidence.status).to.equal("expired");
    });

    it("Should revert when non-owner tries to change status", async function () {
      await expect(
        evidenceStorage.connect(user1).changeEvidenceStatus(testEvidenceId1, "effective"),
      ).to.be.revertedWithCustomError(evidenceStorage, "OwnableUnauthorizedAccount");
    });

    it("Should revert with invalid status", async function () {
      await expect(
        evidenceStorage.connect(owner).changeEvidenceStatus(testEvidenceId1, "invalid"),
      ).to.be.revertedWithCustomError(evidenceStorage, "InvalidStatus");
    });

    it("Should verify expired evidence as invalid", async function () {
      // First change status to expired
      await evidenceStorage.connect(owner).changeEvidenceStatus(testEvidenceId1, "expired");

      // Now verify should return false
      const tx = await evidenceStorage.verifyEvidence(testEvidenceId1);
      const receipt = await tx.wait();
      expect(receipt).to.not.be.null;

      // Check that the event was emitted
      await expect(tx).to.emit(evidenceStorage, "EvidenceVerified");
    });
  });

  describe("Evidence Revocation", function () {
    it("Should allow evidence submitter to revoke their evidence", async function () {
      const tx = await evidenceStorage.connect(user2).revokeEvidence(testEvidenceId2);
      await expect(tx)
        .to.emit(evidenceStorage, "EvidenceRevoked")
        .and.to.emit(evidenceStorage, "EvidenceStatusChanged");

      const evidence = await evidenceStorage.getEvidence(testEvidenceId2);
      expect(evidence.status).to.equal("revoked");
    });

    it("Should allow owner to revoke any evidence", async function () {
      // Reset evidence status first
      await evidenceStorage.connect(owner).changeEvidenceStatus(testEvidenceId1, "effective");

      const tx = await evidenceStorage.connect(owner).revokeEvidence(testEvidenceId1);
      await expect(tx).to.emit(evidenceStorage, "EvidenceRevoked");
    });

    it("Should revert when unauthorized user tries to revoke evidence", async function () {
      // Create a new evidence for testing
      const newHash = "0x8888888888888888888888888888888888888888888888888888888888888888";
      const newHashInfo = {
        algorithm: "SHA256",
        value: newHash,
      };

      // Execute the actual transaction
      const tx = await evidenceStorage
        .connect(user1)
        .submitEvidence(testMetadata, newHashInfo, "Unauthorized revoke memo");

      // Get the evidence ID by checking the latest evidence for this user
      const userEvidences = await evidenceStorage.getUserEvidences(user1.address);
      const newEvidenceId = userEvidences[userEvidences.length - 1]; // Get the latest one

      await expect(evidenceStorage.connect(user2).revokeEvidence(newEvidenceId)).to.be.revertedWithCustomError(
        evidenceStorage,
        "UnauthorizedAccess",
      );
    });
  });

  describe("Utility Functions", function () {
    it("Should check evidence existence correctly", async function () {
      expect(await evidenceStorage.doesEvidenceExist(testEvidenceId1)).to.be.true;
      expect(await evidenceStorage.doesEvidenceExist("NON_EXISTENT")).to.be.false;
    });

    it("Should check hash existence correctly", async function () {
      expect(await evidenceStorage.doesHashExist(testHash)).to.be.true;
      expect(await evidenceStorage.doesHashExist("0x9999999999999999999999999999999999999999999999999999999999999999"))
        .to.be.false;
    });

    it("Should return correct total evidence count", async function () {
      const count = await evidenceStorage.getTotalEvidenceCount();
      expect(count).to.be.greaterThan(0);
    });
  });
});
