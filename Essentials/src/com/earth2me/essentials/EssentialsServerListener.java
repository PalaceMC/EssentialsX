package com.earth2me.essentials;

import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


public class EssentialsServerListener implements Listener {
    private static final List<String> ignoredSLPECallers = Arrays.asList(
        ".LegacyPingHandler.channelRead(", // CB responding to pings from pre-Netty clients
        "de.dytanic.cloudnet.bridge.BukkitBootstrap", // CloudNet v2 doing... something
        "de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin" // CloudNet v3 doing... something else
    );

    private final transient IEssentials ess;
    private boolean unsupportedLogged = false;

    public EssentialsServerListener(final IEssentials ess) {
        this.ess = ess;

        ess.getLogger().info("ServerListPingEvent: Spigot iterator API");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerListPing(final ServerListPingEvent event) {
        try {
            Iterator<Player> iterator = event.iterator();
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (ess.getUser(player).isVanished()) {
                    iterator.remove();
                }
            }
        } catch (UnsupportedOperationException e) {
            if (!unsupportedLogged && shouldWarnSLPECaller(e)) {
                ess.getLogger().log(Level.WARNING, "Could not hide vanished players while handling " + event.getClass().getName(), e);
                unsupportedLogged = true;
            }
        }
    }

    /**
     * Should we warn about this SLPE caller, or should we silently ignore it?
     * This checks against the ignoredSLPECallers strings, and if it matches one of those, we
     * return false.
     *
     * @param throwable A throwable caught by a catch block
     * @return Whether or not to send a warning about this particular caller
     */
    private boolean shouldWarnSLPECaller(Throwable throwable) {
        final int maxStackDepth = 20; // Limit the depth when searching through the stack trace
        int depth = 0;
        for (StackTraceElement element : throwable.getStackTrace()) {
            depth++;
            if (depth > maxStackDepth) {
                break;
            }

            for (String ignoredString : ignoredSLPECallers) {
                if (element.toString().contains(ignoredString)) {
                    return false; // We know about this error and should ignore it, so don't warn
                }
            }
        }

        return true; // We don't know for certain that we can ignore this, so warn just to be safe
    }
}
