package bling.App.Client;

public class ClientThread extends Thread{
    Client client;
    public ClientThread(String jsonConfig) throws Exception{
        client = new Client(jsonConfig);
    }

    @Override
    public void run() {
        client.run();
    }

    public void stopClient(){
        client.stopFlag = true;
    }
}
