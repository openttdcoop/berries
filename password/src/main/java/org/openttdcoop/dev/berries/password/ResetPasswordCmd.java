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

package org.openttdcoop.dev.berries.password;

import org.openttdcoop.dev.grapes.messaging.CommandContext;
import org.openttdcoop.dev.grapes.spi.GrapeCommand;
import org.openttdcoop.dev.grapes.spi.Rename;

/**
 *
 * @author Nathanael Rebsch
 */
@Rename("resetpassword")
public class ResetPasswordCmd implements GrapeCommand
{
    private PasswordPlugin password;
    
    public ResetPasswordCmd (PasswordPlugin password)
    {
        this.password = password;
    }

    @Override
    public void execute(CommandContext cc)
    {
        if (cc.getArguments().isEmpty()) {
            this.password.setNewPassword();
        } else {
            this.password.setNewPassword(cc.getArguments().get(0).toString());
        }

        cc.reply(this.password.getCurrentPassword());
    }

    @Override
    public String getHelp()
    {
        return "Reply with the current password to enter the game.";
    }
}
