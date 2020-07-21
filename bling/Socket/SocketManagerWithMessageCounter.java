package bling.Socket;

public class SocketManagerWithMessageCounter  extends SocketManager{
    private int msgSent = 0;
    private int msgReceive = 0;

    public SocketManagerWithMessageCounter(int port){
        super(port);
    }
    public SocketManagerWithMessageCounter(){
        super();
    }
    @Override
    public Message readMessage() {
        Message msg = super.readMessage();
        if(msg != null)
            msgReceive ++;
        return msg;
    }

    @Override
    public void sendMessage(Message msg, long targetId) {
        msgSent ++;
        super.sendMessage(msg, targetId);
    }

    @Override
    public void broadcastMessage(Message msg) {
        msgSent += getSocketsSize();
        super.broadcastMessage(msg);
    }

    public int getNumMsgSent(){
        return msgSent;
    }
    public int getNumMsgRcv(){
        return msgReceive;
    }
}
