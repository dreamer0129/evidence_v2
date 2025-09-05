package cn.edu.gfkd.evidence.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "certificate")
@Data
public class CertificateConfig {
    
    private String templatePath = "classpath:static/proof_template.pdf";
    private String outputPath = "./certificates";
    private String fontPath = "classpath:static/fonts/simhei.ttf";
    private boolean autoCreateDirectory = true;
    
}