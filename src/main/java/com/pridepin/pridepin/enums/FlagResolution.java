package com.pridepin.pridepin.enums;

/** Admin action taken when resolving a flag. */
public enum FlagResolution {
    /** Close the flag without changing the location. */
    DISMISS,
    /** Close the flag and soft-delete the location. */
    DEACTIVATE
}
