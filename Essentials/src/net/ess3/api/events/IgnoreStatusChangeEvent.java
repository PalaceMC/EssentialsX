package net.ess3.api.events;

import net.ess3.api.IUser;

// Some genius never implemented this event into the api, lol.
// What? Hell no, I'm not implementing it... but I'll keep it here in case.

@SuppressWarnings("unused")
public class IgnoreStatusChangeEvent extends StatusChangeEvent {
    public IgnoreStatusChangeEvent(IUser affected, IUser controller, boolean value) {
        super(affected, controller, value);
    }
}
