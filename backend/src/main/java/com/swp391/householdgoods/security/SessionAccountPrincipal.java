package com.swp391.householdgoods.security;

import com.swp391.householdgoods.identity.application.AuthenticatedAccount;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.domain.AccountRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** The password is erased by Spring after login; only an opaque revocation stamp remains. */
public final class SessionAccountPrincipal extends User {
    private final String credentialStamp;

    public SessionAccountPrincipal(AuthenticatedAccount account) {
        super(account.email(), account.passwordHash() == null ? "" : account.passwordHash(),
                account.status() == AccountStatus.ACTIVE, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())));
        credentialStamp = stamp(account.passwordHash());
    }

    public boolean hasCurrentCredential(String passwordHash) {
        return MessageDigest.isEqual(credentialStamp.getBytes(StandardCharsets.US_ASCII),
                stamp(passwordHash).getBytes(StandardCharsets.US_ASCII));
    }

    public boolean hasCurrentRole(AccountRole role) {
        return getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private static String stamp(String passwordHash) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((passwordHash == null ? "" : passwordHash).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
