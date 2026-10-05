package com.swp391.householdgoods.security;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.identity.application.AuthenticatedAccount;
import com.swp391.householdgoods.identity.application.RegistrationService;
import com.swp391.householdgoods.identity.application.dto.RegisterRequest;
import com.swp391.householdgoods.identity.application.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountQueryService accountQueryService;
    private final RegistrationService registrationService;

    public AuthController(
            AccountQueryService accountQueryService,
            RegistrationService registrationService) {
        this.accountQueryService = accountQueryService;
        this.registrationService = registrationService;
    }

    @GetMapping("/csrf")
    ApiEnvelope<CsrfResponse> csrf(CsrfToken csrfToken) {
        return new ApiEnvelope<>(new CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()));
    }

    @GetMapping("/me")
    ApiEnvelope<CurrentUserResponse> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return new ApiEnvelope<>(CurrentUserResponse.anonymous());
        }

        AuthenticatedAccount account =
                accountQueryService.findForAuthentication(
                        authentication.getName());

        return new ApiEnvelope<>(new CurrentUserResponse(
                true,
                account.email(),
                account.fullName(),
                account.role().name()));
    }

    @PostMapping("/register")
    ApiEnvelope<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return new ApiEnvelope<>(
                registrationService.register(request));
    }

    @GetMapping("/verify-email")
    ApiEnvelope<String> verifyEmail(
            @RequestParam String token) {

        registrationService.verifyEmail(token);

        return new ApiEnvelope<>(
                "Xác minh email thành công.");
    }
}