package com.swp391.householdgoods.security;

import com.swp391.householdgoods.common.api.ApiProblemWriter;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revokes old password sessions on their next API request, including auth/me. */
@Component
public class CredentialSessionFilter extends OncePerRequestFilter {
    private final AccountQueryService accounts;
    private final ApiProblemWriter problems;

    public CredentialSessionFilter(AccountQueryService accounts, ApiProblemWriter problems) {
        this.accounts = accounts;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !apiPath(request).startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionAccountPrincipal principal) {
            boolean current;
            try {
                var account = accounts.findForAuthentication(authentication.getName());
                current = account.status() == AccountStatus.ACTIVE
                        && principal.hasCurrentCredential(account.passwordHash())
                        && principal.hasCurrentRole(account.role());
            } catch (UsernameNotFoundException missing) {
                current = false;
            } catch (DataAccessException unavailable) {
                problems.write(request, response, 503, "Service Unavailable", "AUTHENTICATION_UNAVAILABLE",
                        "Chưa thể kiểm tra phiên đăng nhập. Vui lòng thử lại.");
                return;
            }
            if (!current) {
                SessionLogout.logout(request, response, authentication);
                String path = apiPath(request);
                if (!path.equals("/api/v1/auth/me") && !path.equals("/api/v1/auth/csrf")) {
                    problems.write(request, response, 401, "Unauthorized", "AUTHENTICATION_REQUIRED",
                            "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String apiPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
