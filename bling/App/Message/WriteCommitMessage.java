package bling.App.Message;

import bling.App.Algorithm.VotingInfo;
import bling.Socket.Message;

import java.util.List;

/**
 * send between servers, after write operation, send to other servers to update their file.
 */
public class WriteCommitMessage extends Message {
    public boolean reply;
    public List<String> contents;
    public VotingInfo votingInfo;
}
