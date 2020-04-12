package net.ess3.api.events;

import net.ess3.api.IUser;


// LMAO someone finally figured it out haha...

/**
 * This event is currently unused, and is retained for ABI compatibility and potential future implementation.
 */
@SuppressWarnings("unused")
public class IgnoreStatusChangeEvent extends StatusChangeEvent {
    public IgnoreStatusChangeEvent(IUser affected, IUser controller, boolean value) {
        super(affected, controller, value);
    }
}
