package cn.edu.gfkd.evidence.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;

import cn.edu.gfkd.evidence.generated.EvidenceStorageContract;
import cn.edu.gfkd.evidence.utils.ContractUtils;
import org.web3j.tx.gas.ContractGasProvider;

@Configuration
public class ContractConfig {

    @Value("${blockchain.network:localhost}")
    private String network;

    @Autowired
    private Credentials credentials;

    @Autowired
    private ContractGasProvider contractGasProvider;

    @Bean
    public EvidenceStorageContract evidenceStorageContract() {

        String contractAddress;
        try {
            contractAddress = ContractUtils.getDeployedContractAddress("EvidenceStorageContract", network);
            return EvidenceStorageContract.load(contractAddress,
                    org.web3j.protocol.Web3j.build(new org.web3j.protocol.http.HttpService()),
                    credentials,
                    contractGasProvider);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
