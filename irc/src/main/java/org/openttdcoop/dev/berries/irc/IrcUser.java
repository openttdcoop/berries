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

import org.openttdcoop.dev.grapes.messaging.User;
import org.openttdcoop.dev.grapes.security.SecurityLevel;

/**
 *
 * @author nathanael
 */
public class IrcUser implements User<org.pircbotx.User>
{
    private org.pircbotx.User user;
    private SecurityLevel sl;

    public IrcUser (org.pircbotx.User user, SecurityLevel sl)
    {
        this.user = user;
        this.sl = sl;
    }
    
    @Override
    public String getName()
    {
        return this.user.getNick();
    }

    @Override
    public String getHost()
    {
        return this.user.getHostmask();
    }

    @Override
    public String getIdentifier()
    {
        return String.format("%s@%s", this.user.getLogin(), this.user.getHostmask());
    }

    @Override
    public org.pircbotx.User getRealObject()
    {
        return this.user;
    }

    @Override
    public SecurityLevel getSecurityLevel ()
    {
        return this.sl;
    }
    
}
