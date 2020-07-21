package bling;

import bling.App.Message.PartitionMessage;
import bling.App.Server.Server;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static bling.Util.util.println;

public class Main {

    public static void main(String[] args) throws Exception{
        ArrayList<String> a = new ArrayList<>();
        List<String> b = a.subList(0,0);
        println(b.getClass());
    }
}
