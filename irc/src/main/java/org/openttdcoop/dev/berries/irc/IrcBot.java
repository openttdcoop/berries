/*
 *  Copyright (C) 2011 Nathanael Rebsch
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openttdcoop.dev.berries.irc;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.messaging.MessageContext.AccessType;
import org.openttdcoop.dev.grapes.messaging.MessageParser;
import org.openttdcoop.dev.grapes.security.SecurityLevel;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.MotdEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nathanael Rebsch
 */
public class IrcBot extends ListenerAdapter<PircBotX>
{
    class IrcPluginConnectTask extends TimerTask
    {
        private IrcBot ircbot;

        public IrcPluginConnectTask (IrcBot ircbot)
        {
            this.ircbot = ircbot;
        }

        @Override
        public void run ()
        {
            try {
                ircbot.bot.connect(ircbot.ircplugin.config.fetch("irc.host"), ircbot.ircplugin.config.fetch("irc.port", int.class));
            } catch (IOException ex) {
                ircbot.log.error("Exception trying to connect to IRC", ex);
            } catch (IrcException ex) {
                ircbot.log.error("Exception trying to connect to IRC", ex);
            }
        }
    }

    protected PircBotX bot = new PircBotX();
    private IrcPlugin ircplugin;
    private final Logger log = LoggerFactory.getLogger(IrcBot.class);

    public IrcBot (IrcPlugin ircplugin)
    {
        this.ircplugin = ircplugin;
        bot.getListenerManager().addListener(this);
    }

    public void connect ()
    {
        Timer t = new Timer();
        t.schedule(new IrcPluginConnectTask(this), 0);
    }

    @Override
    public void onMotd (MotdEvent<PircBotX> event) throws Exception
    {
        for (ConfigSection channel : ircplugin.channels.values()) {
            if (channel.fetch("autojoin", boolean.class)) {
                this.ircplugin.ircbot.bot.joinChannel(channel.getSimpleName(), channel.fetch("password"));
            }
        }
    }

    @Override
    public void onMessage (MessageEvent<PircBotX> event) throws Exception
    {
        Channel channel = event.getChannel();
        User user = event.getUser();
        String message = event.getMessage();

        ConfigSection cs = ircplugin.channels.get(channel.getName());
        SecurityLevel sl = SecurityLevel.ANONYMOUS;


        if (user.getChannelsOpIn().contains(channel)) {
            sl = cs.fetch("security.map.op", SecurityLevel.class, ircplugin.config.fetch("security.map.op", SecurityLevel.class));
        } else if (user.getChannelsVoiceIn().contains(channel)) {
            sl = cs.fetch("security.map.voice", SecurityLevel.class, ircplugin.config.fetch("security.map.voice", SecurityLevel.class));
        }

        IrcUser ircUser = new IrcUser(user, sl);
        IrcMessageProvider mp = new IrcMessageProvider(this, this.ircplugin);
        AccessType at = AccessType.PUBLIC;
        IrcMessageContext mc = new IrcMessageContext(mp, ircUser, event.getChannel().getName(), message, at);

        if (this.hasCommandPrefix(cs, "irc.cmdchar", message)) {
            List<String> parts = MessageParser.parseCommandArguments(message.substring(1));

            if (parts.isEmpty()) {
                return;
            }

            IrcCommandContext cc = new IrcCommandContext(mc);

            String[] pluginCmd = ircplugin.pm.splitPluginCommandArguments(cc, parts);
            cc.setArguments(parts);

            ircplugin.pm.executeCommand(cc, pluginCmd[0], pluginCmd[1]);
            return;
        }

        if (cs.fetch("chat.bridge", Boolean.class)) {
            message = String.format("[IRC] <%s> %s", user.getNick(), message);
            ircplugin.pm.getGrapes().serverMessagePublic(message);
            return;
        }

        if (cs.fetch("console.bridge", Boolean.class)) {
            ircplugin.pm.getGrapes().sendAdminRcon(message);
        }
    }
    
    @Override
    public void onPrivateMessage (PrivateMessageEvent<PircBotX> event) throws Exception
    {
        User user = event.getUser();

        /* do not allow unknown users */
        if (!this.isInMyChannel(user)) {
            log.debug("User {} is in none of my channels.", user.getNick());
            return;
        }

        SecurityLevel sl = SecurityLevel.ANONYMOUS;

        if (this.isOpInMyChannel(user)) {
            sl = ircplugin.config.fetch("security.map.op", SecurityLevel.class);
        } else if (this.isVoiceInMyChannel(user)) {
            sl = ircplugin.config.fetch("security.map.voice", SecurityLevel.class);
        }

        String message = event.getMessage();

        IrcUser ircUser = new IrcUser(user, sl);
        IrcMessageProvider mp = new IrcMessageProvider(this, this.ircplugin);
        AccessType at = AccessType.PRIVATE;
        IrcMessageContext mc = new IrcMessageContext(mp, ircUser, user.getNick(), message, at);

        int index = 0;
        if (this.hasCommandPrefix(ircplugin.config, "irc.cmdchar", message)) {
            index++;
        }

        List<String> parts = MessageParser.parseCommandArguments(message.substring(index));

        if (parts.isEmpty()) {
            return;
        }

        IrcCommandContext cc = new IrcCommandContext(mc);

        String[] pluginCmd = ircplugin.pm.splitPluginCommandArguments(cc, parts);
        cc.setArguments(parts);

        ircplugin.pm.executeCommand(cc, pluginCmd[0], pluginCmd[1]);
    }
    
    @Override
    public void onJoin (JoinEvent<PircBotX> event)
    {
        if (event.getUser().equals(bot.getUserBot())) {
            log.info("Joined Channel {}", event.getChannel().getName());
        }
    }

    private boolean hasCommandPrefix (final ConfigSection cs, final String propertyName, final String message)
    {
        return (!cs.fetch(propertyName).isEmpty() && message.trim().startsWith(cs.fetch(propertyName)));
    }

    private boolean isInMyChannel (User user)
    {
        Set<Channel> userChan = user.getChannels();

        if (userChan.isEmpty()) {
            return false;
        }

        for (Channel botChan : this.bot.getChannels()) {
            if (userChan.contains(botChan)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOpInMyChannel (User user)
    {
        Set<Channel> userChan = user.getChannelsOpIn();

        if (userChan.isEmpty()) {
            return false;
        }

        for (Channel botChan : this.bot.getChannels()) {
            if (userChan.contains(botChan)) {
                return true;
            }
        }

        return false;
    }

    private boolean isVoiceInMyChannel (User user)
    {
        Set<Channel> userChan = user.getChannelsVoiceIn();

        if (userChan.isEmpty()) {
            return false;
        }

        for (Channel botChan : this.bot.getChannels()) {
            if (userChan.contains(botChan)) {
                return true;
            }
        }

        return false;
    }
}