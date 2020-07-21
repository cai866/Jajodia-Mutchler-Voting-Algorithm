package bling.Util;

import bling.Socket.Message;

import java.net.Socket;

public class util {
    public static void println(Object obj){
        System.out.println(obj);
    }
    public static void print(Object obj) {System.out.print(obj);}

    public static void sleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException e){
            println(e.getMessage());
        }
    }
    public static String getIpFromAddress(String add){
        return add.split(":")[0];
    }

    public static int getPortFromAddress(String addr){
        return Integer.parseInt(addr.split(":")[1]);
    }

}
