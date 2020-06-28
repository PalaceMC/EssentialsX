package com.earth2me.essentials.api;

import net.ess3.api.IUser;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IJails extends IReload {
    /**
     * Gets the location of the jail with the given name
     *
     * @param jailName The name of the jail
     *
     * @return the location of the jail, null if it doesn't exist
     */
    @Nullable
    Location getJail(String jailName);

    /**
     * Gets a list of jails by names
     *
     * @return a list of jails, if there are none the list will be empty
     */
    Collection<String> getList();

    /**
     * Gets the number of jails
     *
     * @return the size of the list of jails
     */
    int getCount();

    /**
     * Remove the jail with the given name
     *
     * @param jail the jail to remove
     */
    void removeJail(String jail);

    /**
     * Attempts to send the given user to the given jail
     *
     * @deprecated Use {@link IJails#sendToJail(IUser, String, CompletableFuture)}
     *
     * @param user the user to send to jail
     * @param jail the jail to send the user to
     */
    @Deprecated
    void sendToJail(IUser user, String jail);

    /**
     * Attempts to send the given user to the given jail
     *
     * @param user            the user to send to jail
     * @param jail            the jail to send the user to
     * @param future          Future which is completed with the success status of the execution
     */
    void sendToJail(IUser user, String jail, CompletableFuture<Boolean> future);

    /**
     * Set a new jail with the given name and location
     *
     * @param jailName the name of the jail being set
     * @param loc      the location of the jail being set
     */
    void setJail(String jailName, Location loc);
}
