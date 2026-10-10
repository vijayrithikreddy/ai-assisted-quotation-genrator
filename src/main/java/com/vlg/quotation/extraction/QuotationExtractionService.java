package com.vlg.quotation.extraction;

import com.vlg.quotation.configuration.FileUploadProperties;
import com.vlg.quotation.configuration.LlmProperties;
import com.vlg.quotation.extraction.QuotationExtractionProvider.ProviderCall;
import com.vlg.quotation.extraction.QuotationExtractionProvider.SourceFile;
import com.vlg.quotation.extraction.raw.RawExtractionResponse;
import com.vlg.quotation.extraction.schema.QuotationExtractionResponse;
import com.vlg.quotation.infrastructure.files.SourceFileTypeDetector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates extraction: validates the mode and files, stages temporary
 * copies with generated names, calls the configured provider, translates and
 * validates the draft, and always cleans up temp files.
 */
@Service
public class QuotationExtractionService {

    private static final Logger log = LoggerFactory.getLogger(QuotationExtractionService.class);

    private static final String IMAGE_JPEG = SourceFileTypeDetector.IMAGE_JPEG;
    private static final String IMAGE_PNG = SourceFileTypeDetector.IMAGE_PNG;
    private static final String IMAGE_WEBP = SourceFileTypeDetector.IMAGE_WEBP;
    private static final String APPLICATION_PDF = SourceFileTypeDetector.APPLICATION_PDF;
    private static final String MSWORD = "application/msword";
    private static final String WORD_OPEN_XML =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** Uploaded image with its content-sniffed type. */
    record ValidatedImage(byte[] content, String contentType) {
    }

    /** Uploaded supporting document with its resolved type; null when absent. */
    record ValidatedDocument(byte[] content, String contentType) {
    }

    private final LlmProperties llmProperties;
    private final FileUploadProperties fileUploadProperties;
    private final QuotationExtractionProviderFactory providerFactory;
    private final PromptTemplateService promptTemplateService;
    private final ExtractionResponseParser responseParser;
    private final RawExtractionTranslator responseTranslator;
    private final ExtractionSchemaValidator schemaValidator;

    public QuotationExtractionService(
            LlmProperties llmProperties,
            FileUploadProperties fileUploadProperties,
            QuotationExtractionProviderFactory providerFactory,
            PromptTemplateService promptTemplateService,
            ExtractionResponseParser responseParser,
            RawExtractionTranslator responseTranslator,
            ExtractionSchemaValidator schemaValidator) {
        this.llmProperties = llmProperties;
        this.fileUploadProperties = fileUploadProperties;
        this.providerFactory = providerFactory;
        this.promptTemplateService = promptTemplateService;
        this.responseParser = responseParser;
        this.responseTranslator = responseTranslator;
        this.schemaValidator = schemaValidator;
    }

    /** Runs one extraction and returns the review-ready draft. */
    public ExtractionOutcome extract(
            MultipartFile quotationImage,
            String customerModeValue,
            MultipartFile supportingDocument,
            String customerId,
            String customerContext) {
        CustomerMode customerMode = CustomerMode.fromRequestValue(customerModeValue);
        if (customerMode == CustomerMode.EXISTING && !StringUtils.hasText(customerId)) {
            throw new IllegalArgumentException("customerId is required for existing customers.");
        }
        ValidatedImage validatedImage = readImage(quotationImage);
        ValidatedDocument validatedDocument = readSupportingDocument(supportingDocument);

        if (!llmProperties.isConfigured()) {
            throw new LlmUnavailableException(
                    "AI extraction is not configured. Please enter the quotation details manually.");
        }

        String extractionId = "ext-" + UUID.randomUUID();
        long callStartMillis = System.currentTimeMillis();
        Path stagingDirectory = stageTempFiles(validatedImage, validatedDocument);
        try {
            assertImageDecodable(stagingDirectory, validatedImage.contentType());
            String renderedPrompt = promptTemplateService.renderPrompt(
                    llmProperties.getExtractionPromptVersion(),
                    customerMode.providerWorkflowName(),
                    buildCustomerContext(customerMode, customerId, customerContext));
            QuotationExtractionProvider extractionProvider = providerFactory.select(llmProperties.getProvider());
            String draftPayload = extractionProvider.fetchDraftPayload(new ProviderCall(
                    renderedPrompt,
                    new SourceFile(validatedImage.contentType(), validatedImage.content()),
                    validatedDocument == null
                            ? null
                            : new SourceFile(validatedDocument.contentType(), validatedDocument.content())));
            RawExtractionResponse rawResponse = responseParser.parse(draftPayload);
            RawExtractionTranslator.TranslatedDraft translatedDraft =
                    responseTranslator.translate(rawResponse, customerMode);
            schemaValidator.validateDraft(translatedDraft.draft());
            log.info(
                    "Extraction completed extractionId={} provider={} model={} mode={} durationMs={}",
                    extractionId,
                    llmProperties.getProvider(),
                    llmProperties.getModel(),
                    customerMode,
                    System.currentTimeMillis() - callStartMillis);
            return new ExtractionOutcome(
                    extractionId,
                    llmProperties.getModel(),
                    llmProperties.getExtractionPromptVersion(),
                    llmProperties.getExtractionSchemaVersion(),
                    translatedDraft.draft(),
                    translatedDraft.warnings());
        } catch (LlmUnavailableException | LlmBadResponseException | UnreadableImageException providerFailure) {
            log.warn(
                    "Extraction failed extractionId={} failure={}",
                    extractionId,
                    providerFailure.getClass().getSimpleName());
            throw providerFailure;
        } finally {
            deleteStagingDirectory(stagingDirectory);
        }
    }

    private ValidatedImage readImage(MultipartFile quotationImage) {
        if (quotationImage == null || quotationImage.isEmpty()) {
            throw new IllegalArgumentException("A handwritten quotation image is required.");
        }
        if (quotationImage.getSize() > fileUploadProperties.getMaxImageSizeBytes()) {
            throw new IllegalArgumentException(
                    "The quotation image exceeds the " + wholeMegabytes(fileUploadProperties.getMaxImageSizeBytes())
                            + " MB limit. Please use a smaller file.");
        }
        byte[] imageBytes = uploadBytes(quotationImage, "The quotation image could not be read. Please try again.");
        String imageType = SourceFileTypeDetector.resolveContentType(imageBytes, quotationImage.getContentType());
        if (!IMAGE_JPEG.equals(imageType) && !IMAGE_PNG.equals(imageType) && !IMAGE_WEBP.equals(imageType)) {
            throw new IllegalArgumentException("The quotation image must be a JPEG, PNG, or WebP file.");
        }
        return new ValidatedImage(imageBytes, imageType);
    }

    private ValidatedDocument readSupportingDocument(MultipartFile supportingDocument) {
        if (supportingDocument == null || supportingDocument.isEmpty()) {
            return null;
        }
        if (supportingDocument.getSize() > fileUploadProperties.getMaxSupportingDocumentSizeBytes()) {
            throw new IllegalArgumentException(
                    "The supporting document exceeds the "
                            + wholeMegabytes(fileUploadProperties.getMaxSupportingDocumentSizeBytes())
                            + " MB limit. Please use a smaller file.");
        }
        byte[] documentBytes =
                uploadBytes(supportingDocument, "The supporting document could not be read. Please try again.");
        String documentType =
                SourceFileTypeDetector.resolveContentType(documentBytes, supportingDocument.getContentType());
        if (documentType == null || !fileUploadProperties.getAllowedSupportingDocumentTypes().contains(documentType)) {
            throw new IllegalArgumentException(
                    "The supporting document type is not supported. Use a PDF, image, DOC, or DOCX file.");
        }
        if (MSWORD.equals(documentType) || WORD_OPEN_XML.equals(documentType)) {
            throw new IllegalArgumentException(
                    "Word documents cannot be sent to the AI provider yet. Please attach a PDF or image instead.");
        }
        return new ValidatedDocument(documentBytes, documentType);
    }

    private byte[] uploadBytes(MultipartFile uploadedFile, String failureMessage) {
        try {
            return uploadedFile.getBytes();
        } catch (IOException unreadableUpload) {
            throw new IllegalArgumentException(failureMessage, unreadableUpload);
        }
    }

    private Path stageTempFiles(ValidatedImage validatedImage, ValidatedDocument validatedDocument) {
        try {
            Path stagingDirectory = Files.createTempDirectory("vlg-extraction-");
            Files.write(
                    stagingDirectory.resolve("quotation-image" + extensionFor(validatedImage.contentType())),
                    validatedImage.content());
            if (validatedDocument != null) {
                Files.write(
                        stagingDirectory.resolve("supporting-document" + extensionFor(validatedDocument.contentType())),
                        validatedDocument.content());
            }
            return stagingDirectory;
        } catch (IOException stagingFailure) {
            throw new IllegalStateException("Could not stage the extraction files. Please try again.", stagingFailure);
        }
    }

    private String extensionFor(String contentType) {
        if (IMAGE_PNG.equals(contentType)) {
            return ".png";
        }
        if (IMAGE_WEBP.equals(contentType)) {
            return ".webp";
        }
        if (APPLICATION_PDF.equals(contentType)) {
            return ".pdf";
        }
        return ".jpg";
    }

    private void assertImageDecodable(Path stagingDirectory, String imageType) {
        try {
            boolean runtimeReadsImages = ImageIO.getImageReadersByMIMEType(imageType).hasNext();
            if (!runtimeReadsImages) {
                // No decoder for this type on this runtime; accept by signature/size and let the provider judge quality.
                return;
            }
            if (ImageIO.read(stagingDirectory.resolve("quotation-image" + extensionFor(imageType)).toFile()) == null) {
                throw new UnreadableImageException(
                        "The quotation image could not be read. Please capture a clearer image.");
            }
        } catch (IOException unreadableImage) {
            throw new UnreadableImageException(
                    "The quotation image could not be read. Please capture a clearer image.");
        }
    }

    private String buildCustomerContext(CustomerMode customerMode, String customerId, String customerContext) {
        if (customerMode == CustomerMode.NEW) {
            return StringUtils.hasText(customerContext) ? customerContext : "";
        }
        StringBuilder existingContext = new StringBuilder("Saved customer reference: ").append(customerId).append('.');
        if (StringUtils.hasText(customerContext)) {
            existingContext.append(" Saved details: ").append(customerContext.trim());
        }
        return existingContext.toString();
    }

    private long wholeMegabytes(long byteLimit) {
        return Math.max(1, byteLimit / 1_048_576L);
    }

    private void deleteStagingDirectory(Path stagingDirectory) {
        if (stagingDirectory == null) {
            return;
        }
        try (Stream<Path> stagedFiles = Files.walk(stagingDirectory)) {
            stagedFiles.sorted(Comparator.reverseOrder()).forEach(stagedPath -> {
                try {
                    Files.deleteIfExists(stagedPath);
                } catch (IOException cleanupFailure) {
                    log.warn("Extraction temp cleanup failed for a staged file.");
                }
            });
        } catch (IOException | UncheckedIOException cleanupFailure) {
            log.warn("Extraction temp cleanup failed.");
        }
    }

    /** Review-ready outcome. The draft always requires human approval. */
    public record ExtractionOutcome(
            String extractionId,
            String modelIdentifier,
            String promptVersion,
            String schemaVersion,
            QuotationExtractionResponse draft,
            List<com.vlg.quotation.extraction.schema.ExtractionWarning> warnings) {
    }
}
