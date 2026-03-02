package com.notification.consumer.email.delivery;

import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.notification.consumer.config.NotificationProperties;
import com.notification.consumer.config.TemplateProperties;
import com.notification.consumer.event.NotificationEvent;
import com.notification.consumer.logging.Logger;

import reactor.core.publisher.Mono;

@Component// this annotation is used for spring to detect this class and create a bean for it in the application context and create object. This allows us to inject this class into other classes using @Autowired or constructor injection.
public class EmailDeliveryClient {

    private final WebClient.Builder webClientBuilder;
    private final NotificationProperties notificationProperties; // used to get email delivery mode and third party configuration
    private final TemplateProperties templateProperties; // used to get sendername and footer for email
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties; // used to get SMTP configuration when delivery mode is SMTP (spring.mail.host, spring.mail.port)

    public EmailDeliveryClient(WebClient.Builder webClientBuilder, NotificationProperties notificationProperties,
            TemplateProperties templateProperties, JavaMailSender mailSender, MailProperties mailProperties) {
        this.webClientBuilder = webClientBuilder;
        this.notificationProperties = notificationProperties;
        this.templateProperties = templateProperties;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public void sendEmail(NotificationEvent event, String htmlBody) {
        String mode = notificationProperties.getEmail().getDeliveryMode();
        if (!StringUtils.hasText(mode)) {
            mode = "sendgrid";
        }
        mode = mode.toLowerCase(Locale.ROOT).trim();

        if ("smtp".equals(mode)) {
            sendEmailSmtp(event, htmlBody);
            return;
        }
        if (!"sendgrid".equals(mode)) {
            throw new IllegalStateException("notification.email.delivery-mode must be one of: sendgrid, smtp");
        }

        sendEmailSendGrid(event, htmlBody);
    }

    private void sendEmailSendGrid(NotificationEvent event, String htmlBody) {
        NotificationProperties.ThirdPartySettings thirdParty = notificationProperties.getThirdParty();
        validateThirdPartyConfiguration(thirdParty);
        WebClient webClient = webClientBuilder.baseUrl(thirdParty.getBaseUrl()).build();

        String subject = "Notification: " + event.getEventType();
        SendGridMailSendRequest request = new SendGridMailSendRequest(//this is sendgrid mail send request payload which we will send to sendgrid API to send email. We are using record classes to create immutable data structures for the request payload.
                List.of(new Personalization(
                        List.of(new EmailAddress(event.getUserEmail(), null)),
                        Map.of(
                                "notificationId", String.valueOf(event.getNotificationId()),
                                "referenceId", String.valueOf(event.getReferenceId())))),
                new EmailAddress(thirdParty.getFromEmail(), templateProperties.getSenderName()),
                subject,
                List.of(new Content("text/html", htmlBody)));

        Logger.logInfo(EmailDeliveryClient.class, "Sending email via SendGrid (from={}, to={}, notificationId={}, referenceId={})",
                thirdParty.getFromEmail(), event.getUserEmail(), event.getNotificationId(), event.getReferenceId());
        try {
            webClient.post()
                    .uri(thirdParty.getSendPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + thirdParty.getApiKey())
                    .bodyValue(request)
                    .exchangeToMono(response -> {
                        HttpStatusCode status = response.statusCode();
                        Logger.logInfo(getClass(), "SendGrid Response Status: {}", status);

                        if (status.isError()) {
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        Logger.logError(getClass(), "SendGrid Error Body: {}", body);
                                        return Mono.error(new EmailDeliveryException("third-party failure: " + body));
                                    });
                        }

                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(body -> Logger.logInfo(getClass(), "SendGrid Success Body: {}", body))
                                .then();})
                    .block(Duration.ofSeconds(10));
        } catch (RuntimeException ex) {
            throw new EmailDeliveryException(
                    "Failed to send email (to=" + request.personalizations().get(0).to().get(0).email() + ", EmailSubject=" + request.subject() + ")",
                    ex);
        }
    }

    private void sendEmailSmtp(NotificationEvent event, String htmlBody) {
        String fromEmail = notificationProperties.getThirdParty().getFromEmail();
        validateSmtpConfiguration(fromEmail);

        String subject = "Notification: " + event.getEventType();
        Logger.logInfo(EmailDeliveryClient.class, "Sending email via SMTP (host={}, from={}, to={}, notificationId={}, referenceId={})",
                mailProperties.getHost(), fromEmail, event.getUserEmail(), event.getNotificationId(), event.getReferenceId());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8"); // we need to specify UTF-8 encoding to support special characters in the email body. By default, it uses the platform default encoding which may not be UTF-8 and can cause issues with special characters.
            helper.setFrom(fromEmail, templateProperties.getSenderName());
            helper.setTo(event.getUserEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException ex) {
            throw new EmailDeliveryException(
                    "Failed to send email via SMTP (to=" + event.getUserEmail() + ", EmailSubject=" + subject + ")",
                    ex);
        } catch (Exception e){
            Logger.logError(EmailDeliveryClient.class, subject, e);
        }
    }

    private void validateSmtpConfiguration(String fromEmail) {
        if (!StringUtils.hasText(mailProperties.getHost())) {
            throw new IllegalStateException("spring.mail.host must be set when notification.email.delivery-mode=smtp");
        }
        if (mailProperties.getPort() == null || mailProperties.getPort() <= 0) {
            throw new IllegalStateException("spring.mail.port must be set when notification.email.delivery-mode=smtp");
        }
        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalStateException("notification.third-party.from-email must be set when notification.email.delivery-mode=smtp");
        }
    }

    private static void validateThirdPartyConfiguration(NotificationProperties.ThirdPartySettings thirdParty) {
        if (!StringUtils.hasText(thirdParty.getBaseUrl())) {
            throw new IllegalStateException("notification.third-party.base-url must be set");
        }
        if (!StringUtils.hasText(thirdParty.getSendPath())) {
            throw new IllegalStateException("notification.third-party.send-path must be set");
        }
        if (!StringUtils.hasText(thirdParty.getApiKey())) {
            throw new IllegalStateException("notification.third-party.api-key must be set");
        }
        if (!StringUtils.hasText(thirdParty.getFromEmail())) {
            throw new IllegalStateException("notification.third-party.from-email must be set");
        }
    }

    /**
     * SendGrid v3 /mail/send payload.
     * See: https://docs.sendgrid.com/api-reference/mail-send/mail-send
     */
    private static record SendGridMailSendRequest(List<Personalization> personalizations, EmailAddress from, String subject,
            List<Content> content) {
    }

    private static record Personalization(List<EmailAddress> to, Map<String, String> custom_args) {
    }

    private static record EmailAddress(String email, String name) {
    }

    private static record Content(String type, String value) {
    }
}
