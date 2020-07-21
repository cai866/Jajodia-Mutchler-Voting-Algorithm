package bling.App.Server;

import java.util.ArrayList;
import java.util.List;

public class ModifyLog {
    int curVN;
    int startVN;
    List<String> logs;

    ModifyLog(int startVN){
        logs = new ArrayList<>();
        this.startVN = startVN;
        curVN = startVN;
    }

    void addLog(String content){
        logs.add(content);
        curVN ++;
    }

    List<String> getLogs(int fromVN){
        return new ArrayList<>(logs.subList(fromVN - startVN,logs.size()));
    }

    int size(){
        return curVN-startVN;
    }
}
