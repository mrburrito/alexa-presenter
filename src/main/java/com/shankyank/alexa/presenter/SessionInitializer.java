package com.shankyank.alexa.presenter;

import java.util.List;

/**
 * Provides configuration values for a Presenter session.
 */
interface SessionInitializer {
    /**
     * @return the list of available presentations
     */
    List<Presentation> getAvailablePresentations();

    /**
     * @return the presentation starter
     */
    PresentationStarter getPresentationStarter();
}
