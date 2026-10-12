package com.swp391.householdgoods.identity.api;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.identity.application.AccountProfileService;
import com.swp391.householdgoods.identity.application.ProfileException;
import com.swp391.householdgoods.identity.application.dto.PersonalProfile;
import com.swp391.householdgoods.identity.application.dto.UpdatePersonalProfileRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account/profile")
@PreAuthorize("hasRole('CUSTOMER')")
public class PersonalProfileController {
    private final AccountProfileService service;

    public PersonalProfileController(AccountProfileService service) { this.service = service; }

    @GetMapping
    public ApiEnvelope<PersonalProfile> read(Authentication authentication) {
        try {
            return new ApiEnvelope<>(service.ownProfile(authentication.getName()));
        } catch (DataAccessException | TransactionException failure) {
            throw unavailable("Chưa thể tải thông tin tài khoản. Vui lòng thử lại.");
        }
    }

    @PutMapping
    public ApiEnvelope<PersonalProfile> update(Authentication authentication,
            @Valid @RequestBody UpdatePersonalProfileRequest request) {
        try {
            return new ApiEnvelope<>(service.updateOwnProfile(authentication.getName(), request));
        } catch (DataAccessException | TransactionException failure) {
            // Database errors may contain contact data/hash; do not log their raw message.
            throw unavailable("Chưa thể lưu thông tin tài khoản. Vui lòng thử lại.");
        }
    }

    private ProfileException unavailable(String message) {
        return new ProfileException(HttpStatus.SERVICE_UNAVAILABLE, "PROFILE_UNAVAILABLE", message, null);
    }
}
