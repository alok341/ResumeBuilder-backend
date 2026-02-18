package com.alok.resumebuilder.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;


    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("Inside EmailService : sendHtmlEmail() to {}, {},{}", to, subject, htmlContent);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to {}", to);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}", to, ex);
            // DO NOT crash business logic
        }
    }
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, byte[] attachmentData, String attachmentFilename) {
        log.info("Inside EmailService : sendEmailWithAttachment() to {}, {}, {}", to, subject, htmlContent);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));

            mailSender.send(message);
            log.info("Email with attachment sent to {}", to);

        } catch (MessagingException  ex) {
            log.error("Failed to send email with attachment to {}", to, ex);
            // DO NOT crash business logic
        }
    }
}
