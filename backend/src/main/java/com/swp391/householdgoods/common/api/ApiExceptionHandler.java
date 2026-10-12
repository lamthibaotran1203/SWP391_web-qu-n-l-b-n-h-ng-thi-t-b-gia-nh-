package com.swp391.householdgoods.common.api;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.common.web.TraceIdFilter;
import com.swp391.householdgoods.orders.application.InvalidSalesOrderSearchException;
import com.swp391.householdgoods.orders.application.SalesOrderNotFoundException;
import com.swp391.householdgoods.orders.application.SalesOrderStatusConflictException;
import com.swp391.householdgoods.purchasing.application.SupplierDuplicateException;
import com.swp391.householdgoods.purchasing.application.SupplierNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiProblem> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Dữ liệu gửi lên chưa hợp lệ.",
                fieldErrors,
                request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    ResponseEntity<ApiProblem> handleRequestParameterValidation(HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Tham số tìm kiếm chưa hợp lệ.",
                Map.of(),
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiProblem> handleNotFound(
            HttpServletRequest request) {

        return problem(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Không tìm thấy tài nguyên được yêu cầu.",
                Map.of(),
                request);
    }

    @ExceptionHandler(SalesOrderNotFoundException.class)
    ResponseEntity<ApiProblem> handleSalesOrderNotFound(
            SalesOrderNotFoundException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                "Không tìm thấy đơn hàng.",
                Map.of(),
                request);
    }

    @ExceptionHandler(SalesOrderStatusConflictException.class)
    ResponseEntity<ApiProblem> handleSalesOrderStatusConflict(
            SalesOrderStatusConflictException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "ORDER_STATUS_CONFLICT",
                "Trạng thái đơn hàng đã thay đổi hoặc không thể xác nhận.",
                Map.of(),
                request);
    }

    @ExceptionHandler(InvalidSalesOrderSearchException.class)
    ResponseEntity<ApiProblem> handleInvalidSalesOrderSearch(
            InvalidSalesOrderSearchException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "INVALID_ORDER_SEARCH",
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(CatalogException.class)
    ResponseEntity<ApiProblem> handleCatalog(
            CatalogException exception,
            HttpServletRequest request) {
        return problem(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    ResponseEntity<ApiProblem> handleSupplierNotFound(
            SupplierNotFoundException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "SUPPLIER_NOT_FOUND",
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(SupplierDuplicateException.class)
    ResponseEntity<ApiProblem> handleSupplierDuplicate(
            SupplierDuplicateException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "SUPPLIER_DUPLICATE",
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(com.swp391.householdgoods.purchasing.application.PurchaseOrderValidationException.class)
    ResponseEntity<ApiProblem> handlePurchaseOrderValidation(
            com.swp391.householdgoods.purchasing.application.PurchaseOrderValidationException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "PURCHASE_ORDER_INVALID",
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiProblem> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "REQUEST_INVALID",
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(com.swp391.householdgoods.identity.application.PasswordChangeException.class)
    ResponseEntity<ApiProblem> handlePasswordChange(
            com.swp391.householdgoods.identity.application.PasswordChangeException exception,
            HttpServletRequest request) {
        Map<String, String> fields = exception.field() == null ? Map.of()
                : Map.of(exception.field(), exception.getMessage());
        return problem(exception.status(), exception.code(), exception.getMessage(), fields, request);
    }

    @ExceptionHandler(com.swp391.householdgoods.identity.application.ProfileException.class)
    ResponseEntity<ApiProblem> handleProfile(
            com.swp391.householdgoods.identity.application.ProfileException exception,
            HttpServletRequest request) {
        Map<String, String> fields = exception.field() == null ? Map.of()
                : Map.of(exception.field(), exception.getMessage());
        return problem(exception.status(), exception.code(), exception.getMessage(), fields, request);
    }

    @ExceptionHandler(com.swp391.householdgoods.identity.application.EmailVerificationException.class)
    ResponseEntity<ApiProblem> handleEmailVerification(
            com.swp391.householdgoods.identity.application.EmailVerificationException exception,
            HttpServletRequest request) {
        return problem(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                Map.of(),
                request);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ApiProblem> handleUnreadableBody(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Dữ liệu gửi lên chưa hợp lệ.", Map.of(), request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiProblem> handleAccessDenied(HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này.", Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        String traceId = TraceIdFilter.getTraceId(request);

        log.error(
                "Unhandled request failure traceId={}",
                traceId,
                exception);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.",
                Map.of(),
                request);
    }

    private ResponseEntity<ApiProblem> problem(
            HttpStatus status,
            String code,
            String detail,
            Map<String, String> fieldErrors,
            HttpServletRequest request) {

        ApiProblem body = ApiProblem.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                detail,
                fieldErrors,
                TraceIdFilter.getTraceId(request));

        return ResponseEntity
                .status(status)
                .body(body);
    }
}
