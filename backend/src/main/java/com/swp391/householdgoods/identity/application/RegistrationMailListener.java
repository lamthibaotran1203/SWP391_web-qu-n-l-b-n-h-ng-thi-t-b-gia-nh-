package com.swp391.householdgoods.identity.application;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationMailListener {

    private final RegistrationMailService registrationMailService;

    public RegistrationMailListener(
            RegistrationMailService registrationMailService) {

        this.registrationMailService =
                registrationMailService;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            VerificationEmailRequested event) {

        registrationMailService
                .sendVerificationEmail(
                        event.email(),
                        event.fullName(),
                        event.verificationUrl());
    }
}