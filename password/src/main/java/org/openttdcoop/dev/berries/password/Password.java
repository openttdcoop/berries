package org.openttdcoop.dev.berries.password;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openttd.OpenTTD;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDWelcome;
import org.openttdcoop.dev.berries.password.spi.PasswordChanged;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;

/**
 * Password changer plugin to Grapes
 * Changes password from a given list on set intervals.
 * @author ODM
 */
public class Password extends GrapePluginImpl implements Runnable, OpenTTDWelcome
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    private List<String> passwords = new ArrayList<String>();
    private String curPass = "";

    @Override
    public boolean init()
    {
        try {
            initConfig();
        } catch (IOException ex) {
            Logger.getLogger(OpenTTD.class.getName()).log(Level.SEVERE, ex.getCause().getMessage(), ex.getCause());
        }
        setMaxLines();

        return true;
    }

    private void initConfig() throws IOException
    {
        config.define("duration", 900000);
        config.define("wordfile", "dictionaries/words6.txt");
        this.config.store();
    }

    /**
     * Loop that changes the password. Run in seperate thread, sleeps.
     */
    @Override
    public void run()
    {
        while (true) {
                String newpass = getNewPass();
                pm.getGrapes().sendAdminRcon("set network.server_password " + newpass);
                pm.invoke(PasswordChanged.class, newpass);
            try {
                Thread.sleep(Integer.parseInt(config.fetch("duration")));
            } catch (InterruptedException e) {
                Logger.getLogger(OpenTTD.class.getName()).log(Level.WARNING, e.getMessage());
            }
        }
    }

    /**
     * Read the words file and save the number of usable records.
     */
    private void setMaxLines()
    {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(config.fetch("wordfile"));
            System.out.println(is.toString());
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            
            String line;

            while ((line = br.readLine()) != null)
            {
                /* do not take epty passwords */
                if (line.matches("^\\s*$")) {
                    continue;
                }

                passwords.add(line);
            }

            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            Logger.getLogger(OpenTTD.class.getName()).log(Level.WARNING, e.getMessage());
        }
    }

    /**
     * Picks a random word from the file.
     * @return The new password.
     */
    private String getNewPass()
    {
        Random r = new Random();
        int randint = Math.abs(r.nextInt()) % passwords.size();

        curPass = passwords.get(randint);

        return curPass;
    }

    /**
     * Get the current Password.
     * @return The current Password.
     */
    public String getCurrentPassword ()
    {
        return curPass;
    }

    /**
     * Starts the new thread for this berry once a connection is made.
     */
    @Override
    public void onOpenTTDWelcome()
    {
        System.out.println("Thread started");
        new Thread(this).start();
    }
}
