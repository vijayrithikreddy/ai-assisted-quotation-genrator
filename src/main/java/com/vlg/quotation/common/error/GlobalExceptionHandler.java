package com.vlg.quotation.common.error;

import com.vlg.quotation.api.response.ApiErrorResponse;
import com.vlg.quotation.common.web.RequestIdFilter;
import com.vlg.quotation.extraction.LlmBadResponseException;
import com.vlg.quotation.extraction.LlmUnavailableException;
import com.vlg.quotation.extraction.UnreadableImageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every failure to the stable {@link ApiErrorResponse} contract.
 *
 * <p>Messages stay user-safe and plain-language. Technical details go to
 * server logs only, and secrets are never logged here.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String IMAGE_UNREADABLE = "IMAGE_UNREADABLE";
    public static final String AI_BAD_RESPONSE = "AI_BAD_RESPONSE";
    public static final String AI_PROVIDER_UNAVAILABLE = "AI_PROVIDER_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException validationFailure, HttpServletRequest servletRequest) {
        String fieldMessage = validationFailure.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " is invalid")
                .orElse("The request is invalid.");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, INVALID_REQUEST, fieldMessage, servletRequest);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainRuleViolation(
            IllegalArgumentException domainViolation, HttpServletRequest servletRequest) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, INVALID_REQUEST, domainViolation.getMessage(), servletRequest);
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception invalidRequestFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                "The request is invalid. Please check the values and try again.",
                servletRequest);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingUploadPart(
            MissingServletRequestPartException missingPartFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                "A handwritten quotation image is required.",
                servletRequest);
    }

    @ExceptionHandler(UnreadableImageException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableImage(
            UnreadableImageException unreadableImageFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY, IMAGE_UNREADABLE, unreadableImageFailure.getMessage(), servletRequest);
    }

    @ExceptionHandler(LlmBadResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleBadProviderResponse(
            LlmBadResponseException badResponseFailure, HttpServletRequest servletRequest) {
        // Never include provider payload details; the message is already user-safe.
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY, AI_BAD_RESPONSE, badResponseFailure.getMessage(), servletRequest);
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailableProvider(
            LlmUnavailableException unavailableFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE, AI_PROVIDER_UNAVAILABLE, unavailableFailure.getMessage(), servletRequest);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleOversizedUpload(
            MaxUploadSizeExceededException oversizedUploadFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                PAYLOAD_TOO_LARGE,
                "The uploaded file is too large. Please choose a smaller file.",
                servletRequest);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUnknownPath(
            NoResourceFoundException unknownPathFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND, NOT_FOUND, "The requested path was not found.", servletRequest);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleWrongMethod(
            HttpRequestMethodNotSupportedException wrongMethodFailure, HttpServletRequest servletRequest) {
        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                METHOD_NOT_ALLOWED,
                "This action is not supported for the requested path.",
                servletRequest);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedFailure(
            Exception unexpectedFailure, HttpServletRequest servletRequest) {
        // Log the exception class only; never log request bodies, documents, or secrets.
        log.error(
                "Unhandled failure for requestId={} exception={}",
                resolveRequestId(servletRequest),
                unexpectedFailure.getClass().getSimpleName());
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR,
                "Something went wrong. Please try again.",
                servletRequest);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus httpStatus, String errorCode, String message, HttpServletRequest servletRequest) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                httpStatus.value(),
                errorCode,
                message,
                resolveRequestId(servletRequest));
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    private String resolveRequestId(HttpServletRequest servletRequest) {
        Object requestId = servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String ? (String) requestId : "req-unknown";
    }
}
