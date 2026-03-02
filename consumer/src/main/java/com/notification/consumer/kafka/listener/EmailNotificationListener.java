package com.notification.consumer.kafka.listener;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.notification.consumer.email.delivery.EmailDeliveryClient;
import com.notification.consumer.email.template.TemplateService;
import com.notification.consumer.event.NotificationEvent;
import com.notification.consumer.logging.Logger;
import com.notification.consumer.persistence.NotificationSent;
import com.notification.consumer.persistence.NotificationSentRepository;

@Component
public class EmailNotificationListener {

    private final TemplateService templateService;// This is used to render the email body using Thymeleaf templates 
    private final EmailDeliveryClient emailDeliveryClient; // This is used to send the email using either SendGrid API or SMTP based on the configuration
    private final NotificationSentRepository notificationSentRepository;

    public EmailNotificationListener(TemplateService templateService, EmailDeliveryClient emailDeliveryClient,
            NotificationSentRepository notificationSentRepository) {
        this.templateService = templateService;
        this.emailDeliveryClient = emailDeliveryClient;
        this.notificationSentRepository = notificationSentRepository;
    }

    @KafkaListener(id = "notification-email-listener", topics = "${notification.kafka.main-topic}", containerFactory = "kafkaListenerContainerFactory")// This annotation is used to mark this method as a Kafka listener method. It listens to the topic specified in the configuration (notification.kafka.main-topic) and uses the kafkaListenerContainerFactory to create the Kafka listener container. The id attribute is used to specify a unique id for this listener which can be used for monitoring and management purposes.
    public void listen(@Payload NotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        if (event.getNotificationId() != null) {
            try {
                if (notificationSentRepository.existsById(event.getNotificationId())) {
                    try{
                        notificationSentRepository.save(new NotificationSent(
                        event.getNotificationId(),
                        event.getEventType(),
                        event.getCustomerName()));
                    }catch(DataIntegrityViolationException e){
                        Logger.logInfo(EmailNotificationListener.class,
                            "Skipping notification {} (already sent, type={}, topic {}, offset={}, key={})",
                            event.getNotificationId(), event.getEventType(), topic, offset, key);
                    }
                    return;
                }
            } catch (RuntimeException ex) {
                Logger.logWarn(EmailNotificationListener.class,
                        "Audit DB unavailable; proceeding without de-duplication (notificationId={}, type={})",
                        event.getNotificationId(), event.getEventType(), ex);
            }
        }

        Logger.logInfo(EmailNotificationListener.class,
                "Consumed notification {} (type={}, topic {}, offset={}, key={})",
                event.getNotificationId(), event.getEventType(), topic,
                offset, key);
        String body = templateService.renderBody(event);//generates email body by rendering the Thymeleaf template with the data from the notification event. The template will use the event data to populate the email content dynamically based on the event type and content.
        emailDeliveryClient.sendEmail(event, body);// sends the email using the EmailDeliveryClient. The delivery client will decide whether to use SendGrid API or SMTP based on the configuration and send the email accordingly.

        try {
            if (event.getNotificationId() != null) {
                notificationSentRepository.save(new NotificationSent(
                        event.getNotificationId(),
                        event.getEventType(),
                        event.getCustomerName()));
            } else {
                Logger.logWarn(EmailNotificationListener.class,
                        "Email sent but notificationId was null; skipping DB audit insert (type={}, topic {}, offset={}, key={})",
                        event.getEventType(), topic, offset, key);
            }
        } catch (RuntimeException ex) {
            Logger.logError(EmailNotificationListener.class,
                    "Email sent but failed to persist audit row (notificationId={}, type={})",
                    event.getNotificationId(), event.getEventType(), ex);
        }

        Logger.logInfo(EmailNotificationListener.class,
                "Email sent for notification id:{}, useremail:{}, customer name:{}, event type:{}",
                event.getNotificationId(), event.getUserEmail(), event.getCustomerName(), event.getEventType());
    }
}
