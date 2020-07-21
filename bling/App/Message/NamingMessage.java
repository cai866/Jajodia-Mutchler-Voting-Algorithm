package bling.App.Message;

import bling.Socket.Message;

/**
 * from client to server, name the server
 */
public class NamingMessage extends Message {
    public char name;
}
