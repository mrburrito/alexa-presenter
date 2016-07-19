package com.shankyank.alexa.presenter.intents;

import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for presenter supportedIntents handlers.
 */
abstract class AbstractIntentHandler {
    /** The supportedIntents handled by this handler. */
    private final Set<String> supportedIntents;

    protected AbstractIntentHandler(final String... supportedIntents) {
        Set<String> intents = new HashSet<>(Arrays.asList(supportedIntents));
        this.supportedIntents = Collections.unmodifiableSet(intents);
    }

    public final SpeechletResponse handleIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        if (!supportedIntents.contains(request.getIntent().getName())) {
            throw new SpeechletException(String.format("%s invoked for intent %s. Supported intents are %s",
                    getClass().getSimpleName(), request.getIntent().getName(), supportedIntents));
        }
        return execute(request, session);
    }

    protected abstract SpeechletResponse execute(final IntentRequest request, final Session session)
            throws SpeechletException;

    public final Set<String> getSupportedIntents() {
        return supportedIntents;
    }
}
