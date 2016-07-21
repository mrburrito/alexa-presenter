package com.shankyank.alexa.presenter;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.shankyank.alexa.presenter.Presentation.LIST_OF_PRESENTATIONS;

/**
 * The Presenter Speechlet application.
 */
public class PresenterSpeechlet implements Speechlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(PresenterSpeechlet.class);

    public static final String START_PRESENTATION_INTENT = "StartPresentation";
    public static final String PRESENTATION_SLOT = "Presentation";
    public static final String LIST_PRESENTATIONS_INTENT = "ListPresentations";
    public static final String PRESENTATION_KEY = "presenter.selectedPresentation";
    public static final String PRESENTATIONS_KEY = "presenter.presentations";
    public static final String STARTER_KEY = "presenter.presentationStarter";

    private static final String HELP_TEXT =
            "<s>you can list presentations or start a presentation</s><s>what would you like</s>";
    private static final String NO_PRESENTATIONS_TEXT = "<s>no presentations are available</s><s>goodbye</s>";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SessionInitializer sessionInitializer;

    PresenterSpeechlet() {
        this(new SessionInitializer() {
            @Override
            public List<Presentation> getAvailablePresentations() {
                return Arrays.asList(new Presentation("pikachu i can't see you", "pikachu.pptx"),
                        new Presentation("knock knock jokes for dummies", "knock_knock_for_dummies.pptx"),
                        new Presentation("lambda", "lambda.pptx"));
            }

            @Override
            public PresentationStarter getPresentationStarter() {
                return (session, presentation) -> true;
            }
        });
    }

    public PresenterSpeechlet(final SessionInitializer sessionInitializer) {
        this.sessionInitializer = sessionInitializer;
    }

    public void onSessionStarted(SessionStartedRequest request, Session session) throws SpeechletException {
        List<Presentation> presentations = sessionInitializer.getAvailablePresentations();
        session.setAttribute(PRESENTATIONS_KEY, toJson(presentations));
        LOGGER.debug("[{}]: presentations={}", session.getSessionId(), presentations);
    }

    public SpeechletResponse onLaunch(LaunchRequest request, Session session) throws SpeechletException {
        return hasPresentations(session) ?
                createContinueSessionResponse(HELP_TEXT) :
                createEndSessionResponse(NO_PRESENTATIONS_TEXT);
    }

    public SpeechletResponse onIntent(IntentRequest request, Session session) throws SpeechletException {
        if (!hasPresentations(session)) {
            return createEndSessionResponse(NO_PRESENTATIONS_TEXT);
        }

        Intent intent = request.getIntent();
        LOGGER.debug("[{}] Handling intent: {}", session.getSessionId(), intent.getName());
        switch (intent.getName()) {
            case START_PRESENTATION_INTENT:
                return onStartPresentation(intent, session);
            case LIST_PRESENTATIONS_INTENT:
                return listPresentations(session);
            case "AMAZON.YesIntent":
                return startPresentation(session);
            case "AMAZON.NoIntent":
                session.removeAttribute(PRESENTATION_KEY);
                return listPresentations(session);
            case "AMAZON.StopIntent":
            case "AMAZON.CancelIntent":
                return createEndSessionResponse("goodbye");
        }
        return null;
    }

    public void onSessionEnded(SessionEndedRequest request, Session session) throws SpeechletException {
        session.removeAttribute(PRESENTATION_KEY);
    }

    private static boolean hasPresentations(final Session session) throws SpeechletException {
        return !getPresentationList(session).isEmpty();
    }

    private static boolean isSlotEmpty(final Slot slot) {
        return slot == null || slot.getValue() == null || slot.getValue().trim().isEmpty();
    }

    private SpeechletResponse onStartPresentation(final Intent intent, final Session session) throws SpeechletException {
        SpeechletResponse response;
        Slot presentationSlot = intent.getSlot(PRESENTATION_SLOT);
        if (isSlotEmpty(presentationSlot)) {
            LOGGER.debug("[{}] {} Slot was not provided. Prompting for presentation name.", session.getSessionId(),
                    PRESENTATION_SLOT);
            response = listPresentations(session);
        } else {
            MatchedPresentation matched = matchPresentation(presentationSlot, session);
            LOGGER.debug("[{}] Found match: {}", session.getSessionId(), matched);
            if (matched.getConfidence() >= 0.85) {
                LOGGER.debug("[{}] Match confidence {} >= 0.85; starting presentation", session.getSessionId(),
                        matched.getConfidence());
                session.setAttribute(PRESENTATION_KEY, toJson(matched));
                response = startPresentation(session);
            } else if (matched.getConfidence() >= 0.50) {
                LOGGER.debug("[{}] 0.85 > Match Confidence {} >= 0.5; requesting confirmation", session.getSessionId(),
                        matched.getConfidence());
                session.setAttribute(PRESENTATION_KEY, toJson(matched));
                response = createContinueSessionResponse("did you mean %s", matched.getPresentation().getSsml());
            } else {
                LOGGER.debug("[{}] Unrecognized presentation. Confidence < 0.5 for best match: {}",
                        session.getSessionId(), matched);
                response = createContinueSessionResponse("<s>i don't recognize that presentation</s>%s",
                        HELP_TEXT);
            }
        }
        return response;
    }

    private SpeechletResponse startPresentation(final Session session) throws SpeechletException {
        MatchedPresentation presentation = getSessionAttribute(session, PRESENTATION_KEY, MatchedPresentation.class);
        LOGGER.info("[{}] Starting Presentation: {}", session.getSessionId(), presentation);
        SpeechletResponse response;
        if (presentation == null) {
            response = listPresentations(session);
        } else {
            PresentationStarter starter = sessionInitializer.getPresentationStarter();
            if (starter == null) {
                throw new SpeechletException("No presentation starter configured");
            }
            if (starter.startPresentation(session, presentation)) {
                response = createEndSessionResponse("starting %s", presentation.getPresentation().getSsml());
            } else {
                response = createEndSessionResponse("<s>unable to start presentation</s><s>please try again later</s>");
            }
        }
        return response;
    }

    private static SpeechletResponse listPresentations(final Session session) throws SpeechletException {
        return createContinueSessionResponse(generatePresentationListText(session));
    }

    private static MatchedPresentation matchPresentation(final Slot slot, final Session session)
            throws SpeechletException {
        List<Presentation> presentations = getPresentationList(session);
        SortedSet<MatchedPresentation> matches = new TreeSet<>();
        String spokenName = slot.getValue();
        for (Presentation presentation : presentations) {
            String name = presentation.getName();
            double levConfidence = getLevenshteinConfidence(spokenName, name);
            double metaConfidence = getMetaphoneConfidence(spokenName, name);
            double confidence = Math.max(levConfidence, metaConfidence);
            LOGGER.debug("[{}] presentation name=\"{}\", spoken=\"{}\", levenshtein={}, metaphone={}, confidence={}",
                    session.getSessionId(), presentation.getName(), spokenName, levConfidence, metaConfidence,
                    confidence);
            matches.add(new MatchedPresentation(spokenName, confidence, presentation));
        }
        MatchedPresentation bestMatch = matches.last();
        LOGGER.debug("[{}] spoken=\"{}\", best match={}", session.getSessionId(), spokenName, bestMatch);
        return bestMatch;
    }

    private static double getLevenshteinConfidence(final String actual, final String expected) {
        int distance = StringUtils.getLevenshteinDistance(actual, expected);
        LOGGER.debug("levenshteinDistance({}, {}) = {}", actual, expected, distance);
        int nameLength = expected.length();
        return (double) (nameLength - distance) / (double) nameLength;
    }

    private static double getMetaphoneConfidence(final String actual, final String expected) {
        DoubleMetaphone metaphone = new DoubleMetaphone();
        metaphone.setMaxCodeLen(20);
        if (metaphone.isDoubleMetaphoneEqual(actual, expected)) {
            LOGGER.debug("metaphone({}, {}) matches!", actual, expected);
            return 1.0;
        }
        String actualMetaphone = metaphone.doubleMetaphone(actual);
        String expectedMetaphone = metaphone.doubleMetaphone(expected);
        LOGGER.debug("metaphone({}, {}) == '{}', '{}'", actual, expected, actualMetaphone, expectedMetaphone);
        return getLevenshteinConfidence(actualMetaphone, expectedMetaphone);
    }

    private static List<Presentation> getPresentationList(final Session session) throws SpeechletException {
        List<Presentation> presentations = getSessionAttribute(session, PRESENTATIONS_KEY, LIST_OF_PRESENTATIONS);
        return presentations != null ? presentations : Collections.EMPTY_LIST;
    }

    private static String generatePresentationListText(final Session session) throws SpeechletException {
        List<Presentation> presentations = getPresentationList(session);
        int count = presentations.size();
        StringBuilder builder = new StringBuilder(String.format("<s>i can start %d presentations</s>", count));
        for (int i = 0; i < count; i++) {
            builder.append(presentations.get(i).getSsml());
            if (i < count - 1) {
                builder.append("<break strength=\"medium\"/>");
            }
            if (i == count - 2) {
                builder.append("or ");
            }
        }
        builder.append("<s>which would you like</s>");
        return builder.toString();
    }

    private static SpeechletResponse createContinueSessionResponse(final String format, final Object... args) {
        return createResponse(String.format(format, args), false);
    }

    private static SpeechletResponse createEndSessionResponse(final String format, final Object... args) {
        return createResponse(String.format(format, args), true);
    }

    private static SpeechletResponse createResponse(final String ssml, final boolean shouldEndSession) {
        LOGGER.debug("Generating response (end session={}): {}", shouldEndSession, ssml);

        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml(String.format("<speak>%s</speak>", ssml));

        SpeechletResponse response = new SpeechletResponse();
        response.setShouldEndSession(shouldEndSession);
        response.setOutputSpeech(speech);
        return response;
    }

    private static String toJson(final Object obj) throws SpeechletException {
        try {
            return JSON.writeValueAsString(obj);
        } catch (JsonProcessingException jpe) {
            throw new SpeechletException("Serialization Error", jpe);
        }
    }

    private static <T> T getSessionAttribute(final Session session, final String key, final Class<T> type)
            throws SpeechletException {
        try {
            Object value = session.getAttribute(key);
            return value != null ? JSON.readValue(value.toString(), type) : null;
        } catch (IOException ioe) {
            throw new SpeechletException("Deserialization error.", ioe);
        }
    }

    private static <T> T getSessionAttribute(final Session session, final String key, final TypeReference<T> type)
            throws SpeechletException {
        try {
            Object value = session.getAttribute(key);
            return value != null ? JSON.readValue(value.toString(), type) : null;
        } catch (IOException ioe) {
            throw new SpeechletException("Deserialization error.", ioe);
        }
    }
}
