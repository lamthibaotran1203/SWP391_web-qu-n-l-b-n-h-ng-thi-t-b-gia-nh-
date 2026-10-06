package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.application.dto.ChangePasswordRequest;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {
    private final UserAccountRepository repository;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public PasswordChangeService(UserAccountRepository repository, PasswordEncoder encoder, Clock clock) {
        this.repository = repository;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public void changeOwnPassword(String authenticatedEmail, ChangePasswordRequest request) {
        var account = repository.findForPasswordChange(authenticatedEmail).orElseThrow(() ->
                new PasswordChangeException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                        "Bạn cần đăng nhập lại.", null));
        if (account.getStatus() != AccountStatus.ACTIVE || account.getPasswordHash() == null) {
            throw new PasswordChangeException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Bạn cần đăng nhập lại.", null);
        }
        String current = request.currentPassword();
        if (current == null || current.getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(current, account.getPasswordHash())) {
            throw invalid("CURRENT_PASSWORD_INVALID", "Không thể xác minh mật khẩu hiện tại.", "currentPassword");
        }
        if (!PasswordPolicy.accepts(request.newPassword())) {
            throw invalid("NEW_PASSWORD_INVALID", PasswordPolicy.rejectionMessage(request.newPassword()), "newPassword");
        }
        if (!request.newPassword().equals(request.confirmation())) {
            throw invalid("PASSWORD_CONFIRMATION_MISMATCH", "Xác nhận mật khẩu mới chưa khớp.", "confirmation");
        }
        if (encoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw invalid("PASSWORD_REUSED", "Mật khẩu mới phải khác mật khẩu hiện tại.", "newPassword");
        }
        // The account lock serializes concurrent changes. JPA also increments @Version.
        account.changePassword(encoder.encode(request.newPassword()), clock.instant());
        repository.saveAndFlush(account);
    }

    private PasswordChangeException invalid(String code, String message, String field) {
        return new PasswordChangeException(HttpStatus.BAD_REQUEST, code, message, field);
    }
}
