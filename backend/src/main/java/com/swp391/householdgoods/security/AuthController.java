package com.swp391.householdgoods.security;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.identity.application.AuthenticatedAccount;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountQueryService accountQueryService;

    public AuthController(AccountQueryService accountQueryService) {
        this.accountQueryService = accountQueryService;
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

        AuthenticatedAccount account = accountQueryService.findForAuthentication(authentication.getName());
        return new ApiEnvelope<>(new CurrentUserResponse(
                true,
                account.email(),
                account.fullName(),
                account.role().name()));
    }
}
