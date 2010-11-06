/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.openttdcoop.dev.grapes.config.ConfigSection;

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
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        ConfigSection config = irc.channels.get(channel);

        if (!config.fetch("ignorechar").isEmpty() && message.startsWith(config.fetch("ignorechar"))) {
            return;
        }

        /* TODO: create commands (using a tokenizer?) */
        if (message.startsWith(irc.getConfig().fetch("irc.cmdchar"))) {
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
}
