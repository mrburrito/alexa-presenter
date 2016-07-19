package com.shankyank.alexa.presenter;

import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Presenter Speechlet application.
 */
public class PresenterSpeechlet implements Speechlet {
    public static final String START_PRESENTATION_INTENT = "StartPresentation";
    public static final String PRESENTATION_SLOT = "Presentation";
    public static final String LIST_PRESENTATIONS_INTENT = "ListPresentations";
    public static final String PRESENTATIONS_KEY = "presenter.presentations";

    private static final String HELP_TEXT = "you can list presentations or start a presentation. what would you like?";
    private static final String NO_PRESENTATIONS_TEXT = "no presentations are available. goodbye";

    public void onSessionStarted(SessionStartedRequest request, Session session) throws SpeechletException {
        session.setAttribute(PRESENTATIONS_KEY, loadPresentations());
    }

    public SpeechletResponse onLaunch(LaunchRequest request, Session session) throws SpeechletException {
        return getPresentationList(session).isEmpty() ?
                createEndSessionResponse(NO_PRESENTATIONS_TEXT) :
                createSimpleResponse(HELP_TEXT);
    }

    public SpeechletResponse onIntent(IntentRequest request, Session session) throws SpeechletException {
        if (getPresentationList(session).isEmpty()) {
            return createEndSessionResponse(NO_PRESENTATIONS_TEXT);
        }

        String intent = request.getIntent().getName();
        switch (intent) {
            case START_PRESENTATION_INTENT:
                Slot presentationSlot = request.getIntent().getSlot(PRESENTATION_SLOT);
                if (presentationSlot == null || presentationSlot.getValue() == null || presentationSlot.getValue().trim().isEmpty()) {
                    return createSimpleResponse(generatePresentationListText(session));
                }
                break;
            case LIST_PRESENTATIONS_INTENT:
                return createSimpleResponse(generatePresentationListText(session));
            case "AMAZON.StopIntent":
            case "AMAZON.CancelIntent":
                return createEndSessionResponse("goodbye");
        }
        return null;
    }

    public void onSessionEnded(SessionEndedRequest request, Session session) throws SpeechletException {
    }

    private static List<Presentation> loadPresentations() {
        return Arrays.asList(
                new Presentation("pikachu, i can't see you", "pikachu.pptx"),
                new Presentation("knock knock jokes for dummies", "knock_knock_for_dummies.pptx"),
                new Presentation("lambda", "lambda.pptx"));
    }

    private static List<Presentation> getPresentationList(final Session session) {
        List<Presentation> presentations = (List<Presentation>)session.getAttribute(PRESENTATIONS_KEY);
        if (presentations == null) {
            presentations = Collections.emptyList();
        }
        return presentations;
    }

    private static String generatePresentationListText(final Session session) {
        StringBuilder builder = new StringBuilder("i can start ");
        List<Presentation> presentations = getPresentationList(session);
        int count = presentations.size();
        for (int i=0; i < count; i++) {
            if (i == count-1) {
                builder.append("or ");
            }
            builder.append(presentations.get(i).getName());
            if (i < count-1) {
                builder.append("; ");
            }
        }
        builder.append(". which would you like?");
        return builder.toString();
    }

    private static SpeechletResponse createEndSessionResponse(final String text, final Object... args) {
        SpeechletResponse response = createSimpleResponse(text, args);
        response.setShouldEndSession(true);
        return response;
    }

    private static SpeechletResponse createSimpleResponse(final String text, final Object... args) {
        SpeechletResponse response = new SpeechletResponse();
        response.setOutputSpeech(getPlainTextSpeech(String.format(text, args)));
        response.setShouldEndSession(false);
        return response;
    }

    private static OutputSpeech getPlainTextSpeech(final String text) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);
        return speech;
    }
}
