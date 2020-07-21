package bling.App.Client;


import bling.App.Algorithm.VotingInfo;
import bling.App.BaseSite;
import bling.App.Message.*;
import bling.Socket.Message;
import bling.Socket.SocketManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;

import static bling.Util.util.print;
import static bling.Util.util.println;

public class Client extends BaseSite {
    SocketManager sm;
    BufferedReader keyInput;
    public boolean stopFlag = false;
    JSONObject config;
    Map<Character,Long> serverNameToSocketIdMap;
    boolean doWrite = false;
    Map<Character,VotingInfo> replyVotingInfo = new HashMap<>();
    String curPartition;
    Random random = new Random(new Date().getTime());

    /**
     *
     */
    public Client(String jsonConfig) throws Exception{
        sm = new SocketManager();
        keyInput = new BufferedReader(new InputStreamReader(System.in));
        try{
            // try use json config as file path
            config = new JSONObject(new JSONTokener(new FileInputStream(jsonConfig)));
        }catch(FileNotFoundException e){
            // try use it as json string
            try{
                config = new JSONObject(jsonConfig);
            }
            catch(JSONException je){
                System.err.println("config input error");
                System.exit(5);
            }
        }

        serverNameToSocketIdMap = new HashMap<>();
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
        logger.info("client start");

        init();

        printCmdHelp();

        while(!stopFlag){
            // process Message
            Message msg = sm.readMessage();
            if(msg != null){
                logger.info("get " + msg.getClass().getSimpleName() + ": " + msg);
                if(msg.getClass() == WriteReplyMessage.class){
                    processWriteReplyMessage((WriteReplyMessage)msg);
                }
            }
            // keyboard input
            else if(keyInput.ready()){
                String cmd = keyInput.readLine();
                logger.info("get command: " + cmd);
                switch(cmd){
                    case "write":
                        processWriteCmd();
                        break;
                    case "partition":
                        processPartitionCmd();
                        break;
                }
            }
            else{
                Thread.sleep(10);
            }
        }

        // stop client
        sm.stop();
        logger.info("client stop");
    }

    void init() throws Exception{
        logger.info("init client");
        char curServerName = 'A';
        Map<Character,String> addrs = new HashMap<>();

        // connect with servers
        for(Object addr:config.getJSONArray("servers")){
            String ip = ((String)addr).split(":")[0];
            int port = Integer.parseInt(((String)addr).split(":")[1]);
            long socketId = sm.connect(ip,port);
            // assign this server with a name
            serverNameToSocketIdMap.put(curServerName,socketId);
            addrs.put(curServerName,(String)addr);
            curServerName++;
        }
        logger.info("connect to " + addrs.size() + " servers");


        // inform server about their name
        logger.info("sending naming message");
        for(char name:serverNameToSocketIdMap.keySet()){
            NamingMessage msg = new NamingMessage();
            msg.name = name;
            sm.sendMessage(msg,serverNameToSocketIdMap.get(name));
        }

        // give address info to all servers
        logger.info("sending address messages");
        {
            AddressMessage msg = new AddressMessage();
            msg.addrs = addrs;
            sm.broadcastMessage(msg);
        }
    }

    /**
     * process message
     */

    void processWriteReplyMessage(WriteReplyMessage msg){
        // print voting info
        if(msg.success)
            println("write on site " + msg.siteName + " success");
        else
            println("write on site " + msg.siteName + " fail");
        replyVotingInfo.putAll(msg.votingInfos);


        if(replyVotingInfo.size() == serverNameToSocketIdMap.size()){
            // all data collected, show it
            logger.info("all partitions' write finish");

            List<Map.Entry<Character,VotingInfo>> votingInfoList =
                    new ArrayList<>(replyVotingInfo.entrySet());
            votingInfoList.sort((a1,a2)->a1.getKey()-a2.getKey());

            for(Map.Entry<Character,VotingInfo> entry: votingInfoList){
                println("Site " + entry.getKey() + ": " + entry.getValue());
            }

            replyVotingInfo.clear();
            doWrite = false;
        }

    }


    /**
     *  Process CMD input
     */

    void printCmdHelp(){

    }

    void processPartitionCmd() throws Exception{
        // show current servers
        {
            StringBuffer curServer = new StringBuffer();
            for(char ch:serverNameToSocketIdMap.keySet()){
                curServer.append(ch);
            }
            println("current servers: " + curServer.toString());
        }
        // input partition
        print("Input the partition: ");
        String line = keyInput.readLine();
        curPartition = line;
        // send partition info
        String[] parts = line.split(",");
        for(String part:parts){
            for(char ch: part.toCharArray()){
                long socketId = serverNameToSocketIdMap.get(ch);
                PartitionMessage msg = new PartitionMessage();
                msg.sites = part;
                sm.sendMessage(msg,socketId);
            }
        }
    }

    void processWriteCmd() throws Exception{

        // if there is a on going write command, refuse this new one
        if(doWrite){
            System.err.println("Last write not finish, can't make new one");
            return;
        }

        println("do write on all partitions: " + curPartition);
        // create write message
        String content = "Update at time: " + (new Date()).toString() + "\n";
        WriteMessage msg = new WriteMessage();
        msg.content = content;
        // send message to each partition
        for(String part: curPartition.split(",")){
            // randomly pick a server to do write
            char site = part.charAt(random.nextInt(part.length()));
            msg.content = "site " + site + ": " + content;
            sm.sendMessage(msg,serverNameToSocketIdMap.get(site));
        }
        doWrite = true;
    }

}
