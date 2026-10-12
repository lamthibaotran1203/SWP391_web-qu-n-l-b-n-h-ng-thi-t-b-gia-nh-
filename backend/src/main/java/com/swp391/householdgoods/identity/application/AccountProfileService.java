package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.domain.AccountRole;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.application.dto.PersonalProfile;
import com.swp391.householdgoods.identity.application.dto.UpdatePersonalProfileRequest;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileService {

    private final UserAccountRepository repository;
    private final Clock clock;

    public AccountProfileService(UserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PersonalProfile ownProfile(String authenticatedEmail) {
        return projection(requireCustomer(repository.findByEmailIgnoreCase(authenticatedEmail)));
    }

    @Transactional
    public PersonalProfile updateOwnProfile(String authenticatedEmail, UpdatePersonalProfileRequest request) {
        String name = request.fullName() == null ? "" : request.fullName().strip().replaceAll("\\s+", " ");
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().strip();
        if (name.length() < 2 || name.length() > 100) {
            throw new ProfileException(HttpStatus.BAD_REQUEST, "PROFILE_NAME_INVALID", "Họ và tên cần từ 2–100 ký tự.", "fullName");
        }
        if (phone != null && !phone.matches("0[0-9]{9}")) {
            throw new ProfileException(HttpStatus.BAD_REQUEST, "PROFILE_PHONE_INVALID",
                    "Số điện thoại cần 10 chữ số, bắt đầu bằng 0.", "phone");
        }
        // The same account row is locked by UC-05, so profile saves cannot overwrite a password change.
        UserAccount account = requireCustomer(repository.findForProfileUpdate(authenticatedEmail));
        account.updateContactDetails(name, phone, clock.instant());
        repository.saveAndFlush(account);
        return projection(account);
    }

    private UserAccount requireCustomer(Optional<UserAccount> result) {
        UserAccount account = result.orElseThrow(() -> new ProfileException(HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Bạn cần đăng nhập lại.", null));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ProfileException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Bạn cần đăng nhập lại.", null);
        }
        if (account.getRole() != AccountRole.CUSTOMER) throw new AccessDeniedException("Customer profile required");
        return account;
    }

    private PersonalProfile projection(UserAccount account) {
        return new PersonalProfile(account.getEmail(), account.getFullName(), account.getPhone());
    }

    @Transactional
    public AuthenticatedAccount updateFullName(String email, String requestedName) {
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        String fullName = requestedName.strip().replaceAll("\\s+", " ");
        UserAccount account = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        account.updateFullName(fullName, clock.instant());
        return new AuthenticatedAccount(
                account.getEmail(),
                account.getPasswordHash(),
                account.getFullName(),
                account.getRole(),
                account.getStatus());
    }
}
