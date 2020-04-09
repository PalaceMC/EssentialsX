package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import net.ess3.api.events.MuteStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.List;
import java.util.logging.Level;

import static com.earth2me.essentials.I18n.tl;

/**
 * ========= WARNING ========
 *
 * I still use the "muted" interfaces here because, for now, essentials is handling the economy.
 * And I do not want muted players being allowed to pay money to other players. If someone is
 * abusing the economy, or suspected of doing so, the first thing we do is mute them to prevent
 * them from sharing the secret or moving the money to another account.
 *
 * I would really like to have my own economy implementation, but that might not be entirely
 * necessary. So for now, the mute functionality stays here and is only triggered by OUR mute
 * command, to prevent the pay command from being used while muted.
 *
 */

@SuppressWarnings("unused")
public class Commandmute extends EssentialsCommand {
    public Commandmute() {
        super("mute");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        boolean nomatch = false;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        User user;
        try {
            user = getPlayer(server, args, 0, true, true);
        } catch (PlayerNotFoundException e) {
            nomatch = true;
            user = ess.getUser(Bukkit.getPlayer(args[0]));
        }
        if (!user.getBase().isOnline()) {
            if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.mute.offline")) {
                throw new Exception(tl("muteExemptOffline"));
            }
        } else {
            if (user.isAuthorized("essentials.mute.exempt") && sender.isPlayer()) {
                throw new Exception(tl("muteExempt"));
            }
        }
        String muteReason = "";

        if (args.length > 1)
            muteReason = args[1];
        
        final boolean willMute = (args.length > 1) || !user.getMuted();
        final User controller = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        final MuteStatusChangeEvent event = new MuteStatusChangeEvent(user, controller, willMute);
        ess.getServer().getPluginManager().callEvent(event);
        
        if (!event.isCancelled()) {
            if (args.length > 1) {
                user.setMuteReason(muteReason.isEmpty() ? null : muteReason);
                user.setMuted(true);
            } else {
                user.setMuted(!user.getMuted());
                if (!user.getMuted()) {
                    user.setMuteReason(null);
                }
            }
            final boolean muted = user.getMuted();

            if (nomatch) {
                sender.sendMessage(tl("userUnknown", user.getName()));
            }

            if (muted) {

                if (!user.hasMuteReason()) {
                    sender.sendMessage(tl("mutedPlayer", user.getDisplayName()));
                    user.sendMessage(tl("playerMuted"));
                } else {
                    sender.sendMessage(tl("mutedPlayerReason", user.getDisplayName(), user.getMuteReason()));
                    user.sendMessage(tl("playerMutedReason", user.getMuteReason()));
                }

                final String message;
                if (!user.hasMuteReason()) {
                    message = tl("muteNotify", sender.getSender().getName(), user.getName());
                } else {
                    message = tl("muteNotifyReason", sender.getSender().getName(), user.getName(), user.getMuteReason());
                }
                server.getLogger().log(Level.INFO, message);
                ess.broadcastMessage("essentials.mute.notify", message);
            } else {
                sender.sendMessage(tl("unmutedPlayer", user.getDisplayName()));
                user.sendMessage(tl("playerUnmuted"));
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return COMMON_DATE_DIFFS; // Date diff can span multiple words
        }
    }
}
