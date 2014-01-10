package org.openttdcoop.dev.berries.cmdlog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDCmdLogging;
import org.openttdcoop.dev.berries.openttd.spi.OpenTTDCmdLoggingEvent;
import org.openttdcoop.dev.grapes.plugin.PluginManager;
import org.openttdcoop.dev.grapes.config.ConfigSection;
import org.openttdcoop.dev.grapes.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CmdLog - log every docommand received in a file.
 */
@Rename("CmdLog")
public class CmdLogPlugin extends GrapePluginImpl implements OpenTTDCmdLogging
{
    @InjectPluginManager
    protected PluginManager pm;

    @InjectPluginConfig
    protected ConfigSection config;

    private final Logger log = LoggerFactory.getLogger(CmdLogPlugin.class);
    private PrintWriter out;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public boolean init ()
    {
        try {
            initConfig();

            this.out = new PrintWriter(new BufferedWriter(new FileWriter(config.fetch("log.file"), true)));
        } catch (IOException ex) {
            this.log.error(ex.getCause().getMessage(), ex.getCause());
        }

        return true;
    }

    private void initConfig () throws IOException
    {
        this.config.define("log.file", "${grapes/plugin.dir}/docommands.log");
        this.config.store();
    }

    @Override
    public void onOpenTTDCmdLogging (OpenTTDCmdLoggingEvent event)
    {
        Date date = new Date();
        String pattern = "%s Frame: %d Command: %s Tile: 0x%04X P1: %d P2 %d Text \"%s\" CompanyID #%d (%s) ClientID #%d (%s)";
        this.out.println(String.format(pattern, sdf.format(date), event.frame, event.command.toString(), event.tile, event.p1, event.p2, event.text, event.company.id, event.company.name, event.client.id, event.client.name));
        this.out.flush();
    }
}
