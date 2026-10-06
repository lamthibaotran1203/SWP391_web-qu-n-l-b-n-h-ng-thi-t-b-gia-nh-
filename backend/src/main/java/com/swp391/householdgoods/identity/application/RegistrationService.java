package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.application.dto.RegisterRequest;
import com.swp391.householdgoods.identity.application.dto.RegisterResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Duration VERIFICATION_LIFETIME = Duration.ofHours(24);

    private final UserAccountRepository accountRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationMailService registrationMailService;
    private final Clock clock;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public RegistrationService(
            UserAccountRepository accountRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            RegistrationMailService registrationMailService,
            Clock clock) {

        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationMailService = registrationMailService;
        this.clock = clock;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        Instant now = clock.instant();

        UserAccount account = UserAccount.registerCustomer(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().strip(),
                now);

        accountRepository.save(account);

        String rawToken = generateToken();

        EmailVerificationToken verificationToken =
                new EmailVerificationToken(
                        account,
                        hashToken(rawToken),
                        now.plus(VERIFICATION_LIFETIME),
                        now);

        tokenRepository.save(verificationToken);

        String verificationUrl =
                appBaseUrl + "/verify-email?token=" + rawToken;

        registrationMailService.sendVerificationEmail(
                account.getEmail(),
                account.getFullName(),
                verificationUrl);

        return new RegisterResponse(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.");
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();

        EmailVerificationToken token = tokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() ->
                        new IllegalArgumentException("Token xác minh không hợp lệ."));

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

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed =
                    digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashed);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception);
        }
    }
}