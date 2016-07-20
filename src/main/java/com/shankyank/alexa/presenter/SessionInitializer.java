package com.shankyank.alexa.presenter;

import com.amazon.speech.speechlet.SpeechletException;

import java.util.List;

/**
 * Provides configuration values for a Presenter session.
 */
interface SessionInitializer {
    /**
     * @return the list of available presentations
     * @throws SpeechletException if the presentations cannot be retrieved
     */
    List<Presentation> getAvailablePresentations() throws SpeechletException;

    /**
     * @return the presentation starter
     * @throws SpeechletException if the starter cannot be configured
     */
    PresentationStarter getPresentationStarter() throws SpeechletException;
}
