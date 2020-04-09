package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

@SuppressWarnings("unused")
public class Commandworkbench extends EssentialsCommand {
    public Commandworkbench() {
        super("workbench");
    }


    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) {
        user.getBase().openWorkbench(null, true);
    }
}