package bling.App.Server;

import bling.App.Algorithm.Algo;
import bling.App.Algorithm.VotingInfo;
import bling.App.BaseSite;
import bling.App.Message.*;
import bling.Socket.Message;
import bling.Socket.SocketManager;
import bling.Util.util;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * The server
 *
 * Messages:
 * 1. Partition Message:
 *      message to inform server about the partition changes.
 *      Disconnect old sockets and connect new sockets.
 * 2. Write Message:
 *      write operation
 *
 *
 * Note:
 * 1. We make the assumption that no concurrent write operation will happen.
 *
 */
public class Server extends BaseSite {
    SocketManager sm;
    char siteName;
    Map<Character,String> serverAddrs;
    String partition;
    Map<Character,Long> serverNameToSocketIdMap;
    VotingInfo votingInfo;
    Algo algo;
    WriteMessage pendingWriteMessage;
    String FILE_NAME = "file1";
    public boolean stopFlag = false;
    int phase;// three phase: 0. Nothing 1. voting 2. catch-up 3. commit.
    ModifyLog modifyLog = new ModifyLog(2);
    int writeCommitReplyNumber = 0;

    public Server(int port){
        sm = new SocketManager(port);
        serverNameToSocketIdMap = new HashMap<>();
        phase = 0;
    }

    public void run(){
        try{
            _run();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Main Function
     * @throws Exception
     */
    void _run() throws Exception{
        logger.info("server start");

        // only message loop exists
        while(!stopFlag){
            Message msg = sm.readMessage();
            if(msg != null){
                logger.info("receive " + msg.getClass().getSimpleName()+
                        ": " + msg.toString());
                // partition message
                if(msg.getClass().equals(PartitionMessage.class))
                    processPartitionMessage((PartitionMessage)msg);
                // write message
                else if(msg.getClass().equals(WriteMessage.class))
                    processWriteMessage((WriteMessage)msg);
                // address message
                else if(msg.getClass().equals(AddressMessage.class))
                    processAddressMessage((AddressMessage) msg);
                // naming message
                else if(msg.getClass().equals(NamingMessage.class))
                    processNamingMessage((NamingMessage)msg);
                else if(msg.getClass().equals(VotingInfoMessage.class))
                    processVotingInfoMessage((VotingInfoMessage) msg);
                else if(msg.getClass().equals(CatchUpMessage.class))
                    processCatchUpMessage((CatchUpMessage) msg);
                else if(msg.getClass().equals(WriteCommitMessage.class))
                    processWriteCommitMessage((WriteCommitMessage)msg);
                // add new message
            }
            else{
                Thread.sleep(10);
            }
        }
        // stop server
        sm.stop();
        logger.info("server stop");
    }

    /**
     *  Message sending method
     */

    void broadcastInCurrentPartition(Message msg){
        for(char ch: partition.toCharArray()){
            if(ch != siteName){
                sm.sendMessage(msg,serverNameToSocketIdMap.get(ch));
            }
        }
    }


    /**
     *  Process Message
     */

    void processVotingInfoMessage(VotingInfoMessage msg) throws Exception{
        if(msg.request){
            logger.info("reply this message");
            // reply voting info message
            VotingInfoMessage rpy = new VotingInfoMessage();
            rpy.content = true;
            rpy.info = votingInfo;
            rpy.siteName = siteName;
            logger.info("reply content: " + rpy.toString());
            sm.sendMessage(rpy,msg.srcID);
        }
        else if(msg.content){
            logger.info("receive voting info from server " + msg.siteName);
            algo.putOtherVotingInfo(msg.siteName,msg.info);
            if(algo.getOtherInfoSize() == serverNameToSocketIdMap.size()){
                tryWrite();
            }
        }
        else{
            System.err.println("Bad VotingInfoMessage");
            System.exit(2);
        }
    }

    void processPartitionMessage(PartitionMessage msg) throws Exception{
        // disconnect old sockets
        for(long socketID: serverNameToSocketIdMap.values())
            sm.stopSocket(socketID);
        serverNameToSocketIdMap.clear();

        // save partition
        partition = msg.sites;

        // connect new sockets
        for(char ch:partition.toCharArray()){
            if(ch != siteName){
                String addr = serverAddrs.get(ch);
                String ip = util.getIpFromAddress(addr);
                int port = util.getPortFromAddress(addr);
                long socketId = sm.connect(ip,port);
                // save name -> socketId mapping
                serverNameToSocketIdMap.put(ch,socketId);
            }
        }

        // init voting info after the init partition is configured.
        if(votingInfo == null){
            votingInfo = new VotingInfo(this);
        }
        logger.info("new partition is: " + partition);
    }

    void processWriteMessage(WriteMessage msg) throws Exception{
        // request voting info from other servers in my partition
        logger.info("request voting message from other servers");
        VotingInfoMessage vmsg = new VotingInfoMessage();
        vmsg.request = true;
        broadcastInCurrentPartition(vmsg);


        // create Algorithm, wait for voting info
        if(algo == null){
            logger.info("create algo object");
            algo = new Algo(this);
        }
        else{
            System.err.println("Can't process a new write request before the last one finish");
            System.exit(2);
        }

        // pending write request
        pendingWriteMessage = msg;

        // if only myself in this partition
        if(partition.length() == 1)
            tryWrite();
    }

    void processAddressMessage(AddressMessage msg){
        serverAddrs = msg.addrs;
    }

    void processNamingMessage(NamingMessage msg){
        siteName = msg.name;
    }

    void processCatchUpMessage(CatchUpMessage msg) throws Exception{
        if(msg.request){
            // it's catch-up information request
            logger.info("this is a catch up request");

            // edge case
            if(modifyLog.size() == 0){
                throw new Exception("modify logs should not be empty");
            }

            // get catch up content
            List<String> catchUpContent = getCatchUpContent(msg.curVN);

            // send reply
            CatchUpMessage rpy = new CatchUpMessage();
            rpy.request = false;
            rpy.catchUpContent = catchUpContent;
            rpy.newVN = votingInfo.VN;
            logger.info("send catch-up reply: " + rpy);
            sm.sendMessage(rpy,msg.srcID);
        }
        else{
            // receive catch-up content
            logger.info("this is a catch up reply");

            // catch up
            logger.info("catching up");
            for(String content : msg.catchUpContent){
                writeFile(FILE_NAME,content);
            }

            // do unfinished write operation
            logger.info("after catch up, do unfinished write operation");
            doWriteOperation();
        }
    }

    void processWriteCommitMessage(WriteCommitMessage msg) throws Exception{
        if(msg.reply){
            // it's a reply
            logger.info("write commit reply received");
            writeCommitReplyNumber ++;
            if(writeCommitReplyNumber == partition.length()-1){
                // all other sites reply, write finished, send reply, clean up
                logger.info("all writeCommitMessge reply got");

                // send reply to client
                WriteReplyMessage rpy = new WriteReplyMessage();
                rpy.success = true;
                rpy.siteName = siteName;
                for(char site: partition.toCharArray()){
                    // we can do this because after update, all voting info in a partition is same.
                    rpy.votingInfos.put(site,votingInfo);
                }
                logger.info("send write reply to client: " + rpy.toString());
                sm.sendMessage(rpy,pendingWriteMessage.srcID);

                // clean up
                pendingWriteMessage = null;
                logger.info("algorithm finish, destroy algo object. Now votinginfo is: " + votingInfo);
                algo = null;
                writeCommitReplyNumber = 0;
            }
        }
        else{
            // receive a write commit message
            logger.info("write commit received");

            // do writing
            logger.info("do writing to catch up");
            for(String content: msg.contents){
                writeFile(FILE_NAME,content);
            }

            // update voting info
            votingInfo = msg.votingInfo;

            // send reply
            WriteCommitMessage rpy = new WriteCommitMessage();
            rpy.reply = true;
            logger.info("send reply to server, msg: " + rpy);
            sm.sendMessage(rpy,msg.srcID);
        }
    }

    /**
     * Voting Methods
     */

    public void updateVotingInfo(VotingInfo info){
        votingInfo = info;
    }

    public VotingInfo getVotingInfo(){
        return votingInfo;
    }
    public String getPartition(){
        return partition;
    }

    /**
     * Other Methods
     */

    List<String> getCatchUpContent(int oldVN){
        return modifyLog.getLogs(oldVN+1);
    }

    // Run this method means all data collected and algorithm is ready to
    // move from voting phase to catch-up phase.
    void tryWrite() throws Exception{
        logger.info("all data collected, run voting algorithm");
        // all voting info is collected, run voting algorithm
        if(algo.canServerWrite()){
            // voting pass
            logger.info("voting pass");
            // check if need catch-up
            if(algo.getTopVN() == votingInfo.VN){
                logger.info("no need to catch-up");
                // no need of catch-up
                doWriteOperation();
            }
            else{
                // need catch-up
                logger.info("need catch-up");
                // send catch-up message
                char newestSite = algo.getSiteWithTopVN();
                CatchUpMessage msg = new CatchUpMessage();
                msg.request = true;
                msg.curVN = votingInfo.VN;

                logger.info("send catch-up request: " + msg);
                sm.sendMessage(msg,serverNameToSocketIdMap.get(newestSite));
            }
            // do write operation
        }
        else{
            // voting not pass
            logger.info("voting fail");
            // send reply
            WriteReplyMessage rpy = new WriteReplyMessage();
            rpy.success = false;
            rpy.siteName = siteName;
            for(char site: partition.toCharArray()){
                // we can do this because after update, all voting info in a partition is same.
                rpy.votingInfos.put(site,votingInfo);
            }
            logger.info("send reply: " + rpy.toString());
            sm.sendMessage(rpy,pendingWriteMessage.srcID);
            // clean up
            pendingWriteMessage = null;
            logger.info("algorithm finish, destroy algo object");
            algo = null;
        }

    }

    void doWriteOperation() throws Exception{
        // write file
        logger.info("write new update: " +pendingWriteMessage.content);
        writeFile(FILE_NAME,pendingWriteMessage.content);
        // update voting info
        algo.updateVotingInfo();

        // do write on other sites, after receive their confirm, reply client
        logger.info("if needed, send WriteCommitMessage to other sites to update their content");
        Map<Character,VotingInfo> othersVotingInfos = algo.getOthersVotingInfo();
        for(char siteName: othersVotingInfos.keySet()){
            VotingInfo vi = othersVotingInfos.get(siteName);
            // get catch-up content
            logger.info("site " + siteName + " VN: " + vi.VN + ", my VN: " + votingInfo.VN);
            List<String> catchUpContent = getCatchUpContent(vi.VN);
            logger.info("send new modification to site " + siteName + ": " + catchUpContent);

            // send msg
            WriteCommitMessage msg = new WriteCommitMessage();
            msg.contents = catchUpContent;
            msg.votingInfo = votingInfo;
            sm.sendMessage(msg,serverNameToSocketIdMap.get(siteName));
        }
        writeCommitReplyNumber = 0;

        // if there is only one site in this partition,
        // no need to wait for WriteCommitMessage reply
        if(othersVotingInfos.size() == 0){
            logger.info("only one site in partition, no need to update other sites");

            // send reply to client
            WriteReplyMessage rpy = new WriteReplyMessage();
            rpy.success = true;
            rpy.siteName = siteName;
            rpy.votingInfos.put(siteName,votingInfo);
            logger.info("send write reply to client: " + rpy.toString());
            sm.sendMessage(rpy,pendingWriteMessage.srcID);

            // clean up
            pendingWriteMessage = null;
            logger.info("algorithm finish, destroy algo object. Now VotingInfo is: " + votingInfo);
            algo = null;
        }
    }

    void writeFile(String fileName, String content) throws Exception{
        // write file
        File file = new File(fileName);
        if(!file.exists())
            file.createNewFile();
        FileWriter writer = new FileWriter(file,true);
        writer.write(content);
        writer.flush();
        // add modification to log
        modifyLog.addLog(content);
    }

    public char getSiteName(){
        return siteName;
    }

    public void muteLog(){
        logger.setLevel(Level.OFF);
    }
}
