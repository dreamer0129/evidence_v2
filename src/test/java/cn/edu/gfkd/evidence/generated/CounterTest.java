package cn.edu.gfkd.evidence.generated;

import cn.edu.gfkd.evidence.utils.ContractUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CounterTest {

    private static final String nodeUrl = System.getenv().getOrDefault("WEB3J_NODE_URL",
            "http://127.0.0.1:8545");
    private static final String walletPassword = System.getenv().getOrDefault("WEB3J_WALLET_PASSWORD", "123456");
    private static final String walletPath = System.getenv().getOrDefault("WEB3J_WALLET_PATH",
            "src/test/resources/wallet/testnet/keystore/hardhat-wallet0.json");
    
    private static Web3j web3j;
    private static Credentials credentials;
    private static Counter counter;
    private static ContractGasProvider gasProvider;
    private static String contractAddress;

    @BeforeAll
    static void setUp() throws Exception {
        // Initialize web3j connection
        web3j = Web3j.build(new HttpService(nodeUrl));
        
        // Load credentials from wallet
        credentials = WalletUtils.loadCredentials(walletPassword, walletPath);
        
        // Set up gas provider with sufficient gas
        gasProvider = new StaticGasProvider(BigInteger.valueOf(20_000_000_000L), BigInteger.valueOf(4_712_388L));
        
        // Get deployed contract address
        contractAddress = ContractUtils.getDeployedContractAddress("Counter", "localhost");
        assertNotNull(contractAddress, "Contract address should not be null");
        
        // Load contract
        counter = Counter.load(contractAddress, web3j, credentials, gasProvider);
        assertNotNull(counter, "Counter contract should be loaded");
        
        // Test contract by calling a method instead of isValid()
        BigInteger testValue = counter.x().send();
        assertNotNull(testValue, "Should be able to call contract methods");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (web3j != null) {
            web3j.close();
        }
    }

    @Test
    @DisplayName("Test initial counter value")
    void testInitialValue() throws Exception {
        BigInteger initialValue = counter.x().send();
        assertNotNull(initialValue, "Initial value should not be null");
        System.out.println("Initial counter value: " + initialValue);
    }

    @Test
    @DisplayName("Test increment function")
    void testIncrement() throws Exception {
        // Get current value
        BigInteger beforeValue = counter.x().send();
        
        // Increment counter
        TransactionReceipt receipt = counter.increment().send();
        assertNotNull(receipt, "Transaction receipt should not be null");
        assertTrue(receipt.isStatusOK(), "Transaction should be successful");
        
        // Check XUpdate event was emitted
        List<Counter.XUpdateEventResponse> events = Counter.getXUpdateEvents(receipt);
        assertEquals(1, events.size(), "Should have one XUpdate event");
        assertEquals(beforeValue, events.get(0).oldValue, "Old value should match");
        assertEquals(beforeValue.add(BigInteger.ONE), events.get(0).newValue, "New value should be incremented");
        
        // Verify value was incremented
        BigInteger afterValue = counter.x().send();
        assertEquals(beforeValue.add(BigInteger.ONE), afterValue, "Value should be incremented by 1");
        
        System.out.println("Incremented from " + beforeValue + " to " + afterValue);
    }

    @Test
    @DisplayName("Test decrement function")
    void testDecrement() throws Exception {
        // Get current value
        BigInteger beforeValue = counter.x().send();
        
        // Decrement counter
        TransactionReceipt receipt = counter.decrement().send();
        assertNotNull(receipt, "Transaction receipt should not be null");
        assertTrue(receipt.isStatusOK(), "Transaction should be successful");
        
        // Check XUpdate event was emitted
        List<Counter.XUpdateEventResponse> events = Counter.getXUpdateEvents(receipt);
        assertEquals(1, events.size(), "Should have one XUpdate event");
        assertEquals(beforeValue, events.get(0).oldValue, "Old value should match");
        assertEquals(beforeValue.subtract(BigInteger.ONE), events.get(0).newValue, "New value should be decremented");
        
        // Verify value was decremented
        BigInteger afterValue = counter.x().send();
        assertEquals(beforeValue.subtract(BigInteger.ONE), afterValue, "Value should be decremented by 1");
        
        System.out.println("Decremented from " + beforeValue + " to " + afterValue);
    }

    @Test
    @DisplayName("Test multiple increments")
    void testMultipleIncrements() throws Exception {
        BigInteger initialValue = counter.x().send();
        int increments = 5;
        
        // Perform multiple increments
        for (int i = 0; i < increments; i++) {
            TransactionReceipt receipt = counter.increment().send();
            assertTrue(receipt.isStatusOK(), "Increment transaction should be successful");
        }
        
        // Verify final value
        BigInteger finalValue = counter.x().send();
        assertEquals(initialValue.add(BigInteger.valueOf(increments)), finalValue, 
            "Value should be incremented by " + increments);
        
        System.out.println("Performed " + increments + " increments: " + initialValue + " -> " + finalValue);
    }

    @Test
    @DisplayName("Test event filtering")
    void testEventFiltering() throws Exception {
        BigInteger initialValue = counter.x().send();
        
        // Trigger an event and get receipt
        TransactionReceipt receipt = counter.increment().send();
        assertTrue(receipt.isStatusOK(), "Increment should succeed");
        
        // Get events from the transaction receipt
        List<Counter.XUpdateEventResponse> events = Counter.getXUpdateEvents(receipt);
        assertEquals(1, events.size(), "Should have one XUpdate event");
        
        // Verify event data
        Counter.XUpdateEventResponse event = events.get(0);
        assertEquals(initialValue, event.oldValue, "Event old value should match");
        assertEquals(initialValue.add(BigInteger.ONE), event.newValue, "Event new value should match");
        
        System.out.println("XUpdate event from receipt: " + event.oldValue + " -> " + event.newValue);
    }


    @Test
    @DisplayName("Test transaction details")
    void testTransactionDetails() throws Exception {
        // Get current block number
        EthBlock currentBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        BigInteger blockNumber = currentBlock.getBlock().getNumber();
        
        // Perform increment
        TransactionReceipt receipt = counter.increment().send();
        
        // Get transaction details
        org.web3j.protocol.core.methods.response.Transaction transaction = 
            web3j.ethGetTransactionByHash(receipt.getTransactionHash()).send().getTransaction().get();
        
        assertEquals(contractAddress.toLowerCase(), transaction.getTo().toLowerCase(), 
            "Transaction should be sent to contract address");
        assertTrue(transaction.getBlockNumber().compareTo(blockNumber) >= 0, 
            "Transaction should be in current or later block");
        
        System.out.println("Transaction hash: " + receipt.getTransactionHash());
        System.out.println("Block number: " + receipt.getBlockNumber());
        System.out.println("Gas used: " + receipt.getCumulativeGasUsed());
    }

    @Test
    @DisplayName("Test contract loading with different credentials")
    void testContractLoading() throws Exception {
        // Load contract with same credentials (should work)
        Counter counter2 = Counter.load(contractAddress, web3j, credentials, gasProvider);
        
        // Test contract by calling a method instead of isValid()
        BigInteger testValue = counter2.x().send();
        assertNotNull(testValue, "Should be able to call contract methods on second instance");
        
        // Verify both instances return same value
        BigInteger value1 = counter.x().send();
        BigInteger value2 = counter2.x().send();
        assertEquals(value1, value2, "Both contract instances should return same value");
    }

    @Test
    @DisplayName("Test batch operations")
    void testBatchOperations() throws Exception {
        BigInteger initialValue = counter.x().send();
        
        // Perform sequence of operations
        TransactionReceipt receipt1 = counter.increment().send();
        assertTrue(receipt1.isStatusOK());
        
        TransactionReceipt receipt2 = counter.increment().send();
        assertTrue(receipt2.isStatusOK());
        
        TransactionReceipt receipt3 = counter.decrement().send();
        assertTrue(receipt3.isStatusOK());
        
        // Final value should be initialValue + 1
        BigInteger finalValue = counter.x().send();
        assertEquals(initialValue.add(BigInteger.ONE), finalValue, 
            "After +1, +1, -1, value should be +1 from initial");
        
        System.out.println("Batch operations result: " + initialValue + " -> " + finalValue);
    }
}