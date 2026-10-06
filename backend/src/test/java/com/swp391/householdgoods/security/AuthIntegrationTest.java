package com.swp391.householdgoods.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.identity.domain.AccountRole;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.bootstrap-manager.email=manager.auth-test@example.invalid",
        "app.bootstrap-manager.password=AuthTest123",
        "app.bootstrap-manager.full-name=Quản lý kiểm thử"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    private static final String TEST_EMAIL =
            "manager.auth-test@example.invalid";

    private static final String TEST_PASSWORD =
            "AuthTest123";

    private static final String PENDING_EMAIL =
            "pending.auth-test@example.invalid";

    private static final String PENDING_PASSWORD =
            "PendingTest123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginSessionCsrfLogoutFlowUsesPostgresqlAccount()
            throws Exception {

        UserAccount persistedManager =
                repository.findByEmailIgnoreCase(TEST_EMAIL)
                        .orElseThrow();

        assertThat(persistedManager.getRole())
                .isEqualTo(AccountRole.MANAGER);

        assertThat(persistedManager.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(persistedManager.getPasswordHash())
                .startsWith("$2")
                .doesNotContain(TEST_PASSWORD);

        MvcResult initialCsrfResult =
                mockMvc.perform(get("/api/v1/auth/csrf"))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.headerName")
                                        .value("X-XSRF-TOKEN"))
                        .andReturn();

        JsonNode initialCsrf = data(initialCsrfResult);

        Cookie initialCsrfCookie =
                requiredCookie(
                        initialCsrfResult,
                        "XSRF-TOKEN");

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .cookie(initialCsrfCookie)
                                        .header(
                                                initialCsrf
                                                        .get("headerName")
                                                        .asText(),
                                                initialCsrf
                                                        .get("token")
                                                        .asText())
                                        .contentType(
                                                MediaType
                                                        .APPLICATION_FORM_URLENCODED)
                                        .param(
                                                "email",
                                                TEST_EMAIL)
                                        .param(
                                                "password",
                                                TEST_PASSWORD))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.authenticated")
                                        .value(true))
                        .andExpect(
                                jsonPath("$.data.role")
                                        .value("MANAGER"))
                        .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        assertThat(authenticatedSession)
                .isNotNull();

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.authenticated")
                                .value(true))
                .andExpect(
                        jsonPath("$.data.email")
                                .value(TEST_EMAIL));

        MvcResult refreshedCsrfResult =
                mockMvc.perform(
                                get("/api/v1/auth/csrf")
                                        .session(
                                                authenticatedSession))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode refreshedCsrf =
                data(refreshedCsrfResult);

        Cookie refreshedCsrfCookie =
                requiredCookie(
                        refreshedCsrfResult,
                        "XSRF-TOKEN");

        mockMvc.perform(
                        patch("/api/v1/auth/me")
                                .session(authenticatedSession)
                                .cookie(refreshedCsrfCookie)
                                .header(
                                        refreshedCsrf.get("headerName").asText(),
                                        refreshedCsrf.get("token").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        java.util.Map.of("fullName", "Tên quản lý đã cập nhật"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Tên quản lý đã cập nhật"));

        mockMvc.perform(get("/api/v1/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Tên quản lý đã cập nhật"));

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .session(
                                        authenticatedSession)
                                .cookie(
                                        refreshedCsrfCookie)
                                .header(
                                        refreshedCsrf
                                                .get("headerName")
                                                .asText(),
                                        refreshedCsrf
                                                .get("token")
                                                .asText()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.authenticated")
                                .value(false));

        mockMvc.perform(
                        get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.authenticated")
                                .value(false));
    }

    @Test
    void loginWithoutCsrfIsRejectedWithoutCreatingSession()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType
                                                .APPLICATION_FORM_URLENCODED)
                                .param(
                                        "email",
                                        TEST_EMAIL)
                                .param(
                                        "password",
                                        TEST_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("CSRF_INVALID"));
    }

    @Test
    void loginWithWrongPasswordIsRejected()
            throws Exception {

        MvcResult csrfResult =
                mockMvc.perform(
                                get("/api/v1/auth/csrf"))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode csrf = data(csrfResult);

        Cookie csrfCookie =
                requiredCookie(
                        csrfResult,
                        "XSRF-TOKEN");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .cookie(csrfCookie)
                                .header(
                                        csrf.get("headerName")
                                                .asText(),
                                        csrf.get("token")
                                                .asText())
                                .contentType(
                                        MediaType
                                                .APPLICATION_FORM_URLENCODED)
                                .param(
                                        "email",
                                        TEST_EMAIL)
                                .param(
                                        "password",
                                        "WrongPassword123"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_FAILED"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Email hoặc mật khẩu không đúng."));
    }

    @Test
    void loginWithUnknownEmailIsRejected()
            throws Exception {

        MvcResult csrfResult =
                mockMvc.perform(
                                get("/api/v1/auth/csrf"))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode csrf = data(csrfResult);

        Cookie csrfCookie =
                requiredCookie(
                        csrfResult,
                        "XSRF-TOKEN");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .cookie(csrfCookie)
                                .header(
                                        csrf.get("headerName")
                                                .asText(),
                                        csrf.get("token")
                                                .asText())
                                .contentType(
                                        MediaType
                                                .APPLICATION_FORM_URLENCODED)
                                .param(
                                        "email",
                                        "not-found@example.invalid")
                                .param(
                                        "password",
                                        TEST_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_FAILED"));
    }

    @Test
    void pendingVerificationAccountCannotLogin()
            throws Exception {

        repository.findByEmailIgnoreCase(PENDING_EMAIL)
                .ifPresent(repository::delete);

        UserAccount pendingAccount =
                UserAccount.registerCustomer(
                        PENDING_EMAIL,
                        passwordEncoder.encode(
                                PENDING_PASSWORD),
                        "Khách hàng chưa xác minh",
                        Instant.now());

        repository.save(pendingAccount);

        assertThat(pendingAccount.getStatus())
                .isEqualTo(
                        AccountStatus.PENDING_VERIFICATION);

        MvcResult csrfResult =
                mockMvc.perform(
                                get("/api/v1/auth/csrf"))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode csrf = data(csrfResult);

        Cookie csrfCookie =
                requiredCookie(
                        csrfResult,
                        "XSRF-TOKEN");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .cookie(csrfCookie)
                                .header(
                                        csrf.get("headerName")
                                                .asText(),
                                        csrf.get("token")
                                                .asText())
                                .contentType(
                                        MediaType
                                                .APPLICATION_FORM_URLENCODED)
                                .param(
                                        "email",
                                        PENDING_EMAIL)
                                .param(
                                        "password",
                                        PENDING_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_FAILED"));
    }

    @AfterAll
    void removeTestAccounts() {
        repository.findByEmailIgnoreCase(TEST_EMAIL)
                .ifPresent(repository::delete);

        repository.findByEmailIgnoreCase(PENDING_EMAIL)
                .ifPresent(repository::delete);
    }

    private JsonNode data(MvcResult result)
            throws Exception {

        return objectMapper
                .readTree(
                        result
                                .getResponse()
                                .getContentAsByteArray())
                .get("data");
    }

    private Cookie requiredCookie(
            MvcResult result,
            String name) {

        Cookie cookie =
                result
                        .getResponse()
                        .getCookie(name);

        assertThat(cookie)
                .as("response cookie %s", name)
                .isNotNull();

        return cookie;
    }
}
