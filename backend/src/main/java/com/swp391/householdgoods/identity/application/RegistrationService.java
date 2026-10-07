package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.application.dto.RegisterRequest;
import com.swp391.householdgoods.identity.application.dto.RegisterResponse;
import com.swp391.householdgoods.identity.application.dto.ResendVerificationRequest;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.domain.EmailVerificationToken;
import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.EmailVerificationTokenRepository;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Duration VERIFICATION_LIFETIME =
            Duration.ofHours(24);

    private final UserAccountRepository accountRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public RegistrationService(
            UserAccountRepository accountRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {

        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        UserAccount existingAccount =
                accountRepository
                        .findByEmailIgnoreCase(email)
                        .orElse(null);

        if (existingAccount != null) {
            if (existingAccount.getStatus()
                    == AccountStatus.PENDING_VERIFICATION) {

                createVerificationToken(existingAccount);

                return new RegisterResponse(
                        "Tài khoản đang chờ xác minh. "
                                + "Email xác minh mới đã được gửi.");
            }

            throw new IllegalArgumentException(
                    "Email đã được sử dụng.");
        }

        Instant now = clock.instant();

        UserAccount account =
                UserAccount.registerCustomer(
                        email,
                        passwordEncoder.encode(
                                request.password()),
                        request.fullName().strip(),
                        now);

        accountRepository.save(account);

        createVerificationToken(account);

        return new RegisterResponse(
                "Đăng ký thành công. "
                        + "Vui lòng kiểm tra email để xác minh tài khoản.");
    }

    @Transactional
    public RegisterResponse resendVerification(
            ResendVerificationRequest request) {

        String email = normalizeEmail(request.email());

        UserAccount account =
                accountRepository
                        .findByEmailIgnoreCase(email)
                        .orElse(null);

        if (account == null
                || account.getStatus()
                != AccountStatus.PENDING_VERIFICATION) {

            return new RegisterResponse(
                    "Nếu tài khoản cần xác minh, "
                            + "email xác minh mới đã được gửi.");
        }

        createVerificationToken(account);

        return new RegisterResponse(
                "Nếu tài khoản cần xác minh, "
                        + "email xác minh mới đã được gửi.");
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();

        EmailVerificationToken token =
                tokenRepository
                        .findByTokenHash(
                                hashToken(rawToken))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Token xác minh không hợp lệ."));

        if (token.isUsed()) {
            throw new IllegalArgumentException(
                    "Token xác minh đã được sử dụng.");
        }

        if (token.isExpired(now)) {
            throw new IllegalArgumentException(
                    "Token xác minh đã hết hạn.");
        }

        UserAccount account = token.getUser();

        account.verifyEmail(now);
        token.markUsed(now);

        accountRepository.save(account);
        tokenRepository.save(token);
    }

    private void createVerificationToken(
            UserAccount account) {

        Instant now = clock.instant();

        tokenRepository
                .findAllByUserAndUsedAtIsNull(account)
                .forEach(token -> token.markUsed(now));

        String rawToken = generateToken();

        EmailVerificationToken verificationToken =
                new EmailVerificationToken(
                        account,
                        hashToken(rawToken),
                        now.plus(VERIFICATION_LIFETIME),
                        now);

        tokenRepository.save(verificationToken);

        String verificationUrl =
                appBaseUrl
                        + "/verify-email?token="
                        + rawToken;

        eventPublisher.publishEvent(
                new VerificationEmailRequested(
                        account.getEmail(),
                        account.getFullName(),
                        verificationUrl));
    }

    private String normalizeEmail(String email) {
        return email
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");

            byte[] hashed =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(hashed);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception);
        }
    }
}