package com.shankyank.alexa.presenter

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.*
import com.amazon.speech.ui.SsmlOutputSpeech
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import static com.shankyank.alexa.presenter.Presentation.LIST_OF_PRESENTATIONS
import static com.shankyank.alexa.presenter.PresenterSpeechlet.*

/**
 * Tests for the PresenterSpeechlet.
 */
class PresenterSpeechletSpec extends Specification {
    private static final String TEST_SESSION_ID = 'test-session-id'
    private static final ObjectMapper JSON = new ObjectMapper()

    private static final List<Presentation> TEST_PRESENTATIONS = [
            new Presentation('Situation FooBar', 'foobar.pptx'),
            new Presentation('Enterprise FizzBuzz', 'fizzbuzz.pptx'),
            new Presentation('Lambda: Where did all the servers go?', 'lambda.pptx')
    ]

    Session session
    PresentationStarter starter
    SessionInitializer initializer
    PresenterSpeechlet instance

    def setup() {
        starter = Mock(PresentationStarter)
        starter.startPresentation(_, _) >> true
        initializer = Mock(SessionInitializer)
        initializer.getAvailablePresentations() >> TEST_PRESENTATIONS
        initializer.getPresentationStarter() >> starter
        session = Session.builder().withSessionId(TEST_SESSION_ID).build()
        session.setAttribute(PRESENTATIONS_KEY, JSON.writeValueAsString(TEST_PRESENTATIONS))
        instance = new PresenterSpeechlet(initializer)
    }

    def 'session is initialized on startup'() {
        given:
        SessionStartedRequest request = SessionStartedRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()

        when:
        instance.onSessionStarted(request, session)

        then:
        getJsonAttribute(session, PRESENTATIONS_KEY, LIST_OF_PRESENTATIONS) == TEST_PRESENTATIONS
        !session.getAttribute(PRESENTATION_KEY)
    }

    def 'help message returned when launched with no intent'() {
        given:
        LaunchRequest request = LaunchRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()

        when:
        def result = instance.onLaunch(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.ssml == '<speak><s>you can list presentations or start a presentation</s><s>what would you like?</s></speak>'
    }

    def 'user is informed by launch event if no presentations are available and session is terminated'() {
        expect:
        session.setAttribute(PRESENTATIONS_KEY, presentations)
        LaunchRequest request = LaunchRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()
        def result = instance.onLaunch(request, session)
        result.shouldEndSession
        result.outputSpeech.ssml == '<speak><s>no presentations are available</s><s>goodbye</s></speak>'

        where:
        presentations << [null, []]
    }

    def 'user is informed by all actionable intents when no presentations are available and session is terminated'() {
        expect:
        session.setAttribute(PRESENTATIONS_KEY, presentations)
        def result = instance.onIntent(createIntentRequest(intent), session)
        result.shouldEndSession
        result.outputSpeech.ssml == '<speak><s>no presentations are available</s><s>goodbye</s></speak>'

        where:
        presentations | intent
        null          | START_PRESENTATION_INTENT
        null          | LIST_PRESENTATIONS_INTENT
        null          | 'AMAZON.YesIntent'
        null          | 'AMAZON.NoIntent'
        []            | START_PRESENTATION_INTENT
        []            | LIST_PRESENTATIONS_INTENT
        []            | 'AMAZON.YesIntent'
        []            | 'AMAZON.NoIntent'
    }

    def 'user is prompted for a presentation name when start intent has no presentation slot'() {
        given:
        IntentRequest request = createStartRequest()

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.ssml == getPresentationSsml()
    }

    def 'user is notified when start intent cannot identify a requested presentation'() {
        given:
        IntentRequest request = createStartRequest("unknown presentation")

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.ssml == "<speak><s>i don't recognize that presentation</s>" +
                "<s>you can list presentations or start a presentation</s><s>what would you like?</s></speak>"
    }

    def 'recognized presentation is started by start intent'() {
        given:
        Presentation presentation = TEST_PRESENTATIONS.first()
        IntentRequest request = createStartRequest(presentation.name)

        when:
        def result = instance.onIntent(request, session)

        then:
        1*starter.startPresentation(session, { it.presentation == presentation }) >> true
        result.shouldEndSession
        result.outputSpeech.ssml == "<speak>starting ${presentation.ssml}</speak>"
        getJsonAttribute(session, PRESENTATION_KEY, MatchedPresentation).presentation == presentation
    }

    def 'error message is generated when starting presentation fails'() {
        given:
        Presentation presentation = TEST_PRESENTATIONS.first()
        IntentRequest request = createStartRequest(presentation.name)

        when:
        def result = instance.onIntent(request, session)

        then:
        1*starter.startPresentation(session, { it.presentation == presentation }) >> false
        result.shouldEndSession
        result.outputSpeech.ssml == "<speak><s>unable to start presentation</s><s>please try again later</s></speak>"
        getJsonAttribute(session, PRESENTATION_KEY, MatchedPresentation).presentation == presentation
    }

    def 'exception thrown if no presentation starter is configured'() {
        given:
        Presentation presentation = TEST_PRESENTATIONS.first()
        IntentRequest request = createStartRequest(presentation.name)

        when:
        def result = instance.onIntent(request, session)

        then:
        initializer.getPresentationStarter() >> null
        thrown(SpeechletException)
    }

    def 'user is prompted for confirmation when recognized presentation is low confidence'() {
        expect:
        Presentation presentation = new Presentation('low confidence', 'low_confidence.pptx')
        session.setAttribute(PRESENTATIONS_KEY, JSON.writeValueAsString([presentation] + TEST_PRESENTATIONS))
        IntentRequest request = createStartRequest(name)
        def result = instance.onIntent(request, session)
        !result.shouldEndSession
        getJsonAttribute(session, PRESENTATION_KEY, MatchedPresentation)?.presentation == presentation
        result.outputSpeech.ssml == "<speak>did you mean ${presentation.ssml}</speak>"

        where:
        name << [ 'can feed', 'condense' ]
    }

    def 'presentations are listed for yes intent with no previously requested presentation'() {
        given:
        IntentRequest request = createIntentRequest('AMAZON.YesIntent')

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.ssml == getPresentationSsml()
    }

    def 'low confidence presentation is started by yes intent'() {
        given:
        MatchedPresentation presentation = TEST_PRESENTATIONS.first().with {
            new MatchedPresentation(name, 1.0d, it)
        }
        session.setAttribute(PRESENTATION_KEY, JSON.writeValueAsString(presentation))
        IntentRequest request = createIntentRequest('AMAZON.YesIntent')

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.ssml == "<speak>starting ${presentation.presentation.ssml}</speak>"
    }

    def 'presentations are listed for no event with low confidence presentation'() {
        given:
        MatchedPresentation presentation = TEST_PRESENTATIONS.first().with {
            new MatchedPresentation('nope', 0.05d, it)
        }
        session.setAttribute(PRESENTATION_KEY, JSON.writeValueAsString(presentation))
        IntentRequest request = createIntentRequest('AMAZON.NoIntent')

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.ssml == getPresentationSsml()
        !session.getAttribute(PRESENTATION_KEY)
    }

    def 'session is terminated by cancel or stop intent'() {
        expect:
        def result = instance.onIntent(createIntentRequest(intent), session)
        result.shouldEndSession
        result.outputSpeech.ssml == '<speak>goodbye</speak>'

        where:
        intent << ['AMAZON.CancelIntent', 'AMAZON.StopIntent']
    }

    def 'list intent lists the available presentations'() {
        when:
        def result = instance.onIntent(createIntentRequest(LIST_PRESENTATIONS_INTENT), session)

        then:
        !result.shouldEndSession
        result.outputSpeech instanceof SsmlOutputSpeech
        result.outputSpeech.ssml == getPresentationSsml()
    }

    private static def getJsonAttribute = { session, key, type ->
        session.getAttribute(key)?.with {
            JSON.readValue(it, type)
        }
    }

    private static IntentRequest createIntentRequest(final String intentName, final Map slots=[:]) {
        Intent intent = Intent.builder().withName(intentName).withSlots(slots).build()
        IntentRequest.builder().
                withRequestId("test-id").
                withTimestamp(new Date()).
                withIntent(intent).build()
    }

    private static IntentRequest createStartRequest(final String presentationName=null) {
        Map slots = [:]
        if (presentationName) {
            slots[PRESENTATION_SLOT] = Slot.builder().withName(PRESENTATION_SLOT).withValue(presentationName).build()
        }
        createIntentRequest(START_PRESENTATION_INTENT, slots)
    }

    private static String getPresentationSsml(final List presentations=TEST_PRESENTATIONS) {
        List<String> ssml = presentations.collect { it.ssml }
        String joinedSsml = ssml.join('<break strength="medium"/>')
        joinedSsml = joinedSsml.replace(ssml.last(), "or<break strength=\"medium\"/> ${ssml.last()}")
        """
<speak>
<s>i can start ${presentations.size()} presentations</s>
${joinedSsml}
<s>which would you like?</s>
</speak>
""".replaceAll('\n', '')
    }
}
