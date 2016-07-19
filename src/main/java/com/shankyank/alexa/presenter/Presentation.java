package com.shankyank.alexa.presenter;

/**
 * Domain object representing a presentation.
 */
public class Presentation {
    /** The name of the presentation. */
    private final String name;
    /** The filename of the presentation. */
    private final String filename;

    public Presentation(String name, String filename) {
        this.name = name;
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }
}
