package bling.App.Message;

import bling.Socket.Message;

import java.util.List;

/**
 * send between servers, before write file, request and give catch up information.
 */
public class CatchUpMessage extends Message {
    public boolean request;
    public int curVN;
    public int newVN;
    public List<String> catchUpContent;
}
