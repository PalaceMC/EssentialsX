package com.earth2me.essentials.messaging;

/**
 * Represents an interface for message recipients.
 */
public interface IMessageRecipient {

    /**
     * Sends (prints) a message to this recipient.
     *
     * @param message message
     */
    void sendMessage(String message);

    /**
     * Returns the name of this recipient. This name is typically used internally to identify this recipient.
     *
     * @return name of this recipient
     */
    String getName();

    /**
     * Returns whether this recipient is reachable. A case where the recipient is not reachable is if they are offline.
     *
     * @return whether this recipient is reachable
     */
    boolean isReachable();
}
