package cn.edu.gfkd.evidence.service.storage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cn.edu.gfkd.evidence.config.CertificateStorageConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component @RequiredArgsConstructor @Slf4j
public class CertificateStorageFactory {

    private final CertificateStorageConfig config;
    private final List<CertificateStorage> storageImplementations;

    private Map<String, CertificateStorage> storageMap;
    private CertificateStorage defaultStorage;

    @PostConstruct
    public void init() {
        // Create a map of storage type to implementation
        storageMap = storageImplementations.stream()
                .collect(Collectors.toMap(CertificateStorage::getStorageType, Function.identity()));

        // Set the default storage based on configuration
        String storageType = config.getType();
        defaultStorage = storageMap.get(storageType);

        if (defaultStorage == null) {
            log.warn("Configured storage type '{}' not found. Available types: {}", storageType,
                    storageMap.keySet());
            // Fallback to the first available storage
            if (!storageMap.isEmpty()) {
                defaultStorage = storageImplementations.get(0);
                log.info("Using fallback storage type: {}", defaultStorage.getStorageType());
            } else {
                throw new IllegalStateException("No certificate storage implementations available");
            }
        } else {
            log.info("Using certificate storage type: {}", storageType);
        }
    }

    public CertificateStorage getStorage() {
        return defaultStorage;
    }

    public CertificateStorage getStorage(String type) {
        CertificateStorage storage = storageMap.get(type);
        if (storage == null) {
            throw new IllegalArgumentException(
                    "Unknown storage type: " + type + ". Available types: " + storageMap.keySet());
        }
        return storage;
    }

    public Map<String, CertificateStorage> getAvailableStorages() {
        return storageMap;
    }
}