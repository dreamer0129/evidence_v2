package cn.edu.gfkd.evidence.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 合约工具类，提供读取已部署合约地址等功能
 */
public class ContractUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEPLOYMENTS_DIR = "hardhat/deployments";

    /**
     * 获取已部署合约的地址
     * 
     * @param contractName 智能合约名称
     * @param networkName  网络名称
     * @return 合约地址
     * @throws IOException              当文件读取失败时抛出
     * @throws IllegalArgumentException 当合约或网络不存在时抛出
     */
    public static String getDeployedContractAddress(String contractName, String networkName) throws IOException {
        // 验证参数
        if (contractName == null || contractName.trim().isEmpty()) {
            throw new IllegalArgumentException("Contract name cannot be null or empty");
        }
        if (networkName == null || networkName.trim().isEmpty()) {
            throw new IllegalArgumentException("Network name cannot be null or empty");
        }

        // 构建文件路径
        Path filePath = Paths.get(DEPLOYMENTS_DIR, networkName, contractName + ".json");
        File contractFile = filePath.toFile();

        // 检查文件是否存在
        if (!contractFile.exists() || !contractFile.isFile()) {
            throw new IllegalArgumentException(String.format("Contract file not found: %s", filePath));
        }

        // 读取并解析JSON
        JsonNode rootNode = objectMapper.readTree(contractFile);
        JsonNode addressNode = rootNode.get("address");

        // 检查address字段是否存在
        if (addressNode == null || addressNode.isMissingNode()) {
            throw new IllegalArgumentException(String.format("Address field not found in contract file: %s", filePath));
        }

        return addressNode.asText();
    }
}