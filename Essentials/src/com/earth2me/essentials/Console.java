package com.earth2me.essentials;

import com.earth2me.essentials.messaging.IMessageRecipient;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;


public final class Console implements IMessageRecipient {
    public static final String NAME = "Console";
    private static Console instance; // Set in essentials
    
    private final IEssentials ess;

    public static Console getInstance() {
        return instance;
    }

    static void setInstance(IEssentials ess) { // Called in Essentials#onEnable()
        instance = new Console(ess);
    }
    
    /**
     * @deprecated Use {@link Console#getCommandSender()}
     */
    @Deprecated
    public static CommandSender getCommandSender(Server server) throws Exception {
        return server.getConsoleSender();
    }

    private Console(IEssentials ess) {
        this.ess = ess;
    }

    public CommandSender getCommandSender() {
        return ess.getServer().getConsoleSender();
    }

    @Override public String getName() {
        return Console.NAME;
    }

    @Override public void sendMessage(String message) {
        getCommandSender().sendMessage(message);
    }

    @Override public boolean isReachable() {
        return true;
    }
}
