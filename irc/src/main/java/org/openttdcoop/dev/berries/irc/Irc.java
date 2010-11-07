package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openttd.Client;
import org.openttd.OpenTTD;
import org.openttd.enums.DestType;
import org.openttd.enums.NetworkAction;
import org.openttd.network.Network;
import org.openttdcoop.dev.grapes.Grapes;
import org.openttdcoop.dev.grapes.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.openttdcoop.dev.grapes.spi.OpenTTDExtentions.*;

/**
 * Irc plugin to Grapes
 * @author Nathanael Rebsch
 */
public class Irc extends GrapePluginImpl
{
    IrcBot ircbot;

    protected HashMap<String, ConfigSection> channels = new HashMap<String, ConfigSection>();

    public Irc()
    {
        this.ircbot = new IrcBot(this);
    }

    @Override
    public boolean init (PluginManager pm, ConfigSection config)
    {
        this.pm     = pm;
        this.config = config;

        try {
            initConifg();
        } catch (IOException ex) {
            Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, ex.getCause().getMessage(), ex.getCause());
        }

        for (String channel : config.childrenNames()) {
            this.channels.put(channel, config.getChild(channel));
        }

        this.ircbot.init();
        return true;
    }

    private void initConifg () throws IOException
    {
        config.define("irc.host", "irc.oftc.net");
        config.define("irc.port", 6667, "");
        config.define("irc.pass", "");
        config.define("irc.nick", "Grapes");
        config.define("irc.verbose", false);
        config.define("irc.cmdchar", "!");
        config.define("nickserv.command", "", "e.g. /msg nickserv identify ${irc.nick} ${nickserv.password}");
        config.define("nickserv.password", "");

        /* only create an example if no other definition is present */
        if (config.childrenNames().length == 0) {
            ConfigSection example = config.addChild("#example");

            example.define("password", "", "some channels (mode +k) require a password or 'key' to join");
            example.define("autojoin", false, "join this channel automatically");
            example.define("chat.bridge", true, "enable the chat bridge between IRC and OpenTTD");
            example.define("chat.cmdchar", "", "require a command char in order to bridge chat to OpenTTD\nleave blank to distribute all chat");
            example.define("console.bridge", false, "bridge the server console");
            example.define("console.debug", false, "bridge also debug messages");
            example.define("rcon.enabled", false);
            example.define("rcon.oponly", false);
            example.define("rcon.channel", false, "if this is an rcon channel, every thing becomes an rcon command");
            example.define("ignorechar", "", "lines prefixed with this char will be ignored by everything this bot does\nleave empty to disable");
        }

        config.store();
    }

    @Protocol
    public Boolean connectToIrc()
    {
        this.ircbot.connect();

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("autojoin", boolean.class)) {
                this.ircbot.joinChannel(channel.getSimpleName(), channel.fetch("password"));
            }
        }

        return Boolean.TRUE;
    }

    @Chat
    public Boolean bridgeGameIrc (NetworkAction action, DestType desttype, Client client, String message, BigInteger data)
    {
        if (action != NetworkAction.NETWORK_ACTION_CHAT && desttype != DestType.DESTTYPE_BROADCAST) {
            return Boolean.TRUE;
        }

        String msg = msg = String.format("<%s> %s", client.name, message);

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("chat.bridge", boolean.class)) {
                this.ircbot.sendMessage(channel.getSimpleName(), msg);
            }
        }

        return Boolean.TRUE;
    }

    @Console
    public Boolean consoleChannels (String origin, String message)
    {
        String str = String.format("[%s]", origin);
        str        = String.format("%-10s %s\n", str, message);

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("console.bridge", boolean.class)) {
                if (origin.endsWith("console") || channel.fetch("console.debug", boolean.class)) {
                    this.ircbot.sendMessage("#openttd.test", str);
                }
            }
        }

        return Boolean.TRUE;
    }

    public void bridgeIrcGame (String message) throws IOException
    {
            Grapes gr = this.pm.getGrapes();
            Network net = gr.getNetwork();
            net.chatPublic(message);
            System.out.println("message sent?");
    }
}
