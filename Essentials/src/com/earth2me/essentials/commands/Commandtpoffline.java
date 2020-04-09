package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent;

import static com.earth2me.essentials.I18n.tl;

@SuppressWarnings("unused")
public class Commandtpoffline extends EssentialsCommand {
    public Commandtpoffline() {
        super("tpoffline");
    }

    @Override
    public void run(final Server server, final User user, final String label, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new NotEnoughArgumentsException();
        } else {
            final User target = getPlayer(server, args, 0, true, true);
            final Location logout = target.getLogoutLocation();

            String worldName;
            if (logout.getWorld() == null)
                worldName = "null";
            else
                worldName = logout.getWorld().getName();

            if (user.getWorld() != logout.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + worldName)) {
                throw new Exception(tl("noPerm", "essentials.worlds." + worldName));
            }

            user.sendMessage(tl("teleporting", worldName, logout.getBlockX(), logout.getBlockY(), logout.getBlockZ()));
            user.getTeleport().now(logout, false, PlayerTeleportEvent.TeleportCause.COMMAND);
        }
    }
}

