package com.shankyank.alexa.presenter;

import com.amazon.speech.speechlet.SpeechletException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.shankyank.alexa.presenter.Presentation.LIST_OF_PRESENTATIONS;

/**
 * Session initializer that reads its configuration from S3.
 */
public class S3SessionInitializer implements SessionInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3SessionInitializer.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String BUCKET = "bti-presenter";
    private static final String PRESENTATIONS_OBJECT_KEY = "config/presentations.json";
    private static final String TOPIC_OBJECT_KEY = "config/topic.txt";

    @Override
    public List<Presentation> getAvailablePresentations() throws SpeechletException {
        try (InputStream is = openS3Stream(BUCKET, PRESENTATIONS_OBJECT_KEY)) {
            List<Presentation> presentations = JSON.readValue(is, LIST_OF_PRESENTATIONS);
            LOGGER.debug("Loaded presentations: {}", presentations);
            return presentations;
        } catch (IOException | AmazonS3Exception ex) {
            String msg = String.format("Unable to load presentations from s3://%s/%s",
                    BUCKET, PRESENTATIONS_OBJECT_KEY);
            LOGGER.error(msg, ex);
            throw new SpeechletException(msg, ex);
        }
    }

    @Override
    public PresentationStarter getPresentationStarter() throws SpeechletException {
        try (InputStream is = openS3Stream(BUCKET, TOPIC_OBJECT_KEY)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read_count = is.read(buffer); read_count > 0; read_count = is.read(buffer)) {
                baos.write(buffer, 0, read_count);
            }
            String topic = baos.toString("UTF-8");
            LOGGER.debug("Creating SnsPresentationStarter for Topic: {}", topic);
            return new SnsPresentationStarter(topic);
        } catch (IOException | AmazonS3Exception ex) {
            String msg = String.format("Unable to load topic from s3://%s/%s",
                    BUCKET, TOPIC_OBJECT_KEY);
            LOGGER.error(msg, ex);
            throw new SpeechletException(msg, ex);
        }
    }

    private InputStream openS3Stream(final String bucket, final String key) {
        return new AmazonS3Client().getObject(bucket, key).getObjectContent();
    }
}
