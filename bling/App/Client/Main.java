package bling.App.Client;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class Main {
    public static void main(String[] args) throws Exception{
        Options options = new Options();
        options.addOption("c",true,"Path of config Json file");
        CommandLine cmd = new DefaultParser().parse(options,args);
        String configPath = cmd.getOptionValue("c");
        Client client = new Client(configPath);
        client.run();
    }
}
