package bling.App.Server;

import bling.App.Message.*;
import bling.Socket.SocketManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.HashMap;

import static bling.Util.util.println;

public class ServerTest {

    @Test
    void runServer() throws Exception{
        ServerThread thread = new ServerThread(1995);
        thread.start();
        Thread.sleep(1000);
        thread.stopServer();
        thread.join();
    }



    @Test
    void simpleProcessMessage() throws Exception{
        // start server
        ServerThread server1 = new ServerThread(1995);
        ServerThread server2 = new ServerThread(1996);
        server2.server.muteLog();
        server1.start();
        server2.start();

        // create client
        SocketManager client = new SocketManager();
        long sid_1 = client.connect("127.0.0.1",1995);
        long sid_2 = client.connect("127.0.0.1",1996);

        // process naming message
        {
            NamingMessage msg = new NamingMessage();
            msg.name = 'A';
            client.sendMessage(msg,sid_1);
            msg.name = 'B';
            client.sendMessage(msg,sid_2);
            Thread.sleep(500);
            Assertions.assertEquals('A',server1.server.siteName);
            Assertions.assertEquals('B',server2.server.siteName);
        }

        // process address message
        {
            AddressMessage msg = new AddressMessage();
            msg.addrs = new HashMap<>();
            msg.addrs.put('A',"127.0.0.1:1995");
            msg.addrs.put('B',"127.0.0.1:1996");
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals(2,server1.server.serverAddrs.size());
            Assertions.assertEquals(2,server2.server.serverAddrs.size());
        }

        // process partition message
        {
            PartitionMessage msg = new PartitionMessage();
            msg.sites = "AB";
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals("AB",server1.server.partition);
            Assertions.assertEquals("AB",server2.server.partition);
        }

        // process writing message
        {
            WriteMessage msg = new WriteMessage();
            msg.content = "test";
            client.sendMessage(msg,sid_1);
            Thread.sleep(500);
            WriteReplyMessage rpy = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(true,rpy.success);

        }

        client.stop();
        server1.stopServer();
        server2.stopServer();
        server1.join();
        server2.join();
    }

    @Test
    void complexProcessMessage() throws Exception{
        // start server
        ServerThread server1 = new ServerThread(1995);
        ServerThread server2 = new ServerThread(1996);
        server1.server.muteLog();
        server1.start();
        server2.start();

        // create client
        SocketManager client = new SocketManager();
        long sid_1 = client.connect("127.0.0.1",1995);
        long sid_2 = client.connect("127.0.0.1",1996);

        // process naming message
        {
            NamingMessage msg = new NamingMessage();
            msg.name = 'A';
            client.sendMessage(msg,sid_1);
            msg.name = 'B';
            client.sendMessage(msg,sid_2);
            Thread.sleep(500);
            Assertions.assertEquals('A',server1.server.siteName);
            Assertions.assertEquals('B',server2.server.siteName);
        }

        // process address message
        {
            AddressMessage msg = new AddressMessage();
            msg.addrs = new HashMap<>();
            msg.addrs.put('A',"127.0.0.1:1995");
            msg.addrs.put('B',"127.0.0.1:1996");
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals(2,server1.server.serverAddrs.size());
            Assertions.assertEquals(2,server2.server.serverAddrs.size());
        }

        println("partition: AB");
        /**
         * AB
         */
        // process partition message
        {
            PartitionMessage msg = new PartitionMessage();
            msg.sites = "AB";
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals("AB",server1.server.partition);
            Assertions.assertEquals("AB",server2.server.partition);
        }

        {
            WriteMessage msg = new WriteMessage();
            msg.content = "site A: test\n";
            client.sendMessage(msg,sid_1);
            Thread.sleep(500);
            WriteReplyMessage rpy = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(true,rpy.success);
        }

        println("partition: A,B");
        /**
         * A,B
         */
        // process partition message
        {
            PartitionMessage msg = new PartitionMessage();
            msg.sites = "A";
            client.sendMessage(msg,sid_1);
            msg.sites = "B";
            client.sendMessage(msg,sid_2);
            Thread.sleep(2000);
            Assertions.assertEquals("A",server1.server.partition);
            Assertions.assertEquals("B",server2.server.partition);
        }

        // process writing message
        {
            WriteMessage msg = new WriteMessage();

            msg.content = "site A: test\n";
            client.sendMessage(msg,sid_1);
            Thread.sleep(500);
            WriteReplyMessage rpy1 = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(true,rpy1.success);

            msg.content = "site B: test\n";
            client.sendMessage(msg,sid_2);
            Thread.sleep(500);
            WriteReplyMessage rpy2 = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(false,rpy2.success);
        }


        println("partition: AB");
        /**
         * AB
         */
        // process partition message
        {
            PartitionMessage msg = new PartitionMessage();
            msg.sites = "AB";
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals("AB",server1.server.partition);
            Assertions.assertEquals("AB",server2.server.partition);
        }

        // process writing message
        {
            WriteMessage msg = new WriteMessage();
            msg.content = "site B: test\n";
            client.sendMessage(msg,sid_2);
            Thread.sleep(500);
            WriteReplyMessage rpy = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(true,rpy.success);
        }

        client.stop();
        server1.stopServer();
        server2.stopServer();
        server1.join();
        server2.join();
    }

    @Test
    void oneSiteTest() throws Exception{
        // create server
        ServerThread server1 = new ServerThread(1995);
        server1.start();
        Thread.sleep(500);

        // create client
        SocketManager client = new SocketManager();
        long sid_1 = client.connect("127.0.0.1",1995);

        // process naming message
        {
            NamingMessage msg = new NamingMessage();
            msg.name = 'A';
            client.sendMessage(msg,sid_1);
            Thread.sleep(500);
            Assertions.assertEquals('A',server1.server.siteName);
        }

        // process address message
        {
            AddressMessage msg = new AddressMessage();
            msg.addrs = new HashMap<>();
            msg.addrs.put('A',"127.0.0.1:1995");
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals(1,server1.server.serverAddrs.size());
        }

        // process partition message
        {
            PartitionMessage msg = new PartitionMessage();
            msg.sites = "A";
            client.broadcastMessage(msg);
            Thread.sleep(500);
            Assertions.assertEquals("A",server1.server.partition);
        }

        // process writing message
        {
            WriteMessage msg = new WriteMessage();
            msg.content = "test";
            client.sendMessage(msg,sid_1);
            Thread.sleep(500);
            WriteReplyMessage rpy = (WriteReplyMessage) client.readMessage();
            Assertions.assertEquals(true,rpy.success);

        }
        client.stop();
        server1.stopServer();
        server1.join();
    }

    @Test
    void testModifyLog(){
        ModifyLog logs = new ModifyLog(1);
        logs.addLog("log1");
        logs.addLog("log2");
        logs.addLog("log3");
        logs.addLog("log4");
        Assertions.assertEquals(3,logs.getLogs(2).size());
    }

}
