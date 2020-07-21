package bling.Socket;

import java.io.Serializable;
import java.lang.reflect.Field;

public class Message implements Serializable {
    public long srcID;
    public long targetID;

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

// for test
class UpMessge extends Message {
    String content;

}
