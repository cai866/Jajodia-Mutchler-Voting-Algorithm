package bling.App.Message;

import bling.App.Algorithm.VotingInfo;
import bling.Socket.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * after server complete write operation, send this confirmation from server to client
 */
public class WriteReplyMessage extends Message {
    public boolean success;
    public Map<Character,VotingInfo> votingInfos = new HashMap<>();
    public char siteName;
}
