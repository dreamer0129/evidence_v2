import { HardhatRuntimeEnvironment } from "hardhat/types";
import { DeployFunction } from "hardhat-deploy/types";
import { Contract } from "ethers";

/**
 * Deploys a contract named "EvidenceStorage" using the deployer account and
 * constructor arguments set to the deployer address
 *
 * @param hre HardhatRuntimeEnvironment object.
 */
const deployEvidenceStorage: DeployFunction = async function (hre: HardhatRuntimeEnvironment) {
  /*
    On localhost, the deployer account is the one that comes with Hardhat, which is already funded.

    When deploying to live networks (e.g `yarn deploy --network sepolia`), the deployer account
    should have sufficient balance to pay for the gas fees for contract creation.

    You can generate a random account with `yarn generate` or `yarn account:import` to import your
    existing PK which will fill DEPLOYER_PRIVATE_KEY_ENCRYPTED in the .env file (then used on hardhat.config.ts)
    You can run the `yarn account` command to check your balance in every network.
  */
  const { deployer } = await hre.getNamedAccounts();
  const { deploy } = hre.deployments;

  await deploy("EvidenceStorage", {
    from: deployer,
    // Contract constructor arguments - owner address
    args: [deployer],
    log: true,
    // autoMine: can be passed to the deploy function to make the deployment process faster on local networks by
    // automatically mining the contract deployment transaction. There is no effect on live networks.
    autoMine: true,
  });

  // Get the deployed contract to interact with it after deploying.
  const evidenceStorage = await hre.ethers.getContract<Contract>("EvidenceStorage", deployer);
  console.log("📋 EvidenceStorage deployed!");
  console.log("🔧 Owner address:", await evidenceStorage.owner());
  console.log("📊 Total evidence count:", await evidenceStorage.getTotalEvidenceCount());

  // Optional: Submit a test evidence for verification
  console.log("🧪 Submitting test evidence...");
  try {
    const testHash = {
      algorithm: "SHA256",
      value: "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
    };
    const testMetadata = {
      fileName: "test-document.pdf",
      mimeType: "application/pdf",
      size: 12345,
      creationTime: Math.floor(Date.now() / 1000),
    };

    const tx = await evidenceStorage.submitEvidence(testMetadata, testHash);
    const receipt = await tx.wait();

    // Get the generated evidence ID from events
    const evidenceSubmittedEvent = evidenceStorage.interface.getEvent("EvidenceSubmitted");
    const event = receipt.logs.find(
      (log: any) =>
        evidenceSubmittedEvent && log.topics[0] === evidenceStorage.interface.getEvent("EvidenceSubmitted")!.topicHash,
    );
    if (event && evidenceSubmittedEvent) {
      const decodedEvent = evidenceStorage.interface.parseLog(event);
      if (decodedEvent && decodedEvent.args) {
        console.log("✅ Test evidence submitted successfully!");
        console.log("🆔 Generated evidence ID:", decodedEvent.args.evidenceId);
        console.log("📊 Total evidence count after test:", await evidenceStorage.getTotalEvidenceCount());
      } else {
        console.log("⚠️  Could not decode event or evidenceId not found.");
      }
    }
  } catch (error) {
    console.log("⚠️  Test evidence submission skipped (may already exist)");
  }
};

export default deployEvidenceStorage;

// Tags are useful if you have multiple deploy files and only want to run one of them.
// e.g. yarn deploy --tags EvidenceStorage
deployEvidenceStorage.tags = ["EvidenceStorage"];
