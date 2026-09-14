package com.swp391.householdgoods.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.common.api.ApiProblemWriter;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.identity.application.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

@Component
public class SecurityResponseHandlers {

    private final ApiProblemWriter problemWriter;
    private final AccountQueryService accountQueryService;
    private final ObjectMapper objectMapper;

    public SecurityResponseHandlers(
            ApiProblemWriter problemWriter,
            AccountQueryService accountQueryService,
            ObjectMapper objectMapper) {
        this.problemWriter = problemWriter;
        this.accountQueryService = accountQueryService;
        this.objectMapper = objectMapper;
    }

    AuthenticationSuccessHandler loginSuccess() {
        return (request, response, authentication) -> {
            AuthenticatedAccount account = accountQueryService.findForAuthentication(authentication.getName());
            writeJson(response, new ApiEnvelope<>(new CurrentUserResponse(
                    true,
                    account.email(),
                    account.fullName(),
                    account.role().name())));
        };
    }

    AuthenticationFailureHandler loginFailure() {
        return (request, response, exception) -> problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "AUTHENTICATION_FAILED",
                "Email hoặc mật khẩu không đúng.");
    }

    LogoutSuccessHandler logoutSuccess() {
        return (request, response, authentication) ->
                writeJson(response, new ApiEnvelope<>(CurrentUserResponse.anonymous()));
    }

    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "AUTHENTICATION_REQUIRED",
                "Bạn cần đăng nhập để thực hiện thao tác này.");
    }

    AccessDeniedHandler accessDenied() {
        return (request, response, exception) -> {
            boolean csrfFailure = exception instanceof MissingCsrfTokenException
                    || exception instanceof InvalidCsrfTokenException;
            problemWriter.write(
                    request,
                    response,
                    HttpStatus.FORBIDDEN.value(),
                    "Forbidden",
                    csrfFailure ? "CSRF_INVALID" : "ACCESS_DENIED",
                    csrfFailure
                            ? "Phiên bảo mật đã hết hạn. Vui lòng thử lại."
                            : "Bạn không có quyền thực hiện thao tác này.");
        };
    }

    private void writeJson(HttpServletResponse response, Object body) throws java.io.IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
