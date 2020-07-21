package bling.App.Message;

import bling.Socket.Message;

import java.util.Map;

/**
 * Send from client to server to give information about servers' addresses.
 */
public class AddressMessage extends Message {
    public Map<Character,String> addrs;
}
