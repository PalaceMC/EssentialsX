package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;

import static com.earth2me.essentials.I18n.tl;

/**
 * For personal preference reasons, no one is exempt from being ignored. Additionally, mods+ are not allowed to
 * ignore anyone for any reason.
 *
 * Technically, WE (palace) handle ignores, but just to keep Essentials "in the loop," we manually call these
 * ignore interfaces. Until such time that we replace all functionality in Essentials which uses it's own
 * ignoring stuff, this has to stay.
 */

public class Commandignore extends EssentialsCommand {
    public Commandignore() {
        super("ignore");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            StringBuilder sb = new StringBuilder();
            for (String s : user._getIgnoredPlayers()) {
                sb.append(s).append(" ");
            }
            String ignoredList = sb.toString().trim();
            user.sendMessage(ignoredList.length() > 0 ? tl("ignoredList", ignoredList) : tl("noIgnored"));
        } else {
            User player;
            try {
                player = getPlayer(server, args, 0, true, true);
            } catch (PlayerNotFoundException ex) {
                player = ess.getOfflineUser(args[0]);
            }
            if (player == null) {
                throw new PlayerNotFoundException();
            }
            if (player.isIgnoreExempt()) {
                user.sendMessage(tl("ignoreExempt"));
            } else if (user.isIgnoredPlayer(player)) {
                user.setIgnoredPlayer(player, false);
                user.sendMessage(tl("unignorePlayer", player.getName()));
            } else {
                user.setIgnoredPlayer(player, true);
                user.sendMessage(tl("ignorePlayer", player.getName()));
            }
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
