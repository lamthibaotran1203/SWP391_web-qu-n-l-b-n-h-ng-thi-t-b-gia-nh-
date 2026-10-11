package com.swp391.householdgoods.identity.infrastructure;

import com.swp391.householdgoods.identity.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from UserAccount a where lower(a.email)=lower(:email)")
    Optional<UserAccount> findForProfileUpdate(
            @org.springframework.data.repository.query.Param("email") String email);

    // PostgreSQL locks this account until the surrounding service transaction finishes.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from UserAccount a where lower(a.email)=lower(:email)")
    Optional<UserAccount> findForPasswordChange(
            @org.springframework.data.repository.query.Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);
}
