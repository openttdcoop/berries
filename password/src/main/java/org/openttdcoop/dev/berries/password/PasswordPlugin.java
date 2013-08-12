package org.openttdcoop.dev.berries.password;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDWelcome;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDWelcomeEvent;
import org.openttdcoop.dev.berries.password.spi.PasswordChanged;
import org.openttdcoop.dev.berries.password.spi.PasswordChangedEvent;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.security.SecurityLevel;
import org.openttdcoop.dev.grapes.spi.*;
import org.slf4j.LoggerFactory;

/**
 * Password changer plugin to Grapes
 * Changes password from a given list on set intervals.
 * @author ODM
 */
@Rename("Password")
public class PasswordPlugin extends GrapePluginImpl implements Runnable, OpenTTDWelcome
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    private List<String> passwords = new ArrayList<String>();
    private String curPass = "";

    private final org.slf4j.Logger log = LoggerFactory.getLogger(PasswordPlugin.class);
    private final String RESOURCE_WORDS = "!/dictionaries/words6.txt";

    protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    protected ScheduledFuture<?> task;

    @Override
    public boolean init()
    {
        try {
            initConfig();
            pm.registerCommand(new PasswordCmd(this));
            pm.registerCommand(new ResetPasswordCmd(this), SecurityLevel.ADMIN);
            pm.registerCommand(new StopCmd(this), SecurityLevel.ADMIN);
            pm.registerCommand(new StartCmd(this), SecurityLevel.ADMIN);
        } catch (IOException ex) {
            this.log.error(ex.getCause().getMessage(), ex.getCause());
        }
        loadPasswords();

        return true;
    }

    private void initConfig() throws IOException
    {
        config.define("duration", 60);
        config.define("wordfile", "jar:file:${grapes/plugin.dir}./" + this.pm.getPluginDescriptor(this).getJarFile() + this.RESOURCE_WORDS);
        this.config.store();
    }

    /**
     * Loop that changes the password. Run in seperate thread, sleeps.
     */
    @Override
    public void run()
    {
        setNewPassword();
    }

    /**
     * Read the words file and save the number of usable records.
     */
    private void loadPasswords()
    {
        try {
            InputStream is;
            URL resource;
            
            String wordfile = config.fetch("wordfile");
            
            if (wordfile.isEmpty()) {;
                resource = this.pm.getPluginDescriptor(this).getResourceInsideJar(this.RESOURCE_WORDS);
            } else {
                resource = new URL(wordfile);
            }
            
            is = resource.openStream();
            
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
        } catch (Exception ex) {
            this.log.warn(ex.getMessage(), ex);
        }
    }

    protected void setNewPassword()
    {
        this.setNewPassword(getNewPass());
    }
    
    protected void setNewPassword(String newpass)
    {
        pm.getGrapes().sendAdminRcon("set network.server_password " + newpass);
        curPass = newpass;
        GrapeEvent event = new PasswordChangedEvent(newpass);
        pm.invoke(PasswordChanged.class, event);
    }
    
    /**
     * Picks a random word from the file.
     * @return The new password.
     */
    private String getNewPass()
    {
        Random r = new Random();
        int randint = Math.abs(r.nextInt()) % passwords.size();

        return passwords.get(randint);
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
    public void onOpenTTDWelcome(OpenTTDWelcomeEvent event)
    {
        this.startTask();
    }

    protected void startTask ()
    {
        task = scheduler.scheduleAtFixedRate(this, 0, config.fetch("duration", int.class), TimeUnit.SECONDS);
    }
}
