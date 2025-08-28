//SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

// Use openzeppelin to inherit battle-tested implementations
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * A smart contract for blockchain-based evidence storage and verification
 * Supports file hash storage, metadata management, and evidence verification
 * @author Evidence Storage Team
 */
contract EvidenceStorage is Ownable, ReentrancyGuard {
    // Counter for evidence numbering
    uint256 private _evidenceCounter;

    // Structs for organizing evidence data
    struct FileMetadata {
        string fileName;
        string mimeType;
        uint256 size;
        uint256 creationTime;
    }

    struct HashInfo {
        string algorithm; // "SHA256", "MD5", etc.
        bytes32 value; // Hash value
    }

    struct Evidence {
        string evidenceId; // Format: EVID:YYYYMMDD:CN-XXX
        address userId; // User address who submitted evidence
        FileMetadata metadata; // File metadata
        HashInfo hash; // Hash information
        uint256 timestamp; // Evidence submission timestamp
        uint256 blockHeight; // Block height when submitted
        string status; // "effective", "expired", "revoked"
        string memo; // Additional user-provided memo
        bool exists; // Check if evidence exists
    }

    // State variables
    mapping(string => Evidence) private evidences; // evidenceId => Evidence
    mapping(bytes32 => string) private hashToEvidenceId; // hash => evidenceId
    mapping(address => string[]) private userEvidences; // user => evidenceId[]
    mapping(string => bool) private evidenceExists; // evidenceId => exists

    // Events
    event EvidenceSubmitted(
        string indexed evidenceId,
        address indexed user,
        bytes32 indexed hashValue,
        uint256 timestamp
    );

    event EvidenceVerified(string indexed evidenceId, bool isValid, uint256 timestamp);

    event EvidenceRevoked(string indexed evidenceId, address indexed revoker, uint256 timestamp);

    event EvidenceStatusChanged(string indexed evidenceId, string oldStatus, string newStatus, uint256 timestamp);

    // Custom errors
    error EvidenceAlreadyExists(string evidenceId);
    error EvidenceNotFound(string evidenceId);
    error HashAlreadyExists(bytes32 hashValue);
    error InvalidEvidenceId(string evidenceId);
    error InvalidHashValue();
    error InvalidStatus(string status);
    error UnauthorizedAccess(address caller);
    error InvalidFileMetadata();

    // Modifiers
    modifier evidenceExistsCheck(string memory evidenceId) {
        if (!evidenceExists[evidenceId]) {
            revert EvidenceNotFound(evidenceId);
        }
        _;
    }

    modifier validEvidenceId(string memory evidenceId) {
        if (bytes(evidenceId).length == 0) {
            revert InvalidEvidenceId(evidenceId);
        }
        _;
    }

    modifier validHash(bytes32 hashValue) {
        if (hashValue == bytes32(0)) {
            revert InvalidHashValue();
        }
        _;
    }

    modifier validStatus(string memory status) {
        if (
            !_compareStrings(status, "effective") &&
            !_compareStrings(status, "expired") &&
            !_compareStrings(status, "revoked")
        ) {
            revert InvalidStatus(status);
        }
        _;
    }

    constructor(address _owner) Ownable(_owner) {
        _evidenceCounter = 1; // Start from 1
    }

    /**
     * Submit evidence with complete file metadata
     * @param metadata File metadata including name, type, size, creation time
     * @param hash Hash information including algorithm and value
     * @param memo Additional user-provided memo
     * @return evidenceId The generated evidence identifier
     */
    function submitEvidence(
        FileMetadata memory metadata,
        HashInfo memory hash,
        string memory memo
    ) external nonReentrant validHash(hash.value) returns (string memory evidenceId) {
        // Check if hash already exists
        if (bytes(hashToEvidenceId[hash.value]).length > 0) {
            revert HashAlreadyExists(hash.value);
        }

        // Validate file metadata
        if (bytes(metadata.fileName).length == 0) {
            revert InvalidFileMetadata();
        }

        // Generate evidence ID
        evidenceId = string(abi.encodePacked("EVID:", _toString(block.timestamp), ":CN-", _toString(_evidenceCounter)));

        // Create evidence record
        Evidence memory newEvidence = Evidence({
            evidenceId: evidenceId,
            userId: msg.sender,
            metadata: metadata,
            hash: hash,
            timestamp: block.timestamp,
            blockHeight: block.number,
            status: "effective",
            memo: memo,
            exists: true
        });

        // Store evidence
        evidences[evidenceId] = newEvidence;
        evidenceExists[evidenceId] = true;
        hashToEvidenceId[hash.value] = evidenceId;
        userEvidences[msg.sender].push(evidenceId);

        // Increment counter
        _evidenceCounter++;

        // Emit event
        emit EvidenceSubmitted(evidenceId, msg.sender, hash.value, block.timestamp);
    }

    /**
     * Submit evidence with only hash information (for privacy protection)
     * @param fileName Basic file name
     * @param hash Hash information
     * @param memo Additional user-provided memo
     * @return evidenceId The generated evidence identifier
     */
    function submitHashEvidence(
        string memory fileName,
        HashInfo memory hash,
        string memory memo
    ) external nonReentrant validHash(hash.value) returns (string memory evidenceId) {
        // Check if hash already exists
        if (bytes(hashToEvidenceId[hash.value]).length > 0) {
            revert HashAlreadyExists(hash.value);
        }

        // Generate evidence ID
        evidenceId = string(abi.encodePacked("EVID:", _toString(block.timestamp), ":CN-", _toString(_evidenceCounter)));

        // Create minimal file metadata
        FileMetadata memory metadata = FileMetadata({
            fileName: fileName,
            mimeType: "",
            size: 0,
            creationTime: block.timestamp
        });

        // Create evidence record
        Evidence memory newEvidence = Evidence({
            evidenceId: evidenceId,
            userId: msg.sender,
            metadata: metadata,
            hash: hash,
            timestamp: block.timestamp,
            blockHeight: block.number,
            status: "effective",
            memo: memo,
            exists: true
        });

        // Store evidence
        evidences[evidenceId] = newEvidence;
        evidenceExists[evidenceId] = true;
        hashToEvidenceId[hash.value] = evidenceId;
        userEvidences[msg.sender].push(evidenceId);

        // Increment counter
        _evidenceCounter++;

        // Emit event
        emit EvidenceSubmitted(evidenceId, msg.sender, hash.value, block.timestamp);
    }

    /**
     * Get evidence by evidence ID
     * @param evidenceId Evidence identifier
     * @return Evidence data
     */
    function getEvidence(
        string memory evidenceId
    ) external view evidenceExistsCheck(evidenceId) returns (Evidence memory) {
        return evidences[evidenceId];
    }

    /**
     * Get evidence by hash value
     * @param hashValue Hash value to search
     * @return Evidence data
     */
    function getEvidenceByHash(bytes32 hashValue) external view returns (Evidence memory) {
        string memory evidenceId = hashToEvidenceId[hashValue];
        if (bytes(evidenceId).length == 0) {
            revert EvidenceNotFound("Hash not found");
        }
        return evidences[evidenceId];
    }

    /**
     * Verify evidence validity
     * @param evidenceId Evidence identifier
     * @return isValid Whether evidence is valid and effective
     */
    function verifyEvidence(string memory evidenceId) external evidenceExistsCheck(evidenceId) returns (bool isValid) {
        Evidence memory evidence = evidences[evidenceId];
        isValid = _compareStrings(evidence.status, "effective");

        emit EvidenceVerified(evidenceId, isValid, block.timestamp);
        return isValid;
    }

    /**
     * Verify evidence by hash value
     * @param hashValue Hash value to verify
     * @return isValid Whether evidence is valid and effective
     */
    function verifyEvidenceByHash(bytes32 hashValue) external view returns (bool isValid) {
        string memory evidenceId = hashToEvidenceId[hashValue];
        if (bytes(evidenceId).length == 0) {
            return false;
        }

        Evidence memory evidence = evidences[evidenceId];
        isValid = _compareStrings(evidence.status, "effective");
        return isValid;
    }

    /**
     * Revoke evidence (only by owner or evidence submitter)
     * @param evidenceId Evidence identifier
     */
    function revokeEvidence(string memory evidenceId) external evidenceExistsCheck(evidenceId) {
        Evidence storage evidence = evidences[evidenceId];

        // Check authorization: only owner or evidence submitter can revoke
        if (msg.sender != owner() && msg.sender != evidence.userId) {
            revert UnauthorizedAccess(msg.sender);
        }

        string memory oldStatus = evidence.status;
        evidence.status = "revoked";

        emit EvidenceRevoked(evidenceId, msg.sender, block.timestamp);
        emit EvidenceStatusChanged(evidenceId, oldStatus, "revoked", block.timestamp);
    }

    /**
     * Change evidence status (only by owner)
     * @param evidenceId Evidence identifier
     * @param newStatus New status ("effective", "expired", "revoked")
     */
    function changeEvidenceStatus(
        string memory evidenceId,
        string memory newStatus
    ) external onlyOwner evidenceExistsCheck(evidenceId) validStatus(newStatus) {
        Evidence storage evidence = evidences[evidenceId];
        string memory oldStatus = evidence.status;
        evidence.status = newStatus;

        emit EvidenceStatusChanged(evidenceId, oldStatus, newStatus, block.timestamp);
    }

    /**
     * Get all evidence IDs for a specific user
     * @param user User address
     * @return Array of evidence IDs
     */
    function getUserEvidences(address user) external view returns (string[] memory) {
        return userEvidences[user];
    }

    /**
     * Get total number of evidences
     * @return Total count
     */
    function getTotalEvidenceCount() external view returns (uint256) {
        return _evidenceCounter - 1; // Subtract 1 because counter starts from 1
    }

    /**
     * Check if evidence exists
     * @param evidenceId Evidence identifier
     * @return exists Whether evidence exists
     */
    function doesEvidenceExist(string memory evidenceId) external view returns (bool exists) {
        return evidenceExists[evidenceId];
    }

    /**
     * Check if hash exists in the system
     * @param hashValue Hash value to check
     * @return exists Whether hash exists
     */
    function doesHashExist(bytes32 hashValue) external view returns (bool exists) {
        return bytes(hashToEvidenceId[hashValue]).length > 0;
    }

    // Internal helper functions
    function _compareStrings(string memory a, string memory b) internal pure returns (bool) {
        return keccak256(abi.encodePacked(a)) == keccak256(abi.encodePacked(b));
    }

    function _toString(uint256 value) internal pure returns (string memory) {
        if (value == 0) {
            return "0";
        }
        uint256 temp = value;
        uint256 digits;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }
        bytes memory buffer = new bytes(digits);
        while (value != 0) {
            digits -= 1;
            buffer[digits] = bytes1(uint8(48 + uint256(value % 10)));
            value /= 10;
        }
        return string(buffer);
    }
}
