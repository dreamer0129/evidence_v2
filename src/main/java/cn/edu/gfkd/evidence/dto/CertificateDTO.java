package cn.edu.gfkd.evidence.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDTO {
    
    private Long id;
    private String evidenceId;
    private String certificateId;
    private LocalDateTime generatedAt;
    private Long fileSize;
    private String status;
    
}