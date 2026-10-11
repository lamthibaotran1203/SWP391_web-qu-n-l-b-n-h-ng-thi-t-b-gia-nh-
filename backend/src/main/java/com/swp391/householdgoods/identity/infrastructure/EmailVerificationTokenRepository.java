package com.swp391.householdgoods.identity.infrastructure;

import com.swp391.householdgoods.identity.domain.EmailVerificationToken;
import com.swp391.householdgoods.identity.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(
            String tokenHash);

    List<EmailVerificationToken> findAllByUserAndUsedAtIsNull(
            UserAccount user);
}