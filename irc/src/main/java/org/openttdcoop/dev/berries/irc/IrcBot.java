/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openttdcoop.dev.berries.irc;

import org.openttdcoop.dev.grapes.enums.CommandAccess;
import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.OpenTTDExtentions.IrcCommand;

/**
 *
 * @author nathanael
 */
public class IrcBot extends PircBot
{
    protected Irc irc;

    public IrcBot (Irc irc)
    {
        this.irc = irc;
    }

    public void init ()
    {
        this.setName(irc.getConfig().fetch("irc.nick"));
        this.setLogin("Grapes IRC Berry");
        this.setVersion("0.1");
        this.setVerbose(irc.getConfig().fetch("irc.verbose", boolean.class));
    }

    public void connect ()
    {
        try {
            this.connect(irc.getConfig().fetch("irc.host"), irc.getConfig().fetch("irc.port", int.class));
        } catch (IOException ex) {
            Logger.getLogger(IrcBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IrcException ex) {
            Logger.getLogger(IrcBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
	if (message.startsWith(irc.getConfig().fetch("irc.cmdchar")))
	{
	    EnumSet access = EnumSet.noneOf(CommandAccess.class);
	    access.add(CommandAccess.Private);
	    boolean isOp = false;
	    boolean rcon = false;
	    for (String channel : irc.channels.keySet())
	    {
		ConfigSection config = irc.channels.get(channel);
		boolean channelop = isOp(channel, sender);
		if (channelop) isOp = true;
		if (config.fetch("rcon.enabled", boolean.class) && (isOp || !config.fetch("rcon.oponly", boolean.class))) rcon = true;
	    }
	    if (isOp) access.add(CommandAccess.Op);
	    if (rcon) access.add(CommandAccess.Rcon);
	    this.irc.getPluginManager().invoke(IrcCommand.class, message.substring(1), null, sender, access);
	    return;
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        ConfigSection config = irc.channels.get(channel);

        if (!config.fetch("ignorechar").isEmpty() && message.startsWith(config.fetch("ignorechar"))) {
            return;
        }

        if (message.startsWith(irc.getConfig().fetch("irc.cmdchar"))) 
	{
	    EnumSet access = EnumSet.noneOf(CommandAccess.class);
	    access.add(CommandAccess.Channel);
	    boolean isOp = isOp(channel, sender);
	    if (isOp) access.add(CommandAccess.Op);
	    if (config.fetch("rcon.enabled", boolean.class) && (isOp || !config.fetch("rcon.oponly", boolean.class))) access.add(CommandAccess.Rcon);
	    this.irc.getPluginManager().invoke(IrcCommand.class, message.substring(1), channel, sender, access);
	    return;
        }

        if (config.fetch("chat.bridge", boolean.class)) {
            if (config.fetch("chat.cmdchar").isEmpty() || message.startsWith(config.fetch("chat.cmdchar"))) {
                try {
                    this.irc.bridgeIrcGame(String.format("<%s> %s", sender, message));
                } catch (IOException ex) {
                    Logger.getLogger(IrcBot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public boolean isOp(String channel, String nick)
    {
	User[] users = this.getUsers(channel);
	for (User user : users)
	{
	    if (user.getNick().equals(nick))
	    {
		return user.isOp();
	    }
	}

	return true;
    }
}
