package com.shankyank.alexa.presenter;

import com.amazon.speech.speechlet.Session;

import java.io.Serializable;

/**
 * Triggers the start of a presentation.
 */
interface PresentationStarter extends Serializable {
    /**
     * Start the presentation.
     * @param session the speechlet session
     * @param presentation the matched presentation
     * @return true if the presentation was successfully started
     */
    boolean startPresentation(final Session session, final MatchedPresentation presentation);
}
