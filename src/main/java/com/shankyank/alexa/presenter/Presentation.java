package com.shankyank.alexa.presenter;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Domain object representing a presentation.
 */
public class Presentation {
    /** The name of the presentation. */
    private final String name;
    /** The SSML pronunication markup for the presentation. */
    private final String ssml;
    /** The filename of the presentation. */
    private final String filename;

    public Presentation(String name, String filename) {
        this(name, filename, name);
    }

    @JsonCreator
    Presentation(final String name, final String filename, final String ssml) {
        this.name = name;
        this.filename = filename;
        if (ssml == null || ssml.trim().isEmpty()) {
            this.ssml = name;
        } else {
            this.ssml = ssml;
        }
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getSsml() { return ssml; }

    @Override
    public String toString() {
        return "Presentation{" +
                "name='" + name + '\'' +
                ", ssml='" + ssml + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Presentation that = (Presentation) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (ssml != null ? !ssml.equals(that.ssml) : that.ssml != null) return false;
        return filename != null ? filename.equals(that.filename) : that.filename == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ssml != null ? ssml.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        return result;
    }
}
