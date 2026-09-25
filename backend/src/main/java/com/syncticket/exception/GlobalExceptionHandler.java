package com.syncticket.exception;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailed(
            ValidationFailedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        enrich(problem, ErrorCode.VALIDATION_FAILED, request);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, ErrorCode.TICKET_NOT_FOUND, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCommentNotFound(CommentNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, ErrorCode.TICKET_NOT_FOUND, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn(
                "Rejected transition ticketId path={} from={} to={}",
                request.getRequestURI(),
                ex.getCurrentStatus(),
                ex.getRequestedStatus());
        ProblemDetail problem = problem(
                HttpStatus.CONFLICT, ErrorCode.INVALID_STATUS_TRANSITION, ex.getMessage(), request);
        problem.setTitle("Invalid status transition");
        problem.setProperty("currentStatus", ex.getCurrentStatus().name());
        problem.setProperty("requestedStatus", ex.getRequestedStatus().name());
        problem.setProperty(
                "allowedTransitions",
                ex.getAllowedTransitions().stream().map(Enum::name).sorted().toList());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(TicketNotEditableException.class)
    public ResponseEntity<ProblemDetail> handleNotEditable(TicketNotEditableException ex, HttpServletRequest request) {
        ProblemDetail problem =
                problem(HttpStatus.CONFLICT, ErrorCode.TICKET_NOT_EDITABLE, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler({OptimisticLockException.class, ConcurrentModificationException.class})
    public ResponseEntity<ProblemDetail> handleOptimisticLock(RuntimeException ex, HttpServletRequest request) {
        String detail = ex.getMessage() != null
                ? ex.getMessage()
                : "This ticket was changed by someone else. Reload to see the latest version.";
        ProblemDetail problem = problem(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION, detail, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        ProblemDetail problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Something went wrong on our side. Please try again.",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        enrich(problem, ErrorCode.VALIDATION_FAILED, request);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is malformed or invalid.");
        enrich(problem, ErrorCode.VALIDATION_FAILED, request);
        problem.setTitle("Validation failed");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'.");
        enrich(problem, ErrorCode.VALIDATION_FAILED, request);
        problem.setTitle("Validation failed");
        return ResponseEntity.badRequest().body(problem);
    }

    private FieldErrorDetail toFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid value.";
        return new FieldErrorDetail(fieldError.getField(), message);
    }

    private static ProblemDetail problem(
            HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        enrich(problem, code, request);
        return problem;
    }

    private static void enrich(ProblemDetail problem, String code, WebRequest request) {
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        if (request instanceof org.springframework.web.context.request.ServletWebRequest servletWebRequest) {
            problem.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
    }

    private static void enrich(ProblemDetail problem, String code, HttpServletRequest request) {
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setInstance(URI.create(request.getRequestURI()));
    }
}
