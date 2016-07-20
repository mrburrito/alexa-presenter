package com.shankyank.alexa.presenter

import spock.lang.Specification

class S3SessionInitializerSpec extends Specification {
    S3SessionInitializer instance = new S3SessionInitializer()

    def 'can load presentations'() {
        expect:
        instance.availablePresentations
    }

    def 'can configure presentation starter'() {
        expect:
        instance.presentationStarter
    }
}
