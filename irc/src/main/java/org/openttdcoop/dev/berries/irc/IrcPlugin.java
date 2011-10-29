package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import org.openttd.Client;
import org.openttd.enums.DestType;
import org.openttd.enums.NetworkAction;
import org.openttd.network.Protocol;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDChat;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDConsole;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDProtocol;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.pircbotx.exception.IrcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Irc plugin to Grapes
 * @author Nathanael Rebsch
 */
public class IrcPlugin extends GrapePluginImpl implements OpenTTDProtocol, OpenTTDChat, OpenTTDConsole
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    IrcBot ircbot;

    protected HashMap<String, ConfigSection> channels = new HashMap<String, ConfigSection>();
    
    private final Logger log = LoggerFactory.getLogger(IrcPlugin.class);

    public IrcPlugin()
    {
        this.ircbot = new IrcBot(this);
    }

    @Override
    public boolean init ()
    {
        try {
            initConfig();
        } catch (IOException ex) {
            log.error(ex.getCause().getMessage(), ex.getCause());
        }

        for (String channel : config.childrenNames()) {
            this.channels.put(channel, config.getChild(channel));
        }

        return true;
    }

    private void initConfig () throws IOException
    {
        config.define("irc.host", "irc.oftc.net");
        config.define("irc.port", 6667, "");
        config.define("irc.pass", "");
        config.define("irc.nick", "DrGrapes");
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
            example.define("ignorechar", "", "lines prefixed with this char will be ignored by everything this bot does\nleave empty to disable");
        }

        config.store();
    }

    @Override
    public void onOpenTTDProtocol(Protocol protocol)
    {
        try {
            //this.ircbot.connect();
            this.ircbot.bot.setVerbose(config.fetch("irc.verbose", Boolean.class));
            this.ircbot.bot.setName(config.fetch("irc.nick"));
            this.ircbot.bot.setLogin("grapes");
            this.ircbot.bot.setVersion("Grapes IRC Plugin");
            this.ircbot.bot.connect(config.fetch("irc.host"), config.fetch("irc.port", Integer.class));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } catch (IrcException ex) {
            log.error(ex.getMessage(), ex);
        }

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("autojoin", boolean.class)) {
                this.ircbot.bot.joinChannel(channel.getSimpleName(), channel.fetch("password"));
            }
        }
    }

    @Override
    public void onOpenTTDChat(NetworkAction action, DestType desttype, Client client, String message, BigInteger data)
    {
        if (desttype != DestType.DESTTYPE_BROADCAST) {
            return;
        }

        switch (action) {
            case NETWORK_ACTION_CHAT_CLIENT:
            case NETWORK_ACTION_CHAT_COMPANY:
                return;
        }

        String msg = msg = String.format("<%s> %s", client.name, message);

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("chat.bridge", Boolean.class)) {
                this.ircbot.bot.sendMessage(channel.getSimpleName(), msg);
            }
        }
    }

    @Override
    public void onOpenTTDConsole(String origin, String message)
    {
        String str = String.format("[%s]", origin);
        str        = String.format("%-10s %s\n", str, message);

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("console.bridge", Boolean.class)) {
                if (origin.endsWith("console") || channel.fetch("console.debug", boolean.class)) {
                    this.ircbot.bot.sendMessage(channel.getSimpleName(), str);
                }
            }
        }
    }
}
