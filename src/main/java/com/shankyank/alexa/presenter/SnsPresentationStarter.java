package com.shankyank.alexa.presenter;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presentation starter that publishes the matched presentation to an SNS
 * topic.
 */
public class SnsPresentationStarter implements PresentationStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsPresentationStarter.class);
    private static final String DEFAULT_SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:616223318980:bti-presenter";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String topicArn;

    public SnsPresentationStarter() {
        this(DEFAULT_SNS_TOPIC_ARN);
    }

    public SnsPresentationStarter(String topicArn) {
        this.topicArn = topicArn.trim();
    }

    @Override
    public boolean startPresentation(final Session session, final MatchedPresentation presentation) {
        String message;
        try {
            message = JSON.writeValueAsString(presentation);
        } catch (JsonProcessingException jpe) {
            LOGGER.error("[{}] Error serializing presentation {}", session.getSessionId(), presentation, jpe);
            return false;
        }
        try {
            PublishResult result = new AmazonSNSClient().publish(topicArn, message);
            LOGGER.debug("[{}] Published message [{}] '{}' to topic {}.", session.getSessionId(),
                    result.getMessageId(), message, topicArn);
            return true;
        } catch (AmazonSNSException ase) {
            LOGGER.error("[{}] Unable to publish start message '{}' to SNS topic {}.",
                    session.getSessionId(), message, topicArn, ase);
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s {topic=%s}", SnsPresentationStarter.class.getSimpleName(), topicArn);
    }
}
