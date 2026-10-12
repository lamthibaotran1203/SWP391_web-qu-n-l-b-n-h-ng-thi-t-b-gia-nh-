package com.swp391.householdgoods.identity.application;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalProfileIntegrationTest {
    private static final String API = "/api/v1/account/profile";
    private static final String PASSWORD = "known test password";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private String email;
    private String otherEmail;
    private String hash;
    private long id;

    @BeforeEach void createCustomers() {
        email = "profile-" + UUID.randomUUID() + "@example.invalid";
        otherEmail = "other-" + email;
        hash = encoder.encode(PASSWORD);
        id = insert(email);
        insert(otherEmail);
    }
    private long insert(String address) {
        return jdbc.queryForObject("insert into app.user_account(email,password_hash,full_name,phone,role,status,"
                + "email_verified_at,created_at,updated_at) values (?,?,'Khách kiểm thử','0901234567','CUSTOMER',"
                + "'ACTIVE',now(),now(),now()) returning user_id", Long.class, address, hash);
    }
    @AfterEach void clean() { jdbc.update("delete from app.user_account where email in (?,?)", email, otherEmail); }

    @Test void readsOnlyOwnSafeProfileEvenWhenAnotherEmailIsSupplied() throws Exception {
        mvc.perform(get(API).session(login()).param("email", otherEmail)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.phone").value("0901234567"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test void savesNormalizedContactsOnlyAndRetainsTheSession() throws Exception {
        var session = login();
        write(session, Map.of("fullName", "  Nguyễn   An ", "phone", " 0912345678 ",
                "email", otherEmail, "userId", id + 1, "role", "MANAGER", "passwordHash", "fake"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Nguyễn An"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"));
        mvc.perform(get(API).session(session)).andExpect(jsonPath("$.data.phone").value("0912345678"));
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(jsonPath("$.data.fullName").value("Nguyễn An"));
        assertThat(jdbc.queryForObject("select password_hash from app.user_account where user_id=?", String.class, id)).isEqualTo(hash);
        assertThat(jdbc.queryForObject("select role from app.user_account where user_id=?", String.class, id)).isEqualTo("CUSTOMER");
        assertThat(jdbc.queryForObject("select full_name from app.user_account where email=?", String.class, otherEmail)).isEqualTo("Khách kiểm thử");
        assertThat(jdbc.queryForObject("select count(*) from app.hm_address where user_id=?", Long.class, id)).isZero();
    }

    @Test void permitsClearingTheOptionalPhone() throws Exception {
        write(login(), Map.of("fullName", "Nguyễn An", "phone", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phone").isEmpty());
        assertThat(jdbc.queryForObject("select phone from app.user_account where user_id=?", String.class, id)).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"12345", "+84912345678", "09123abcde"})
    void rejectsInvalidPhoneWithoutSavingAnyField(String phone) throws Exception {
        write(login(), Map.of("fullName", "Tên chưa được lưu", "phone", phone))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.phone").exists());
        assertUnchanged();
    }

    @ParameterizedTest @ValueSource(strings = {" ", "A"})
    void rejectsInvalidNameWithoutSavingPhone(String name) throws Exception {
        write(login(), Map.of("fullName", name, "phone", "0912345678"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.fullName").exists());
        assertUnchanged();
    }

    @ParameterizedTest @ValueSource(strings = {"STAFF", "MANAGER"})
    void deniesOtherRolesAtTheApi(String role) throws Exception {
        jdbc.update("update app.user_account set role=? where user_id=?", role, id);
        var session = login();
        mvc.perform(get(API).session(session)).andExpect(status().isForbidden());
        write(session, Map.of("fullName", "Tên mới", "phone", "0912345678")).andExpect(status().isForbidden());
        assertUnchanged();
    }

    @Test void requiresAuthenticationCsrfAndACurrentRole() throws Exception {
        mvc.perform(get(API)).andExpect(status().isUnauthorized());
        write(null, Map.of("fullName", "Tên mới")).andExpect(status().isUnauthorized());
        var session = login();
        mvc.perform(put(API).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Tên mới\"}")).andExpect(status().isForbidden());
        jdbc.update("update app.user_account set role='STAFF' where user_id=?", id);
        mvc.perform(get(API).session(session)).andExpect(status().isUnauthorized());
        assertUnchanged();
    }

    @Test void storageFailureRollsBackAndReturnsASafeError() throws Exception {
        var session = login();
        String name = "reject_profile_" + id;
        jdbc.execute("create function app." + name + "() returns trigger language plpgsql as $$ begin "
                + "raise exception 'PRIVATE_CONTACT_OR_HASH'; end $$");
        jdbc.execute("create trigger " + name + " before update on app.user_account for each row "
                + "when (NEW.user_id=" + id + ") execute function app." + name + "()");
        try {
            var response = write(session, Map.of("fullName", "Tên chưa lưu", "phone", "0912345678"))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PROFILE_UNAVAILABLE"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("PRIVATE_CONTACT_OR_HASH", hash);
            assertUnchanged();
        } finally {
            jdbc.execute("drop trigger " + name + " on app.user_account");
            jdbc.execute("drop function app." + name + "()");
        }
    }

    private void assertUnchanged() {
        assertThat(jdbc.queryForObject("select full_name from app.user_account where user_id=?", String.class, id)).isEqualTo("Khách kiểm thử");
        assertThat(jdbc.queryForObject("select phone from app.user_account where user_id=?", String.class, id)).isEqualTo("0901234567");
    }
    private MockHttpSession login() throws Exception {
        var csrf = csrf(null);
        return (MockHttpSession) mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()).param("email", email).param("password", PASSWORD))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private ResultActions write(MockHttpSession session, Map<String, ?> body) throws Exception {
        var csrf = csrf(session);
        var request = put(API).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (session != null) request.session(session);
        return mvc.perform(request);
    }
    private Csrf csrf(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andReturn();
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"), json.readTree(result.getResponse().getContentAsString()).at("/data/token").asText());
    }
    private record Csrf(Cookie cookie, String token) {}
}
