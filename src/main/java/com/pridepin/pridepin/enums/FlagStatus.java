package com.pridepin.pridepin.enums;

/** Lifecycle state of a location flag. */
public enum FlagStatus {
    /** Awaiting admin review. */
    OPEN,
    /** Admin reviewed and dismissed — no action taken on the location. */
    DISMISSED,
    /** Admin reviewed and acted — location has been deactivated. */
    ACTIONED
}
