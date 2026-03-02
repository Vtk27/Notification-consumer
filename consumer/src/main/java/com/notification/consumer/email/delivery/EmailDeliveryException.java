package com.notification.consumer.email.delivery;

import com.notification.consumer.logging.Logger;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
        Logger.logError(getClass(), message, cause);
    }

    public EmailDeliveryException(String message) {
        super(message);
        Logger.logError(getClass(), message, getCause());
    }
}
