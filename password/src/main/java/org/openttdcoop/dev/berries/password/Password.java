package org.openttdcoop.dev.berries.password;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openttd.OpenTTD;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.openttdcoop.dev.grapes.spi.OpenTTDExtentions.*;

/**
 * Password changer plugin to Grapes
 * Changes password from a given list on set intervals.
 * @author ODM
 */
public class Password extends GrapePluginImpl implements Runnable
{

    /**
     * The total number of words(-1) in the file.
     */
    private int maxWords = 0;
    /**
     * Filepath containing the words used.
     */
    private String filepath = "";

    @Override
    public boolean init(PluginManager pm, ConfigSection config)
    {
	this.pm = pm;
	this.config = config;
	try
	{
	    initConfig();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, ex.getCause().getMessage(), ex.getCause());
	}
	setMaxLines();

	return true;
    }

    private void initConfig() throws IOException
    {
	config.define("duration", 900000);
	config.define("wordfile", "../berries/password/src/main/resources/dictionaries/words6.txt");
	this.config.store();
    }

    /**
     * Loop that changes the password. Run in seperate thread, sleeps.
     */
    @Override
    public void run()
    {
	while (true)
	{
	    try
	    {
		String newpass = getNewPass();
		this.getPluginManager().getGrapes().getNetwork().SEND_ADMIN_PACKET_ADMIN_RCON("set network.server_password " + newpass);
	    }
	    catch (IOException e)
	    {
		Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, e.getMessage());
	    }
	    try
	    {
		Thread.sleep(Integer.parseInt(config.fetch("duration")));
	    }
	    catch (InterruptedException e)
	    {
		Logger.getLogger(OpenTTD.class.getName()).log(Level.WARNING, e.getMessage());
	    }
	}
    }

    /**
     * Determines the total number of words in the wordfile.
     * Used to be able to pick a word at random.
     */
    private void setMaxLines()
    {
	try
	{
	    filepath = config.fetch("wordfile");
	    File file = new File(filepath);
	    RandomAccessFile randFile = new RandomAccessFile(file, "r");
	    long last = randFile.length();
	    randFile.close();
	    FileReader reader = new FileReader(file);
	    LineNumberReader linereader = new LineNumberReader(reader);
	    linereader.skip(last);
	    maxWords = linereader.getLineNumber();
	    linereader.close();
	    reader.close();
	}
	catch (IOException e)
	{
	    Logger.getLogger(OpenTTD.class.getName()).log(Level.WARNING, e.getMessage());
	}
    }

    /**
     * Picks a random word from the file.
     * @return The new password.
     */
    private String getNewPass()
    {
	String pass = "";

	try
	{
	    File file = new File(filepath);
	    int word = (int) (Math.random() * maxWords);
	    FileReader reader = new FileReader(file);
	    LineNumberReader linereader = new LineNumberReader(reader);
	    for (int i = 0; i < word; i++)
	    {
		linereader.readLine();
	    }
	    pass = linereader.readLine();
	    linereader.close();
	    reader.close();
	}
	catch (IOException e)
	{
	    Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, e.getMessage());
	}
	return pass;
    }

    /**
     * Starts the new thread for this berry once a connection is made.
     */
    @Welcome
    public Boolean startPasswords()
    {
	System.out.println("Thread started");
	new Thread(this).start();
	return true;
    }
}
