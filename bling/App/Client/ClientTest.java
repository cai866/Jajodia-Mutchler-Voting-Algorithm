package bling.App.Client;

import org.junit.jupiter.api.Test;

public class ClientTest {
    @Test
    void runClient() throws Exception{
        ClientThread client = new ClientThread("{\"servers\":[]}");
        client.start();
        Thread.sleep(500);
        client.stopClient();
        client.join();
    }
}
