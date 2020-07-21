package bling.App.Algorithm;

import bling.App.Server.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Algo {
    private Server server;
    private VotingInfo myVotingInfo;
    private Map<Character,VotingInfo> othersVotingInfo;
    private Logger logger;

    public Algo(Server server){
        this.server = server;
        othersVotingInfo = new HashMap<>();
        myVotingInfo = server.getVotingInfo();
        logger = server.logger;
    }

    public boolean canServerWrite(){
        // TODO: return whether can do write operation.
    }

    public void updateVotingInfo(){
        myVotingInfo.VN = getTopVN()+1;
        myVotingInfo.RU = othersVotingInfo.size() +1 ;
        myVotingInfo.DS = server.getPartition().substring(0,1);
    }

    public void putOtherVotingInfo(char siteName,VotingInfo info){
        othersVotingInfo.put(siteName,info);
    }

    public int getOtherInfoSize(){
        return othersVotingInfo.size();
    }

    public int getTopVN(){
        int max = myVotingInfo.VN;
        for(VotingInfo info: othersVotingInfo.values()){
            if(info.VN > max)
                max = info.VN;
        }
        return max;
    }
    public char getSiteWithTopVN(){
        int max = myVotingInfo.VN;
        char site = server.getSiteName();
        for(char key:othersVotingInfo.keySet()){
            if(othersVotingInfo.get(key).VN > max){
                max = othersVotingInfo.get(key).VN;
                site = key;
            }
        }
        return site;

    }

    public Map<Character,VotingInfo> getOthersVotingInfo(){
        return othersVotingInfo;
    }

    /**
     * Get partition that this server in
     * like: "ABC" or "DEFG"
     */
    String getPartition(){
        return server.getPartition();
    }

}
