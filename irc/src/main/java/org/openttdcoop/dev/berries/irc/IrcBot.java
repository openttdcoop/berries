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

import java.util.List;
import java.util.Set;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.messaging.MessageContext.AccessType;
import org.openttdcoop.dev.grapes.messaging.MessageParser;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nathanael Rebsch
 */
public class IrcBot extends ListenerAdapter<PircBotX> {

    protected PircBotX bot = new PircBotX();
    private IrcPlugin ircplugin;
    private final Logger log = LoggerFactory.getLogger(IrcBot.class);

    public IrcBot(IrcPlugin ircplugin) {
        this.ircplugin = ircplugin;
        bot.getListenerManager().addListener(this);
    }

    @Override
    public void onMessage(MessageEvent<PircBotX> event) throws Exception {
        Channel channel = event.getChannel();
        User user = event.getUser();
        String message = event.getMessage();
        
        ConfigSection cs = ircplugin.channels.get(channel.getName());
        
        IrcUser ircUser = new IrcUser(event.getUser());
        IrcMessageProvider mp = new IrcMessageProvider(this, this.ircplugin);
        AccessType at = AccessType.PUBLIC;
        IrcMessageContext mc = new IrcMessageContext(mp, ircUser, event.getChannel(), event.getMessage(), at);

        if (this.hasCommandPrefix(cs, message)) {
            List<String> parts = MessageParser.parseCommandArguments(message.substring(1));
                    
            if (parts.isEmpty()) {
                return;
            }
            
            IrcCommandContext cc = new IrcCommandContext(mc);
            cc.setArguments(parts);

            String[] pluginCmd = ircplugin.pm.splitPluginCommandArguments(cc);

            ircplugin.pm.execute(cc, pluginCmd[0], pluginCmd[1]);
            return;
        }

        if (cs.fetch("chat.bridge", Boolean.class)) {
            message = String.format("[IRC] <%s> %s", user.getNick(), message);
            ircplugin.pm.getGrapes().serverMessagePublic(message);
            return;
        }

        if (cs.fetch("console.bridge", Boolean.class)) {
            log.error("console brige");
            ircplugin.pm.getGrapes().sendAdminRcon(event.getMessage());
            return;
        }
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {

        /* do not allow unknown users */
        if (this.isInMyChannel(event.getUser())) {
            return;
        }

        /* Todo: Command Handling */
    }

    private boolean hasCommandPrefix(final ConfigSection cs, final String message) {
        return (!cs.fetch("chat.cmdchar").isEmpty() && message.trim().startsWith(cs.fetch("chat.cmdchar")));
    }

    private boolean isInMyChannel(User user) {
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
}