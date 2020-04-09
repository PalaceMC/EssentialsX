package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.TextInput;
import com.earth2me.essentials.textreader.TextPager;
import org.bukkit.Server;

@SuppressWarnings("unused")
public class Commandrules extends EssentialsCommand {
    public Commandrules() {
        super("rules");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final IText input = new TextInput(sender, "rules", true, ess);
        final IText output = new KeywordReplacer(input, sender, ess);
        final TextPager pager = new TextPager(output);
        pager.showPage(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null, commandLabel, sender);
    }
}
