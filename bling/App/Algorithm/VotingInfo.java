package bling.App.Algorithm;

import bling.App.Server.Server;

import java.io.Serializable;
import java.lang.reflect.Field;

public class VotingInfo implements Serializable {
    public VotingInfo(Server server){
        VN = 1;
        RU = server.getPartition().length();
        // TODO: should include 3 when only 3 servers exists?
        DS = server.getPartition().substring(0,1);
    }
    public int VN;
    public int RU;
    public String DS;

    @Override
    public String toString(){
        try{
            String rtn = "";
            rtn += "[";
            for(Field field: this.getClass().getFields()){
                rtn += field.getName() + ":" + field.get(this) + ", ";
            }
            rtn += "]";
            return rtn;
        }
        catch(Exception e){
            e.printStackTrace();
            System.exit(3);
        }
        return null;
    }
}
