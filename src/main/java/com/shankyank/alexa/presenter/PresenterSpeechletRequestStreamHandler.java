package com.shankyank.alexa.presenter;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

import java.util.Collections;
import java.util.Set;

/**
 * Lambda handler for Presenter application.
 */
public final class PresenterSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
    /** The Alexa application ID for the Presenter application. */
    private static final String PRESENTER_APPLICATION_ID = "";
    /** The set of application IDs that are valid for this service. */
    private static final Set<String> SUPPORTED_APPLICATION_IDS = Collections.emptySet();

    public PresenterSpeechletRequestStreamHandler() {
        super(new PresenterSpeechlet(), SUPPORTED_APPLICATION_IDS);
    }
}
