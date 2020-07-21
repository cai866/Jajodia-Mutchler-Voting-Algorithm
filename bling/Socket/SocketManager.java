package bling.Socket;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manage all Socket Connections.
 *
 * Function:
 * 1. Running ServerSocket, waiting for new connection.
 * 2. Maintain a collections of current Socket Connections, support adding,
 *      removing by this machine, detecting disconnect from other side of socket.
 * 3. Manage JsonMessage Queue. Support sending messages, broadcasting and receive message from sockets.
 *
 * Property:
 * 1. Singleton Class
 *
 *
 * Project Language Level: 8
 */
public class SocketManager {


    private Map<Long,SocketThread> sockets = new ConcurrentHashMap<>(); // store all exists connections
    private ListeningThread listener = null; // ServerSocket, waiting for new connection.
    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();// message queue

    private int listenPort = 0;
    private Logger logger;



    public SocketManager(int listenPort){
        this.listenPort = listenPort;

        // set logger
        logger = Logger.getLogger("socket");
        logger.setLevel(Level.OFF);

        // launch listener
        if(listenPort != 0){
            listener = new ListeningThread(listenPort,this);
            listener.start();
        }
    }
    public SocketManager(){
        this(0);
    }

    /**
     * Start Socket Manager
     */
    @Deprecated
    public void start(){
        // do nothing
    }

    /**
     * Disconnect from all the sockets
     */
    public void stop(){
        try{
            // stop listener first
            if(listener != null){
                listener.shouldStop = true;
                listener.join(4000);
                if(listener.isAlive())
                    throw new IllegalThreadStateException("listener can't stop");
            }


            // stop other sockets
            for(SocketThread thread: sockets.values()){
                thread.shouldStop = true;
            }
            for(SocketThread thread: sockets.values()){
                thread.join(4000);
                if(thread.isAlive())
                    throw new IllegalThreadStateException("socket thread can't stop");
            }
        }catch(IllegalThreadStateException|InterruptedException e){
            e.printStackTrace();
            System.exit(5);
        }
    }



    /**
     * Connect to one address
     *
     * return the Socket Id
     *
     * @param ip ip to connect to
     * @param port port to connect to
     */
    public Long connect(String ip, int port) throws Exception{
        logger.info("try connect to " + ip + ":" + port);
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip,port),2000);
            addSocket(socket);
            return getID(socket);
        }catch(Exception e){
            logger.info("exception when connect to " + ip + ":"  + port + " " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Try to connect to all hosts in LAN with specific port.
     * @param port
     *
     * Bad implementation. When debug on one computer, since other host have same ip, they can't be connected.
     *      but if not do this, a host may connect to themself.
     */
    @Deprecated
    public void connectLAN(int port){
        logger.info("try connect to all hosts in LAN");
        String myIP = getIP();

        Queue<String> ips = scanIPinLAN();
        logger.info("reachable ip in LAN: " + ips);
        for(String ip: ips){ // scanIP() will scan the available IP in LAN
            if(ip.equals(myIP))
                continue;
            try{
                Socket socket = new Socket(ip,port);
                addSocket(socket);
            }catch(Exception e){
                logger.info("exception when connect to " + ip + ":"  + port + " " + e.getMessage());
                continue;
            }
        }
        logger.info(sockets.size() + " sockets connected in LAN");
    }

    /**
     * Add a socket into socket manager management.
     *
     * Note:
     * 1. It's possible to have two same socket connection between two machine.
     *      Since two machine could try connecting at the same time. So, check duplicate is
     *      necessary.
     */
    void addSocket(Socket socket){

        long id = getID(socket);
        try{
            // create a thread to hold that socket.
            if(!sockets.containsKey(id)){
                SocketThread thread = new SocketThread(socket,this);
                thread.start();
                sockets.put(id,thread);
                logger.info("socket added");
            }
            // If socket to that destination already exists, close this socket.
            else{
                logger.info("socket exists, close this socket");
                socket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
            System.exit(4);
        }
    }

    /**
     * Close and remove a socket
     *
     * Invoke by Socket manager: close socket actively.
     */
    public void stopSocket(long id){
        try{
            sockets.get(id).shouldStop = true;
            sockets.get(id).join(4000);
            if(sockets.get(id).isAlive())
                throw new IllegalThreadStateException("socket can't stop");
            sockets.remove(id);
        }catch(IllegalThreadStateException|InterruptedException e){
            e.printStackTrace();
            System.exit(6);
        }
    }

    public void stopAllSocket(){
        try{
            for(SocketThread st: sockets.values()){
               st.shouldStop = true;
            }
            for(SocketThread st: sockets.values()){
                st.join(4000);
            }
            for(SocketThread st: sockets.values()){
                if(st.isAlive())
                    throw new IllegalThreadStateException("socket can't stop");
            }
            sockets.clear();
        }catch(IllegalThreadStateException|InterruptedException e){
            e.printStackTrace();
            System.exit(6);
        }
    }

    /**
     * remove socket from socket list
     *
     * Invoke by socketThread: when socket disconnect
     * @param id
     */
    void removeSocket(long id){
        sockets.remove(id);
    }

    public int getSocketsSize(){
        return sockets.size();
    }




    /**
     * send message to socket manager
     */
    public void sendMessage(Message msg, long targetId){
        try{
            msg.targetID = targetId;
            sockets.get(targetId).sendMessage(msg);
        }catch(IOException e){
            e.printStackTrace();
            System.exit(7);
        }
    }

    public void broadcastMessage(Message msg) {
        for(long i: sockets.keySet()){
            sendMessage(msg,i);
        }
    }

    /**
     * read message from socket manager
     */
    public Message readMessage(){
        return messages.poll();
    }

    public boolean hasMessage(){
        return messages.size() > 0;
    }

    /**
     * Add message to message queue
     *
     * invoked by socket thread, when they received message
     */
    void addMessageToQueue(Message msg){
        messages.add(msg);
    }



    /**
     * get IP of Socket Manager
     * @return
     */
    String getIP(){
        try{
            return InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
            System.exit(3);
        }
        return null;
    }

    /**
     * get socket ID of our side
     * @return
     */
    long getLocalID(Socket socket){
        long id = 0;
        for(byte b: socket.getLocalAddress().getAddress()){
            id += (b + 128);
            id = id << 8;
        }
        id = id << 16;
        id += socket.getLocalPort();
        return id;
    }

    /**
     * get ID of a specific socket
     * @param socket
     * @return
     */
    long getID(Socket socket){
        long id = 0;
        for(byte b: socket.getInetAddress().getAddress()){
            id += (b+128);
            id = id << 8;
        }
        id = id << 16;
        id += socket.getPort();
        return id;
    }

    /**
     *  scan the available ip on LAN
     */
    static ConcurrentLinkedQueue<String> scanIPinLAN(){
        final byte[] ip;

        ConcurrentLinkedQueue<String> ips = new ConcurrentLinkedQueue<>();
        try {
            ip = InetAddress.getLocalHost().getAddress();
        }catch(UnknownHostException e){
            return ips;
        }

        for(int i=1;i<=254;i++) {
            final int j = i;  // i as non-final variable cannot be referenced from inner class
            new Thread(new Runnable() {   // new thread for parallel execution
                public void run() {
                    try {
                        ip[3] = (byte)j;
                        InetAddress address = InetAddress.getByAddress(ip);
                        String output = address.toString().substring(1);
                        if (address.isReachable(1000)) {
                            ips.add(output);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();     // dont forget to start the thread
        }
        return ips;
    }
}
