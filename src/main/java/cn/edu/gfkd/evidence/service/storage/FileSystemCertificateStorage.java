package cn.edu.gfkd.evidence.service.storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.stereotype.Component;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于文件系统的证书存储实现
 */
@Component @Slf4j
public class FileSystemCertificateStorage implements CertificateStorage {

    private final String basePath;
    private final boolean createDirectories;
    private final String urlBasePath;

    public FileSystemCertificateStorage(CertificateStorageConfig config) {
        this.basePath = config.getFilesystem().getBasePath();
        this.createDirectories = config.getFilesystem().isCreateDirectories();
        this.urlBasePath = config.getFilesystem().getUrlBasePath();
    }

    @Override
    public String storeCertificate(EvidenceEntity evidence, InputStream certificateData)
            throws IOException {
        log.debug("Storing certificate for evidenceId: {}", evidence.getEvidenceId());

        // 确保基础目录存在
        ensureBaseDirectoryExists();

        // 生成证书ID
        String certificateId = generateCertificateId(evidence);
        Path filePath = Paths.get(basePath, certificateId + ".pdf");

        // 创建父目录（如果需要）
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // 存储文件
        Files.copy(certificateData, filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Certificate stored successfully: {} -> {}", certificateId, filePath);
        return certificateId;
    }

    @Override
    public Optional<InputStream> getCertificate(String certificateId) throws IOException {
        if (certificateId == null || certificateId.isEmpty()) {
            return Optional.empty();
        }

        Path filePath = Paths.get(basePath, certificateId + ".pdf");
        if (!Files.exists(filePath)) {
            log.debug("Certificate file not found: {}", certificateId);
            return Optional.empty();
        }

        InputStream inputStream = new FileInputStream(filePath.toFile());
        return Optional.of(inputStream);
    }

    @Override
    public boolean deleteCertificate(String certificateId) throws IOException {
        if (certificateId == null || certificateId.isEmpty()) {
            return false;
        }

        Path filePath = Paths.get(basePath, certificateId + ".pdf");
        if (!Files.exists(filePath)) {
            log.debug("Certificate file not found for deletion: {}", certificateId);
            return false;
        }

        try {
            Files.delete(filePath);
            log.info("Certificate file deleted successfully: {}", certificateId);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete certificate file {}: {}", certificateId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean certificateExists(String certificateId) {
        if (certificateId == null || certificateId.isEmpty()) {
            return false;
        }

        Path filePath = Paths.get(basePath, certificateId + ".pdf");
        return Files.exists(filePath);
    }

    @Override
    public long getCertificateSize(String certificateId) throws IOException {
        if (certificateId == null || certificateId.isEmpty()) {
            return 0;
        }

        Path filePath = Paths.get(basePath, certificateId + ".pdf");
        if (!Files.exists(filePath)) {
            return 0;
        }

        return Files.size(filePath);
    }

    @Override
    public Optional<String> getPublicUrl(String certificateId) {
        if (certificateId == null || certificateId.isEmpty()) {
            return Optional.empty();
        }

        try {
            String publicUrl = urlBasePath + "/" + certificateId + ".pdf";
            return Optional.of(publicUrl);
        } catch (Exception e) {
            log.warn("Failed to generate public URL for {}: {}", certificateId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getStorageType() {
        return "filesystem";
    }

    @Override
    public void initialize() throws IOException {
        log.info("Initializing FileSystemCertificateStorage with base path: {}", basePath);
        ensureBaseDirectoryExists();
    }

    private void ensureBaseDirectoryExists() throws IOException {
        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            if (createDirectories) {
                Files.createDirectories(baseDir);
                log.info("Created base directory: {}", basePath);
            } else {
                throw new IOException("Base directory does not exist: " + basePath);
            }
        }
    }

    private String generateCertificateId(EvidenceEntity evidence) {
        // 生成基于证据ID的证书ID
        String evidenceId = evidence.getEvidenceId();
        // 清理特殊字符
        String safeEvidenceId = evidenceId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "cert_" + safeEvidenceId;
    }

    /**
     * 获取证书存储的基础路径
     * 
     * @return 基础路径
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * 清理过期的证书文件
     * 
     * @param daysToKeep 保留天数
     * @return 清理的文件数量
     * @throws IOException 清理失败时抛出异常
     */
    public int cleanupOldCertificates(int daysToKeep) throws IOException {
        log.info("Cleaning up certificates older than {} days", daysToKeep);

        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            return 0;
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        int cleanedCount = 0;

        try (var stream = Files.walk(baseDir)) {
            var files = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pdf")).filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            log.warn("Failed to get last modified time for {}: {}", path,
                                    e.getMessage());
                            return false;
                        }
                    }).toList();

            for (Path file : files) {
                try {
                    Files.delete(file);
                    cleanedCount++;
                    log.debug("Deleted old certificate: {}", file);
                } catch (IOException e) {
                    log.error("Failed to delete old certificate {}: {}", file, e.getMessage(), e);
                }
            }
        }

        log.info("Cleaned up {} old certificate files", cleanedCount);
        return cleanedCount;
    }
}