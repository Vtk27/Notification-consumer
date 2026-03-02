package com.notification.consumer.email.template;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.notification.consumer.config.TemplateProperties;
import com.notification.consumer.event.NotificationEvent;

@Service
public class TemplateService {

    private final SpringTemplateEngine templateEngine;//used to render email body based on template and context
    private final TemplateProperties templateProperties;// used to get sendername and footer for email

    public TemplateService(SpringTemplateEngine templateEngine, TemplateProperties templateProperties) {// constructor injection of dependencies
        this.templateEngine = templateEngine;
        this.templateProperties = templateProperties;
    }

    public String renderBody(NotificationEvent event) {
        Context context = new Context();
        context.setVariable("customerName", event.getCustomerName());
        context.setVariable("eventType", event.getEventType());
        context.setVariable("content", event.getMetadata());
        context.setVariable("metadata", event.getMetadata());
        context.setVariable("footer", templateProperties.getFooter());
        context.setVariable("senderName", templateProperties.getSenderName());
        return templateEngine.process("email-notification", context);// load email-notification.html from resources/templates and render it with context
    }
}
