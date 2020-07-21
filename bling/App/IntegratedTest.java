package bling.App;

import bling.App.Client.Client;
import bling.App.Client.ClientThread;
import bling.App.Server.ServerThread;
import org.junit.jupiter.api.Test;

public class IntegratedTest {
    @Test
    void wholeProcessTest() throws Exception{
    }

    public static void main(String[] args) throws Exception{
        // create 8 servers
        ServerThread[] servers = new ServerThread[8];
        for(int i = 0; i< 8 ;i++){
            servers[i] = new ServerThread(1995+i);
            if(i >= 0 && i <= 3)
                servers[i].server.muteLog();
            servers[i].start();
        }
        Thread.sleep(500);

        // create  1 client and run
        String clientConfig = "{\"servers\":[" +
                "\"127.0.0.1:1995\"," +
                "\"127.0.0.1:1996\"," +
                "\"127.0.0.1:1997\"," +
                "\"127.0.0.1:1998\"," +
                "\"127.0.0.1:1999\"," +
                "\"127.0.0.1:2000\"," +
                "\"127.0.0.1:2001\"," +
                "\"127.0.0.1:2002\"" +
                "]}";
        Client client = new Client(clientConfig);
        client.run();



    }
}
