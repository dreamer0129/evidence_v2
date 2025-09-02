package cn.edu.gfkd.evidence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;

@Configuration
public class BlockchainConfig {

    @Value("${blockchain.credentials.private-key:}")
    private String privateKey;

    @Bean
    public Credentials blockchainCredentials() {
        if (privateKey == null || privateKey.trim().isEmpty()) {
            // Use a default private key for development/testing purposes: 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
            return Credentials.create("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
        }
        return Credentials.create(privateKey);
    }
}