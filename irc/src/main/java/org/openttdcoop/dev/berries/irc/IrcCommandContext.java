/*
 * Copyright (C) 2012 nathanael
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

import java.util.List;
import org.openttdcoop.dev.grapes.messaging.CommandContext;
import org.openttdcoop.dev.grapes.spi.GrapeCommand;
import org.pircbotx.Channel;

/**
 *
 * @author nathanael
 */
public class IrcCommandContext extends IrcMessageContext implements CommandContext<IrcMessageProvider, IrcUser>
{
    private String command;
    private List<String> arguments;

    public IrcCommandContext (IrcMessageProvider mp, IrcUser user, Channel channel, String message, AccessType access)
    {
        super(mp, user, channel, message, access);
    }

    public IrcCommandContext (IrcMessageContext mc)
    {
        super(mc);
    }

    protected void setArguments (List<String> arguments)
    {
        this.arguments = arguments;
    }

    @Override
    public String getCommand()
    {
        return this.command;
    }

    @Override
    public List<String> getArguments()
    {
        return this.arguments;
    }

    @Override
    public Class<? extends GrapeCommand> getNativeCommandType()
    {
        return IrcCommand.class;
    }
    
}
