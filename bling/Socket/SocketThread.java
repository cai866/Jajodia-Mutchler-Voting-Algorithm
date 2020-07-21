package bling.Socket;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A thread constantly wait for socket input.
 */
class SocketThread extends Thread{

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private SocketManager sm;
    private Logger logger;

    long id;
    boolean shouldStop = false;

    SocketThread(Socket socket, SocketManager socketManager) throws IOException {
        logger = Logger.getLogger("socket");
        logger.setLevel(Level.OFF);
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        sm = socketManager;
        id = sm.getID(socket);
    }

    @Override
    public void run() {
        try{
            // set read time out
            socket.setSoTimeout(1000);

            // looping for input stream
            while(true){
                try{
                    if(shouldStop)
                        break;

                    Message msg = (Message)in.readObject();
                    logger.info("receive message from: " + id + ", msg: " + msg.toString());
                    sm.addMessageToQueue(msg);
                }catch(SocketTimeoutException e) { // time out exception
                }catch(EOFException e) { // disconnect
                    logger.info("socket disconnect, id: " + id);
                    sm.removeSocket(sm.getID(socket));
                    break;
                }
            }
            in.close();
            out.close();
            socket.close();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(6);
        }
    }

    void sendMessage(Message msg) throws IOException{
        logger.info("send message to " + id + ", msg: " + msg.toString());
        msg.srcID = sm.getLocalID(socket);
        out.writeObject(msg);
    }

}
