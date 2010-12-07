package org.openttdcoop.dev.berries.commander;

import org.openttdcoop.dev.grapes.enums.CommandAccess;
import java.io.IOException;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openttd.Client;
import org.openttd.OpenTTD;
import org.openttd.enums.DestType;
import org.openttd.enums.NetworkAction;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.openttdcoop.dev.grapes.spi.GrapeExtentionPoint.*;
import org.openttdcoop.dev.grapes.spi.OpenTTDExtentions.*;

/**
 * Commander plugin for Grapes.
 * Processes commands. Can insert itself into IRC and the game itself.
 * @author ODM
 */
public class Commander extends GrapePluginImpl
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    Commands commands = new Commands();

    @Override
    public boolean init()
    {
	try
	{
	    initConfig();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, ex.getCause().getMessage(), ex.getCause());
	}

	return true;
    }

    private void initConfig() throws IOException
    {
	this.config.define("ingame.commands", true);
	this.config.define("ingame.commandchar", '!');
	this.config.store();
    }

    /**
     * Adds the commander to grapes once the server has started.
     */
    @Welcome
    public Boolean linkCommander()
    {
	if (Boolean.parseBoolean(this.config.fetch("ingame.commands")) && this.config.fetch("ingame.commandchar").length() > 0)
	{
	    char ingamechar = this.config.fetch("ingame.commandchar").charAt(0);
	    this.pm.getGrapes().setCommandChar(ingamechar);
	}
	return true;
    }

    /**
     * Processes a command received from ingame chat.
     * @param client The client requesting the command.
     * @param message The command message.
     */
    @Command
    public Boolean ingameCommandReceived(NetworkAction action, DestType desttype, Client client, String message, BigInteger data)
    {
	EnumSet access = EnumSet.of(CommandAccess.Ingame);
	processCommand(message, access);
	try{
	    this.pm.getGrapes().getNetwork().chatPublic("Response to ingame.");
	}
	catch(IOException e)
	{
	    Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, e.getMessage());
	}
	return true;
    }

    /**
     * Processes a command from IRC, either from a channel or a PM.
     * @param message  The command message.
     * @param channel Originating channel, null if PM.
     * @param user The user making the request.
     * @param access The access this user has on IRC.
     * @return
     */
    @IrcCommand
    public Boolean ircCommandReceived(String message, String channel, String user, EnumSet<CommandAccess> access)
    {
	String answer = processCommand(message, access);
	if (answer != null)
	    this.pm.invoke(IrcCommandReply.class, channel, user, answer);
	return true;
    }

    /**
     * Processes a command by splitting its parameters and finding the result.
     * @param message The full command message.
     * @param access The acces available for this command.
     * @return The returned string, or null if not applicable/command not found/command can't be processed.
     */
    public String processCommand(String message, EnumSet<CommandAccess> access)
    {
	message = message.trim();
	if (message != null && message.length() != 0)
	{
	    try {
	    String command = message.split(" ")[0];
	    System.out.println("Command: " + command);
	    String[] parameters = new String[0];
	    if (message.length() != command.length())
	    {
		String rest = message.substring(command.length()).trim();
		parameters = parseParameters(rest);
	    }
	    return findCommand(command, parameters, access);
	    }
	    catch (Exception e)
	    {
		Logger.getLogger(OpenTTD.class.getName()).log(Level.WARNING, e.getMessage());
	    }
	}
	return null;
    }

    /**
     * Parses a string of parameters into subsections.
     * @param params The full string containing the parameters.
     * @return An array of Strings containing the seperate parameters.
     */
    public String[] parseParameters(String params)
    {
	boolean inquotes = false;
	StringBuilder current = new StringBuilder();
	LinkedList<String> parameters = new LinkedList<String>();
	for (int i = 0; i <params.length();i++)
	{
	    char c = params.charAt(i);
	    if (inquotes && c != '"')
		current.append(c);
	    else if (inquotes && c == '"')
		inquotes = false;
	    else if (c == '"')
	    {
		inquotes = true;
		if (current.length() > 0)
		{
		    parameters.add(current.toString());
		    current = new StringBuilder();
		}
	    }
	    else if (c == ' ')
	    {
		if (current.length() > 0)
		{
		    parameters.add(current.toString());
		    current = new StringBuilder();
		}
	    }
	    else
	    {
		current.append(c);
	    }
	}
	if (current.length() > 0)
	    parameters.add(current.toString());
	System.out.println(parameters);
	return parameters.toArray(new String[parameters.size()]);
    }

    /**
     * Finds the command in the commands available, and processes them.
     * @param command The command to find.
     * @param parameters The matching parameters.
     * @param access The access available for the command.
     * @return The returned answer String, or null if not found/not applicable.
     */
    //TODO: Add switch for commands
    private String findCommand(String command, String[] parameters, EnumSet<CommandAccess> access)
    {
	return commands.example(parameters, access);
    }
}
