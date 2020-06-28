package com.earth2me.essentials.commands;

import com.earth2me.essentials.AsyncTeleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.events.TPARequestEvent;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.earth2me.essentials.I18n.tl;

@SuppressWarnings("unused")
public class Commandtpa extends EssentialsCommand {
    public Commandtpa() {
        super("tpa");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        User player = getPlayer(server, user, args, 0);
        if (user.getName().equalsIgnoreCase(player.getName())) {
            throw new NotEnoughArgumentsException();
        }
        if (!player.isAuthorized("essentials.tpaccept")) {
            throw new Exception(tl("teleportNoAcceptPermission", player.getDisplayName()));
        }
        if (!player.isTeleportEnabled()) {
            throw new Exception(tl("teleportDisabled", player.getDisplayName()));
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + player.getWorld().getName())) {
            throw new Exception(tl("noPerm", "essentials.worlds." + player.getWorld().getName()));
        }
        // Don't let sender request teleport twice to the same player.
        if (user.getConfigUUID().equals(player.getTeleportRequest()) && player.hasOutstandingTeleportRequest() // Check timeout
            && !player.isTpRequestHere()) { // Make sure the last teleport request was actually tpa and not tpahere
            throw new Exception(tl("requestSentAlready", player.getDisplayName()));
        }
        if (player.isAutoTeleportEnabled() && !player.isIgnoredPlayer(user)) {
            final Trade charge = new Trade(this.getName(), ess);
            AsyncTeleport teleport = (AsyncTeleport) user.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
            teleport.teleport(player.getBase(), charge, PlayerTeleportEvent.TeleportCause.COMMAND, future);
            future.thenAccept(success -> {
                if (success) {
                    player.sendMessage(tl("requestAcceptedAuto", user.getDisplayName()));
                    user.sendMessage(tl("requestAcceptedFromAuto", player.getDisplayName()));
                }
            });
            return;
        }

        if (!player.isIgnoredPlayer(user)) {
            TPARequestEvent tpaEvent = new TPARequestEvent(user.getSource(), player, false);
            ess.getServer().getPluginManager().callEvent(tpaEvent);
            if (tpaEvent.isCancelled()) {
                throw new Exception(tl("teleportRequestCancelled", player.getDisplayName()));
            }
            player.requestTeleport(user, false);
            player.sendMessage(tl("teleportRequest", user.getDisplayName()));
            player.sendMessage(tl("typeTpaccept"));
            player.sendMessage(tl("typeTpdeny"));
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendMessage(tl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation()));
            }
        }
        user.sendMessage(tl("requestSent", player.getDisplayName()));
        if (user.isAuthorized("essentials.tpacancel")) {
            user.sendMessage(tl("typeTpacancel"));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
