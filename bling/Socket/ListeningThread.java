package bling.Socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Thread will be waiting for
 * connections from other process.
 *
 * When got incoming connection,
 * adding socket to socketManager.
 */
class ListeningThread  extends Thread{


    private ServerSocket serverSocket;
    private SocketManager socketManager;
    private Logger logger;

    private int port;
    boolean shouldStop = false; // Terminate Flag


    ListeningThread(int port, SocketManager sm){
        logger = Logger.getLogger("socket");
        logger.setLevel(Level.OFF);

        this.port = port;
        socketManager = sm;
    }


    @Override
    public void run(){
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);

            logger.info("socket manager listener start listening, port: " +port);
            while (true){

                if(shouldStop){
                    break;
                }

                try{
                    Socket socket = serverSocket.accept();
                    logger.info("receive new socket " + socketManager.getID(socket));
                    socketManager.addSocket(socket);
                }catch(IOException e){
                }
            }
            serverSocket.close();

        }catch(IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}
