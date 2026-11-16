package com.pocketcloud.build;

/** An expected build failure (bad repo, failing npm script…). The message is shown to the user. */
public class BuildException extends RuntimeException {

    public BuildException(String message) {
        super(message);
    }
}
