package cn.edu.gfkd.evidence.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix = "certificate.storage")
public class CertificateStorageConfig {

    private String type = "filesystem";
    private Filesystem filesystem = new Filesystem();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Filesystem getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(Filesystem filesystem) {
        this.filesystem = filesystem;
    }

    public static class Filesystem {
        private String basePath = "data/certificates";
        private boolean createDirectories = true;
        private String urlBasePath = "data/certificates";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public boolean isCreateDirectories() {
            return createDirectories;
        }

        public void setCreateDirectories(boolean createDirectories) {
            this.createDirectories = createDirectories;
        }

        public String getUrlBasePath() {
            return urlBasePath;
        }

        public void setUrlBasePath(String urlBasePath) {
            this.urlBasePath = urlBasePath;
        }
    }
}