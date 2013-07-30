package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import org.openttd.Client;
import org.openttd.RconBuffer;
import org.openttd.StringFunc;
import org.openttd.enums.DestType;
import org.openttd.enums.NetworkAction;
import org.openttd.enums.NetworkErrorCode;
import org.openttd.network.Protocol;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDChat;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientError;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientJoin;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDClientQuit;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDConsole;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDProtocol;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDRcon;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.pircbotx.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Irc plugin to Grapes
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
            this.configSetDefaultsifMissing(example);
        } else {
            for (String c : config.childrenNames()) {
                this.configSetDefaultsifMissing(config.getChild(c));
            }
        }

        config.store();
    }

    private void configSetDefaultsifMissing(ConfigSection cs)
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
    public void onOpenTTDProtocol(Protocol protocol)
    {
        this.ircbot.bot.setVerbose(config.fetch("irc.verbose", Boolean.class));
        this.ircbot.bot.setName(config.fetch("irc.nick"));
        this.ircbot.bot.setLogin("grapes");
        this.ircbot.bot.setVersion("Grapes IRC Plugin");
        
        this.ircbot.connect();
    }

    @Override
    public void onOpenTTDChat(NetworkAction action, DestType desttype, Client client, String message, BigInteger data)
    {
        if (desttype != DestType.DESTTYPE_BROADCAST) {
            return;
        }

        String announcement = null;

        switch (action) {
            case NETWORK_ACTION_CHAT:
                String msg = msg = String.format("<%s> %s", client.name, message);

                for (ConfigSection channel : channels.values()) {
                    if (channel.fetch("chat.bridge", Boolean.class)) {
                        this.ircbot.bot.sendMessage(channel.getSimpleName(), msg);
                    }
                }
                return;

            case NETWORK_ACTION_COMPANY_JOIN:
                announcement = String.format("%s as joined company #%d", highlight(client.name), data);
                break;

            case NETWORK_ACTION_COMPANY_NEW:
                announcement = String.format("%s as started a new company (#%d)", highlight(client.name), data);
                break;

            case NETWORK_ACTION_COMPANY_SPECTATOR:
                announcement = String.format("%s has joined spectators", highlight(client.name));
                break;

            case NETWORK_ACTION_GIVE_MONEY:
                announcement = String.format("%s '%d'", message, data);
                break;

            default:
                return;
        }

        this.announce(announcement);
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

    @Override
    public void onOpenTTDRcon(RconBuffer rconBuffer)
    {
        StringBuilder sb = new StringBuilder();
        
        for (RconBuffer.Entry rconEntry : rconBuffer) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(rconEntry);
        }
        
        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("console.bridge", Boolean.class)) {
                this.ircbot.bot.sendMessage(channel.getSimpleName(), StringFunc.stripColour(sb.toString()));
            }
        }
    }

    private void announce(String announcement)
    {
        for (ConfigSection channel : channels.values()) {
            if (channel.fetch("announcements", Boolean.class)) {
                this.ircbot.bot.sendMessage(channel.getSimpleName(), Colors.DARK_GRAY + announcement);
            }
        }
    }
    
    private String highlight(String str)
    {
        return Colors.BOLD + str + Colors.BOLD;
    }
    
    @Override
    public void onOpenTTDClientJoin(Client client)
    {
        String announcement = String.format("%s has joined the game (Client #%d)", highlight(client.name), client.id);
        this.announce(announcement);
    }

    @Override
    public void onOpenTTDClientQuit(Client client)
    {
        String announcement = String.format("%s has left the game (leaving)", highlight(client.name));
        this.announce(announcement);
    }

    @Override
    public void onOpenTTDClientError(Client client, NetworkErrorCode error)
    {
        String announcement = String.format("%s has left the game (%s)", highlight(client.name), error.toReadableString());
        this.announce(announcement);
    }
}
