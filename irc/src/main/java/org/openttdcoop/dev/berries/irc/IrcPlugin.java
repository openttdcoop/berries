package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.util.HashMap;
import org.openttd.RconBuffer;
import org.openttd.StringFunc;
import org.openttd.enums.DestType;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDChat;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDChatEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientError;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientErrorEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientJoin;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientJoinEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientQuit;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientQuitEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDConsole;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDConsoleEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDProtocol;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDProtocolEvent;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDRcon;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDRconEvent;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.security.SecurityLevel;
import org.openttdcoop.dev.grapes.spi.*;
import org.pircbotx.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Irc plugin to Grapes
 *
 * @author Nathanael Rebsch
 */
@Rename("IRC")
public class IrcPlugin extends GrapePluginImpl implements OpenTTDProtocol, OpenTTDChat, OpenTTDConsole, OpenTTDClientJoin, OpenTTDClientQuit, OpenTTDClientError, OpenTTDRcon
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    IrcBot ircbot;
    protected HashMap<String, ConfigSection> channels = new HashMap<String, ConfigSection>();
    private final Logger log = LoggerFactory.getLogger(IrcPlugin.class);

    public IrcPlugin ()
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
        config.define("irc.port", 6667);
        config.define("irc.pass", "");
        config.define("irc.nick", "DrGrapes");
        config.define("irc.verbose", false);
        config.define("irc.cmdchar", "!");
        config.define("nickserv.command", "", "e.g. /msg nickserv identify ${irc.nick} ${nickserv.password}");
        config.define("nickserv.password", "");
        config.define("security.map.op", SecurityLevel.ADMIN);
        config.define("security.map.voice", SecurityLevel.PRIVILEGED);

        /* only create an example if no other definition is present */
        if (config.childrenNames().length == 0) {
            ConfigSection example = config.addChild("#example");
            this.configSetDefaultsifMissing(example);
        } else {
            for (String c : config.childrenNames()) {
                this.configSetDefaultsifMissing(config.getChild(c));
            }
        }

        config.store();
    }

    private void configSetDefaultsifMissing (ConfigSection cs)
    {
        cs.define("password", "", "some channels (mode +k) require a password or 'key' to join");
        cs.define("autojoin", false, "join this channel automatically");
        cs.define("chat.bridge", true, "enable the chat bridge between IRC and OpenTTD");
        cs.define("chat.cmdchar", "", "require a command char in order to bridge chat to OpenTTD\nleave blank to distribute all chat");
        cs.define("console.bridge", false, "bridge the server console");
        cs.define("console.debug", false, "bridge also debug messages");
        cs.define("rcon.enabled", false);
        cs.define("rcon.oponly", false);
        cs.define("ignorechar", "", "lines prefixed with this char will be ignored by everything this bot does\nleave empty to disable");
        cs.define("announcements", true, "send announcements in to irc (new client, new company, etc.)");
    }

    @Override
    public void onOpenTTDProtocol (OpenTTDProtocolEvent event)
    {
        this.ircbot.bot.setVerbose(config.fetch("irc.verbose", Boolean.class));
        this.ircbot.bot.setName(config.fetch("irc.nick"));
        this.ircbot.bot.setLogin("grapes");
        this.ircbot.bot.setVersion("Grapes IRC Plugin");
        this.ircbot.bot.setMessageDelay(300);
        this.ircbot.bot.useShutdownHook(false);

        this.ircbot.connect();
    }

    @Override
    public void onOpenTTDChat (OpenTTDChatEvent event)
    {
        if (event.desttype != DestType.DESTTYPE_BROADCAST) {
            return;
        }

        String announcement = null;

        switch (event.action) {
            case NETWORK_ACTION_CHAT:
                String msg = String.format("<%s> %s", event.client.name, event.message);

                for (ConfigSection channel : channels.values()) {
                    if (channel.fetch("chat.bridge", Boolean.class)) {
                        this.ircbot.bot.sendMessage(channel.getSimpleName(), msg);
                    }
                }
                return;

            case NETWORK_ACTION_COMPANY_JOIN:
                announcement = String.format("%s as joined company #%d", highlight(event.client.name), event.data);
                break;

            case NETWORK_ACTION_COMPANY_NEW:
                announcement = String.format("%s as started a new company (#%d)", highlight(event.client.name), event.data);
                break;

            case NETWORK_ACTION_COMPANY_SPECTATOR:
                announcement = String.format("%s has joined spectators", highlight(event.client.name));
                break;

            case NETWORK_ACTION_GIVE_MONEY:
                announcement = String.format("%s '%d'", event.message, event.data);
                break;

            default:
                return;
        }

        this.announce(announcement);
    }

    @Override
    public void onOpenTTDConsole (OpenTTDConsoleEvent event)
    {
        String str = String.format("[%s]", event.origin);
        str = String.format("%-10s %s\n", str, event.message);

        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("console.bridge", Boolean.class)) {
                if (event.origin.endsWith("console") || channel.fetch("console.debug", boolean.class)) {
                    this.ircbot.bot.sendMessage(channel.getSimpleName(), str);
                }
            }
        }
    }

    @Override
    public void onOpenTTDRcon (OpenTTDRconEvent event)
    {
        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("console.bridge", Boolean.class)) {
                for (RconBuffer.Entry rconEntry : event.rconBuffer) {
                    this.ircbot.bot.sendMessage(channel.getSimpleName(), StringFunc.stripColour(rconEntry.message));
                }
            }
        }
    }

    private void announce (String announcement)
    {
        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("announcements", Boolean.class)) {
                this.ircbot.bot.sendMessage(channel.getSimpleName(), Colors.DARK_GRAY + announcement);
            }
        }
    }

    private String highlight (String str)
    {
        return Colors.BOLD + str + Colors.BOLD;
    }

    @Override
    public void onOpenTTDClientJoin (OpenTTDClientJoinEvent event)
    {
        String announcement = String.format("%s has joined the game (Client #%d)", highlight(event.client.name), event.client.id);
        this.announce(announcement);
    }

    @Override
    public void onOpenTTDClientQuit (OpenTTDClientQuitEvent event)
    {
        String announcement = String.format("%s has left the game (leaving)", highlight(event.client.name));
        this.announce(announcement);
    }

    @Override
    public void onOpenTTDClientError (OpenTTDClientErrorEvent event)
    {
        String announcement = String.format("%s has left the game (%s)", highlight(event.client.name), event.error.toReadableString());
        this.announce(announcement);
    }
}
