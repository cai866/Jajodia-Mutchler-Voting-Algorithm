package bling.App.Message;

import bling.App.Algorithm.VotingInfo;
import bling.Socket.Message;

/**
 * send between servers. request or reply voting info
 */
public class VotingInfoMessage extends Message {
    public boolean request;
    public boolean content;
    public char siteName;
    public VotingInfo info;
}
