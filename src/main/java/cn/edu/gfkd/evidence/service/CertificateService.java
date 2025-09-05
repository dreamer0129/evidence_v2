package cn.edu.gfkd.evidence.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import cn.edu.gfkd.evidence.entity.EvidenceEntity;
import cn.edu.gfkd.evidence.exception.CertificateGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class CertificateService {

    private final CertificateConfig certificateConfig;

    public String generateCertificate(EvidenceEntity evidence)
            throws CertificateGenerationException {
        log.debug("Generating certificate for evidenceId: {}", evidence.getEvidenceId());

        try {
            // Ensure output directory exists
            String outputDirPath = ensureOutputDirectory();

            // Generate output filename
            String outputFileName = "certificate_" + evidence.getEvidenceId() + ".pdf";
            String outputPath = outputDirPath + "/" + outputFileName;

            // Load template PDF
            Resource templateResource = new ClassPathResource("static/proof_template.pdf");
            if (!templateResource.exists()) {
                throw new CertificateGenerationException(
                        "Certificate template not found: static/proof_template.pdf");
            }

            try (InputStream templateStream = templateResource.getInputStream();
                    RandomAccessRead readPdf = new RandomAccessReadBuffer(templateStream);
                    PDDocument document = Loader.loadPDF(readPdf)) {

                PDAcroForm pdfForm = document.getDocumentCatalog().getAcroForm();
                if (pdfForm != null) {
                    // Load font for Chinese support
                    PDFont font = loadFont(document);
                    PDResources resources = new PDResources();
                    resources.put(COSName.getPDFName("F1"), font);
                    pdfForm.setDefaultResources(resources);

                    // Fill form fields
                    fillFormFields(pdfForm, evidence);

                } else {
                    throw new CertificateGenerationException("PDF form not found in template");
                }

                // Save the filled PDF
                document.save(outputPath);

                log.info("Certificate generated successfully: {}", outputPath);
                return outputPath;

            }

        } catch (IOException e) {
            log.error("Failed to generate certificate for evidence {}: {}",
                    evidence.getEvidenceId(), e.getMessage(), e);
            throw new CertificateGenerationException(
                    "Failed to generate certificate: " + e.getMessage(), e);
        }
    }

    private String ensureOutputDirectory() throws IOException {
        String outputPath = certificateConfig.getOutputPath();
        Path path = Paths.get(outputPath);

        if (!Files.exists(path)) {
            if (certificateConfig.isAutoCreateDirectory()) {
                Files.createDirectories(path);
                log.info("Created certificate output directory: {}", outputPath);
            } else {
                throw new IOException("Output directory does not exist: " + outputPath);
            }
        }

        return outputPath;
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        // Try to load Chinese font for better character support
        try {
            Resource fontResource = new ClassPathResource("static/fonts/SourceHanSans-Regular.ttf");
            if (fontResource.exists()) {
                try (InputStream fontStream = fontResource.getInputStream();
                        RandomAccessRead readFont = new RandomAccessReadBuffer(fontStream)) {
                    return PDType0Font.load(document, readFont, false, false);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load Chinese font, using default font: {}", e.getMessage());
        }

        // Use standard Helvetica as last resort
        try {
            return PDType0Font.load(document,
                    new java.io.File("src/main/resources/static/fonts/simhei.ttf"));
        } catch (IOException e) {
            log.error("Failed to load any font: {}", e.getMessage());
            throw new IOException("Failed to load any font for PDF generation", e);
        }
    }

    private void fillFormFields(PDAcroForm pdfForm, EvidenceEntity evidence) throws IOException {
        Map<String, String> fieldContentMap = createFieldContentMap(evidence);

        for (PDField field : pdfForm.getFields()) {
            String fieldName = field.getFullyQualifiedName();
            if (fieldContentMap.containsKey(fieldName)) {
                if (field instanceof PDTextField) {
                    PDTextField textField = (PDTextField) field;
                    textField.setDefaultAppearance("/F1 12 Tf 0 g");
                    textField.setValue(fieldContentMap.get(fieldName));
                    textField.setReadOnly(true);
                } else if (field instanceof PDNonTerminalField) {
                    // Handle non-terminal fields (field groups)
                    log.debug("Skipping non-terminal field: {}", fieldName);
                } else {
                    log.warn("Unsupported field type: {} for field: {}",
                            field.getClass().getSimpleName(), fieldName);
                }
            }
        }
    }

    private Map<String, String> createFieldContentMap(EvidenceEntity evidence) {
        Map<String, String> fieldMap = new HashMap<>();

        // Map evidence entity fields to PDF form fields
        fieldMap.put("id", evidence.getEvidenceId());
        fieldMap.put("owner", evidence.getUserAddress());
        fieldMap.put("fileName", evidence.getFileName());
        fieldMap.put("fileType", evidence.getMimeType() != null ? evidence.getMimeType() : "");
        fieldMap.put("fileSize", formatFileSize(evidence.getFileSize()));
        fieldMap.put("fileHash", evidence.getHashValue());
        fieldMap.put("txHash", evidence.getTransactionHash());
        fieldMap.put("blockHeight", evidence.getBlockNumber().toString());
        fieldMap.put("timestamp", formatTimestamp(evidence.getBlockTimestamp()));
        fieldMap.put("memo", evidence.getMemo() != null ? evidence.getMemo() : "");

        // Generate dynamic content
        Date current = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fieldMap.put("date", sdf.format(current));

        SimpleDateFormat sdfChinese = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        String content = "\t" + sdfChinese.format(current)
                + "本处在证据链节点，接收到新增数据存证事件的同步信息，该存证事件包含的以下信息，均由智能数据科学系提供，存证信息经本处系统验证，与区块上的一致，特此证明。";
        fieldMap.put("content", content);

        return fieldMap;
    }

    private String formatFileSize(Long fileSize) {
        if (fileSize == null || fileSize == 0) {
            return "0 bytes";
        }

        String[] units = { "bytes", "KB", "MB", "GB" };
        int unitIndex = 0;
        double size = fileSize.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private String formatTimestamp(BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            return "";
        }

        try {
            long timestampLong = timestamp.longValue() * 1000; // Convert to milliseconds
            Date date = new Date(timestampLong);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        } catch (Exception e) {
            log.warn("Failed to format timestamp: {}", timestamp, e);
            return timestamp.toString();
        }
    }

    public boolean certificateExists(String certificatePath) {
        if (certificatePath == null || certificatePath.isEmpty()) {
            return false;
        }

        Path path = Paths.get(certificatePath);
        return Files.exists(path);
    }

    public byte[] getCertificateBytes(String certificatePath) throws IOException {
        if (certificatePath == null || certificatePath.isEmpty()) {
            throw new IOException("Certificate path is null or empty");
        }

        Path path = Paths.get(certificatePath);
        if (!Files.exists(path)) {
            throw new IOException("Certificate file not found: " + certificatePath);
        }

        return Files.readAllBytes(path);
    }

    public long getCertificateFileSize(String certificatePath) throws IOException {
        if (certificatePath == null || certificatePath.isEmpty()) {
            return 0;
        }

        Path path = Paths.get(certificatePath);
        if (!Files.exists(path)) {
            return 0;
        }

        return Files.size(path);
    }
}