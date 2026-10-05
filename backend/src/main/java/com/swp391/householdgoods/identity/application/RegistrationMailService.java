package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.config.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegistrationMailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public RegistrationMailService(
            JavaMailSender mailSender,
            MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public void sendVerificationEmail(
            String email,
            String fullName,
            String verificationUrl) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailProperties.fromEmail());
        message.setTo(email);
        message.setSubject("Xác minh tài khoản Gia Dụng Việt");

        message.setText(
                "Xin chào " + fullName + ",\n\n"
                        + "Cảm ơn bạn đã đăng ký tài khoản.\n\n"
                        + "Vui lòng xác minh email bằng đường dẫn sau:\n"
                        + verificationUrl
                        + "\n\n"
                        + "Đường dẫn xác minh có hiệu lực trong 24 giờ.\n\n"
                        + mailProperties.fromName());

        mailSender.send(message);
    }
}