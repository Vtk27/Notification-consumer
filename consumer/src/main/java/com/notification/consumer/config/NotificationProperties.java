package com.notification.consumer.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final KafkaSettings kafka = new KafkaSettings();
    private final EmailSettings email = new EmailSettings();
    private final ThirdPartySettings thirdParty = new ThirdPartySettings();

    public KafkaSettings getKafka() {
        return kafka;
    }

    public EmailSettings getEmail() {
        return email;
    }

    public ThirdPartySettings getThirdParty() {
        return thirdParty;
    }

    public static class EmailSettings {
        /**
         * Supported values: sendgrid, smtp
         */
        private String deliveryMode;

        public String getDeliveryMode() {
            return deliveryMode;
        }

        public void setDeliveryMode(String deliveryMode) {
            this.deliveryMode = deliveryMode;
        }
    }

    public static class KafkaSettings {
        private String bootstrapServers;
        private String groupId;
        private String mainTopic;
        private String dlqTopic;

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getMainTopic() {
            return mainTopic;
        }

        public void setMainTopic(String mainTopic) {
            this.mainTopic = mainTopic;
        }

        public String getDlqTopic() {
            return dlqTopic;
        }

        public void setDlqTopic(String dlqTopic) {
            this.dlqTopic = dlqTopic;
        }
    }

    public static class ThirdPartySettings {
        private String baseUrl;
        private String sendPath;
        private String apiKey;
        private String fromEmail;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSendPath() {
            return sendPath;
        }

        public void setSendPath(String sendPath) {
            this.sendPath = sendPath;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }
    }

}
