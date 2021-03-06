package net.ess3.api.events;

import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the player teleports
 */
public class UserTeleportEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final IUser user;
    private final TeleportCause cause;
    private final Location target;
    private boolean cancelled = false;

    public UserTeleportEvent(IUser user, TeleportCause cause, Location target) {
        super(!Bukkit.isPrimaryThread());
        this.user = user;
        this.cause = cause;
        this.target = target;
    }

    public IUser getUser() {
        return user;
    }

    public TeleportCause getTeleportCause() {
        return cause;
    }

    public Location getLocation() {
        return target;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
