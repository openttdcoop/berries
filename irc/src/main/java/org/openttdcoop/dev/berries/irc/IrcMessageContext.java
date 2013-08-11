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

import org.openttdcoop.dev.grapes.messaging.MessageContext;
import org.openttdcoop.dev.grapes.messaging.MessageContext.AccessType;

/**
 *
 * @author nathanael
 */
public class IrcMessageContext implements MessageContext<IrcMessageProvider, IrcUser>
{
    protected final IrcMessageProvider mp;
    protected final IrcUser user;
    protected final String message;
    protected final AccessType access;
    protected final String origin;

    public IrcMessageContext (IrcMessageProvider mp, IrcUser user, String origin, String message, AccessType access)
    {
        this.mp = mp;
        this.user = user;
        this.origin = origin;
        this.message = message;
        this.access = access;
    }

    protected IrcMessageContext (IrcMessageContext mc)
    {
        this.mp = mc.mp;
        this.user = mc.user;
        this.origin = mc.origin;
        this.message = mc.message;
        this.access = mc.access;
    }

    @Override
    public String getMessage ()
    {
        return this.message;
    }

    @Override
    public IrcMessageProvider getProvider ()
    {
        return this.mp;
    }

    @Override
    public IrcUser getUser ()
    {
        return this.user;
    }

    @Override
    public AccessType getAccessType ()
    {
        return this.access;
    }

    @Override
    public void reply (String message)
    {
        if (this.access == AccessType.PRIVATE) {
            mp.sendPrivateMessage(user, message);
        } else {
            this.mp.sendPublicMessage(this.origin, message);
        }
    }
}
