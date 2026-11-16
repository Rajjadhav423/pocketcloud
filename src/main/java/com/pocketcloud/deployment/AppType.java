package com.pocketcloud.deployment;

public enum AppType {
    /** Plain files (HTML/CSS/JS) served directly by the router. */
    STATIC,
    /** A long-running Node.js server process. */
    NODE
}
