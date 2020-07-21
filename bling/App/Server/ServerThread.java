package bling.App.Server;

public class ServerThread  extends Thread{
    public Server server;

    public ServerThread(int port){
        server = new Server(port);
    }

    @Override
    public void run() {
        server.run();
    }

    public void stopServer(){
        server.stopFlag = true;
    }


}
