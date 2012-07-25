/*
 * Copyright (C) 2011 nathanael
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openttdcoop.dev.berries.irc;

import org.openttdcoop.dev.grapes.messaging.MessageProvider;

/**
 *
 * @author nathanael
 */
public class IrcMessageProvider implements MessageProvider<IrcUser>
{
    private IrcBot bot;
    private IrcPlugin plugin;

    public IrcMessageProvider (IrcBot bot, IrcPlugin plugin)
    {
        this.bot = bot;
        this.plugin = plugin;
    }
    
    @Override
    public String getMessageProviderName()
    {
        return "IRC";
    }

    @Override
    public void sendPublicMessage(String channel, String message)
    {
        this.bot.bot.sendMessage(channel, message);
    }

    @Override
    public void sendPrivateMessage(IrcUser user, String message)
    {
        this.bot.bot.sendMessage(user.getRealObject(), message);
    }

    @Override
    public void sendPublicNotice(String channel, String message)
    {
        this.bot.bot.sendNotice(channel, message);
    }

    @Override
    public void sendPrivateNotice(IrcUser user, String message)
    {
        this.bot.bot.sendNotice(user.getRealObject(), message);
    }
}
