package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

import static com.earth2me.essentials.I18n.tl;

@SuppressWarnings("unused")
public class Commandpaytoggle extends EssentialsCommand {
    public Commandpaytoggle() {
        super("paytoggle");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) {
        boolean acceptingPay = !user.isAcceptingPay();
        if (commandLabel.contains("payon")) {
            acceptingPay = true;
        } else if (commandLabel.contains("payoff")) {
            acceptingPay = false;
        }
        user.setAcceptingPay(acceptingPay);
        if (acceptingPay) {
            user.sendMessage(tl("payToggleOn"));
        } else {
            user.sendMessage(tl("payToggleOff"));
        }
    }
}
