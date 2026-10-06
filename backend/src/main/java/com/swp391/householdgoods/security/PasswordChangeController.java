package com.swp391.householdgoods.security;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.identity.application.PasswordChangeException;
import com.swp391.householdgoods.identity.application.PasswordChangeService;
import com.swp391.householdgoods.identity.application.dto.ChangePasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordChangeController {
    private final PasswordChangeService service;

    public PasswordChangeController(PasswordChangeService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/auth/password")
    public ApiEnvelope<ChangePasswordResponse> change(@Valid @RequestBody ChangePasswordRequest body,
            Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        try {
            service.changeOwnPassword(authentication.getName(), body);
        } catch (DataAccessException | TransactionException failure) {
            // SQL failures can contain row data/hash: do not pass them to generic exception logging.
            throw new PasswordChangeException(HttpStatus.SERVICE_UNAVAILABLE, "PASSWORD_CHANGE_UNAVAILABLE",
                    "Chưa thể đổi mật khẩu. Vui lòng thử lại.", null);
        }
        // Service transaction has committed before revoking the submitting session.
        SessionLogout.logout(request, response, authentication);
        return new ApiEnvelope<>(new ChangePasswordResponse(true, true,
                "Đã đổi mật khẩu. Vui lòng đăng nhập lại bằng mật khẩu mới."));
    }

    public record ChangePasswordResponse(boolean changed, boolean loginRequired, String message) {}
}
