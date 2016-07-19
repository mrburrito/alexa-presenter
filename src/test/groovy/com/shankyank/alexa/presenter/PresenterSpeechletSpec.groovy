package com.shankyank.alexa.presenter

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest
import com.amazon.speech.speechlet.Session
import com.amazon.speech.speechlet.SessionStartedRequest
import spock.lang.Specification

import static com.shankyank.alexa.presenter.PresenterSpeechlet.LIST_PRESENTATIONS_INTENT
import static com.shankyank.alexa.presenter.PresenterSpeechlet.START_PRESENTATION_INTENT
import static com.shankyank.alexa.presenter.PresenterSpeechlet.PRESENTATION_SLOT
import static com.shankyank.alexa.presenter.PresenterSpeechlet.PRESENTATIONS_KEY

/**
 * Tests for the PresenterSpeechlet.
 */
class PresenterSpeechletSpec extends Specification {
    private static final String TEST_SESSION_ID = 'test-session-id'

    private static final List<Presentation> TEST_PRESENTATIONS = [
            new Presentation('Situation FooBar', 'foobar.pptx'),
            new Presentation('Enterprise FizzBuzz', 'fizzbuzz.pptx'),
            new Presentation('Lambda: Where did all the servers go?', 'lambda.pptx')
    ]

    PresenterSpeechlet instance = new PresenterSpeechlet()
    Session session

    def setup() {
        session = Session.builder().withSessionId(TEST_SESSION_ID).build()
        session.setAttribute(PRESENTATIONS_KEY, TEST_PRESENTATIONS)
    }

    private static IntentRequest createStartRequest(final String presentationName=null) {
        Map slots = [:]
        if (presentationName) {
            slots[PRESENTATION_SLOT] = Slot.builder().withName(PRESENTATION_SLOT).withValue(presentationName).build()
        }
        Intent intent = Intent.builder().withName(START_PRESENTATION_INTENT).withSlots(slots).build()
        return IntentRequest.builder().withRequestId("test-id").withTimestamp(new Date()).withIntent(intent).build()
    }

    private static String getPresentationListString() {
        String listString = TEST_PRESENTATIONS.collect { it.name }.subList(0, TEST_PRESENTATIONS.size()-1).join('; ')
        if (TEST_PRESENTATIONS.size() > 1) {
            listString += "; or ${TEST_PRESENTATIONS.last().name}"
        }
        return listString
    }

    def 'available presentations are loaded when session starts'() {
        given:
        SessionStartedRequest request = SessionStartedRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()

        when:
        instance.onSessionStarted(request, session)

        then:
        !session.getAttribute(PRESENTATIONS_KEY).empty
    }

    def 'help message returned when launched with no intent'() {
        given:
        LaunchRequest request = LaunchRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()

        when:
        def result = instance.onLaunch(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.text == 'you can list presentations or start a presentation. what would you like?'
    }

    def 'user is informed by launch event if no presentations are available and session is terminated'() {
        given:
        LaunchRequest request = LaunchRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).build()
        session.setAttribute(PRESENTATIONS_KEY, [])

        when:
        def result = instance.onLaunch(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == 'no presentations are available. goodbye'
    }

    def 'user is informed by start intent if no presentations are available and session is terminated'() {
        given:
        IntentRequest request = createStartRequest()
        session.setAttribute(PRESENTATIONS_KEY, [])

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == 'no presentations are available. goodbye'
    }

    def 'user is informed by list intent if no presentations are available and session is terminated'() {
        given:
        IntentRequest request = IntentRequest.builder().withRequestId("test-id").withTimestamp(new Date()).
                withIntent(Intent.builder().withName(LIST_PRESENTATIONS_INTENT).build()).build()
        session.setAttribute(PRESENTATIONS_KEY, [])

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == 'no presentations are available. goodbye'
    }

    def 'user is prompted for a presentation name when start intent has no presentation slot'() {
        given:
        IntentRequest request = createStartRequest()

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.text == "i can start ${presentationListString}. which would you like?"
    }

    def 'user is notified when start intent cannot identify a requested presentation'() {
        given:
        IntentRequest request = createStartRequest("unknown presentation")

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.text == "i didn't recognize the presentation unknown presentation. " +
                "you can list presentations or start a presentation. what would you like?"
    }

    def 'recognized presentation is started by start intent'() {
        given:
        IntentRequest request = createStartRequest(TEST_PRESENTATIONS.first().name)

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == "starting presentation ${TEST_PRESENTATIONS.first().name}"
        // verify SNS message published
    }

    def 'session is terminated by cancel intent'() {
        given:
        IntentRequest request = IntentRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).
                withIntent(Intent.builder().withName("AMAZON.CancelIntent").build()).build()

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == 'goodbye'
    }

    def 'session is terminated by stop intent'() {
        given:
        IntentRequest request = IntentRequest.builder().
                withRequestId("test-id").withTimestamp(new Date()).
                withIntent(Intent.builder().withName("AMAZON.StopIntent").build()).build()

        when:
        def result = instance.onIntent(request, session)

        then:
        result.shouldEndSession
        result.outputSpeech.text == 'goodbye'
    }

    def 'list intent lists the available presentations'() {
        given:
        IntentRequest request = IntentRequest.builder().withRequestId("test-id").withTimestamp(new Date()).
                withIntent(Intent.builder().withName(LIST_PRESENTATIONS_INTENT).build()).build()

        when:
        def result = instance.onIntent(request, session)

        then:
        !result.shouldEndSession
        result.outputSpeech.text == "i can start ${presentationListString}. which would you like?"
    }
}
