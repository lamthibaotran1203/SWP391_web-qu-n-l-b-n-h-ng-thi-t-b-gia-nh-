package com.swp391.householdgoods.identity.application;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.identity.application.dto.ChangePasswordRequest;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import com.swp391.householdgoods.security.SessionAccountPrincipal;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordChangeIntegrationTest {
    private static final String OLD = "Known password 1";
    private static final String NEW = "a simple new phrase";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired PasswordChangeService service;
    @Autowired UserAccountRepository accounts;
    @Autowired PlatformTransactionManager transactions;
    private String email;
    private String originalHash;
    private long id;

    @BeforeEach void createAccount() {
        email = "password-" + UUID.randomUUID() + "@example.invalid";
        originalHash = encoder.encode(OLD);
        id = jdbc.queryForObject("insert into app.user_account(email,password_hash,full_name,role,status,"
                + "email_verified_at,created_at,updated_at) values (?,?,'Người kiểm thử','CUSTOMER',"
                + "'ACTIVE',now(),now(),now()) returning user_id", Long.class, email, originalHash);
    }
    @AfterEach void removeAccount() { jdbc.update("delete from app.user_account where user_id=?",id); }

    @ParameterizedTest @ValueSource(strings={"CUSTOMER","STAFF","ADMIN"})
    void everyAuthenticatedRoleChangesOnlyOwnCredentialAndOldPasswordStopsWorking(String role) throws Exception {
        jdbc.update("update app.user_account set role=? where user_id=?",role,id);
        var session = login(OLD);
        var principal = ((SecurityContext)session.getAttribute("SPRING_SECURITY_CONTEXT")).getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(SessionAccountPrincipal.class);
        assertThat(((SessionAccountPrincipal)principal).getPassword()).isNull(); // Spring erased credentials.
        submit(session, new ChangePasswordRequest(OLD,NEW,NEW)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.loginRequired").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertThat(session.isInvalid()).isTrue();
        String stored = hash();
        assertThat(stored).startsWith("$2").isNotEqualTo(originalHash).doesNotContain(NEW);
        assertThat(encoder.matches(NEW,stored)).isTrue();
        assertThat(jdbc.queryForObject("select version from app.user_account where user_id=?",Long.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select full_name from app.user_account where user_id=?",String.class,id)).isEqualTo("Người kiểm thử");
        attemptLogin(OLD).andExpect(status().isUnauthorized());
        attemptLogin(NEW).andExpect(status().isOk());
    }

    @Test void anotherOldSessionIsRejectedAndMeBecomesAnonymous() throws Exception {
        var first = login(OLD); var second = login(OLD);
        var staleCsrf = csrf(second);
        submit(first,new ChangePasswordRequest(OLD,NEW,NEW)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/password").session(second).cookie(staleCsrf.cookie)
                .header("X-XSRF-TOKEN",staleCsrf.token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ChangePasswordRequest(OLD,"another phrase","another phrase"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        assertThat(second.isInvalid()).isTrue();
        var third = login(NEW);
        service.changeOwnPassword(email,new ChangePasswordRequest(NEW,"yet another phrase","yet another phrase"));
        mvc.perform(get("/api/v1/auth/me").session(third)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test void wrongCurrentMismatchReuseAndInvalidPolicyNeverModifyCredentialOrSession() throws Exception {
        var session = login(OLD);
        List<ChangePasswordRequest> rejected = List.of(
            new ChangePasswordRequest("wrong",NEW,NEW),
            new ChangePasswordRequest(OLD,NEW,"does not match"),
            new ChangePasswordRequest(OLD,OLD,OLD),
            new ChangePasswordRequest(OLD,"short","short"),
            new ChangePasswordRequest(OLD,"a".repeat(65),"a".repeat(65)),
            new ChangePasswordRequest(OLD,"é".repeat(37),"é".repeat(37)));
        for (var body: rejected) {
            var result = submit(session,body).andExpect(status().isBadRequest()).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain(OLD,NEW,originalHash);
            assertThat(hash()).isEqualTo(originalHash);
            assertThat(session.isInvalid()).isFalse();
        }
        assertThat(jdbc.queryForObject("select version from app.user_account where user_id=?",Long.class,id)).isZero();
    }

    @Test void utf8BoundarySpacesAndNonCompositionPolicyAreEnforcedWithoutTrimming() {
        assertThat(PasswordPolicy.accepts("é".repeat(36))).isTrue(); // exactly 72 bytes
        assertThat(PasswordPolicy.accepts("é".repeat(37))).isFalse();
        assertThat(PasswordPolicy.accepts("😀".repeat(8))).isTrue();
        assertThat(PasswordPolicy.accepts("😀".repeat(7))).isFalse();
        assertThat(PasswordPolicy.accepts("a".repeat(64))).isTrue();
        assertThat(PasswordPolicy.accepts("abcdefgh")).isTrue();
        assertThat(PasswordPolicy.accepts(" abcdefg ")).isTrue();
        String spaced = " new password ";
        service.changeOwnPassword(email,new ChangePasswordRequest(OLD,spaced,spaced));
        assertThat(encoder.matches(spaced,hash())).isTrue();
        assertThat(encoder.matches(spaced.strip(),hash())).isFalse();
    }

    @Test void csrfAnonymousInactiveAndMalformedRequestsCannotChangePassword() throws Exception {
        var body = json.writeValueAsString(new ChangePasswordRequest(OLD,NEW,NEW));
        var session = login(OLD);
        mvc.perform(post("/api/v1/auth/password").session(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        var anonymous = csrf(null);
        mvc.perform(post("/api/v1/auth/password").cookie(anonymous.cookie).header("X-XSRF-TOKEN",anonymous.token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        var csrf = csrf(session);
        mvc.perform(post("/api/v1/auth/password").session(session).cookie(csrf.cookie).header("X-XSRF-TOKEN",csrf.token)
                .contentType(MediaType.APPLICATION_JSON).content("{ malformed"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
        jdbc.update("update app.user_account set status='INACTIVE' where user_id=?",id);
        mvc.perform(post("/api/v1/auth/password").session(session).cookie(csrf.cookie)
                .header("X-XSRF-TOKEN",csrf.token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        assertThat(hash()).isEqualTo(originalHash);
    }

    @Test void lockSerializesTwoConcurrentChangesAndOnlyOneNewPasswordWins() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            Callable<Boolean> changeA=()->{start.await();return tryChange("first new phrase");};
            Callable<Boolean> changeB=()->{start.await();return tryChange("second new phrase");};
            var a=pool.submit(changeA);var b=pool.submit(changeB); start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            assertThat(jdbc.queryForObject("select version from app.user_account where user_id=?",Long.class,id)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    private boolean tryChange(String newPassword) {
        try { service.changeOwnPassword(email,new ChangePasswordRequest(OLD,newPassword,newPassword)); return true; }
        catch(PasswordChangeException rejected) {
            assertThat(rejected.code()).isEqualTo("CURRENT_PASSWORD_INVALID"); return false;
        }
    }

    @Test void databaseWriteFailureRollsBackAndDoesNotExposeCredentialThroughApiProblem() throws Exception {
        // Test-only trigger simulates a real PostgreSQL storage failure. Removed even if test fails.
        jdbc.execute("create function app.test_reject_password_write() returns trigger language plpgsql as $$ begin "
                + "if new.email like 'password-%@example.invalid' and new.password_hash is distinct from old.password_hash "
                + "then raise exception 'simulated storage failure'; end if; return new; end $$");
        jdbc.execute("create trigger test_password_write before update on app.user_account "
                + "for each row execute function app.test_reject_password_write()");
        try {
            var session=login(OLD);
            submit(session,new ChangePasswordRequest(OLD,NEW,NEW)).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_UNAVAILABLE"));
            assertThat(hash()).isEqualTo(originalHash);
            assertThat(session.isInvalid()).isFalse();
        } finally {
            jdbc.execute("drop trigger test_password_write on app.user_account");
            jdbc.execute("drop function app.test_reject_password_write()");
        }
    }

    @Test void accountVersionRejectsStaleProfileSaveAfterPasswordChange() {
        var stale = accounts.findById(id).orElseThrow();
        service.changeOwnPassword(email,new ChangePasswordRequest(OLD,NEW,NEW));
        var transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(()->transaction.execute(status->accounts.saveAndFlush(stale)))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(encoder.matches(NEW,hash())).isTrue();
    }

    private String hash() { return jdbc.queryForObject("select password_hash from app.user_account where user_id=?",String.class,id); }
    private org.springframework.test.web.servlet.ResultActions submit(MockHttpSession session,ChangePasswordRequest body) throws Exception {
        var csrf = csrf(session);
        return mvc.perform(post("/api/v1/auth/password").session(session).cookie(csrf.cookie)
                .header("X-XSRF-TOKEN",csrf.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private MockHttpSession login(String password) throws Exception {
        return (MockHttpSession)attemptLogin(password).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private org.springframework.test.web.servlet.ResultActions attemptLogin(String password) throws Exception {
        var csrf=csrf(null);
        return mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie).header("X-XSRF-TOKEN",csrf.token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("email",email).param("password",password));
    }
    private Csrf csrf(MockHttpSession session) throws Exception {
        var request=get("/api/v1/auth/csrf"); if(session!=null)request.session(session);
        MvcResult result=mvc.perform(request).andExpect(status().isOk()).andReturn();
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"),
                json.readTree(result.getResponse().getContentAsString()).get("data").get("token").asText());
    }
    private record Csrf(Cookie cookie,String token) {}
}
