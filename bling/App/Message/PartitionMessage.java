package bling.App.Message;

import bling.Socket.Message;

/**
 * from client to server, appoint partition for server
 */
public class PartitionMessage extends Message {
    public String sites;
}

