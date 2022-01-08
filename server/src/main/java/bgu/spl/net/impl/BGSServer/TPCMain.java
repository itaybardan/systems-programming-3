package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.BidiMessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessageProtocolImpl;
import bgu.spl.net.api.bidi.Messages.Message;
import bgu.spl.net.srv.DataBase;
import bgu.spl.net.srv.Server;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TPCMain {
    public static void main(String[] args) {
        if (args[0] == null) {
            System.out.println("Please enter legal port number");
            return;
        }

        int port = Integer.parseInt(args[0]);
        //DataBase to hold all the messages and users of the BGSServer.
        DataBase dataBase = new DataBase();
        //ReadWriteLocks to synchronize different part of the functions in the DataManager
        ReadWriteLock logOrSendLock = new ReentrantReadWriteLock(true);
        ReadWriteLock registerOrUserList = new ReentrantReadWriteLock(true);
        //creating and activating the Tread-Per-Client Server
        Server<Message> threadPerClientServer = Server.threadPerClient(
                port,
                () -> new BidiMessageProtocolImpl(dataBase, logOrSendLock, registerOrUserList),
                BidiMessageEncoderDecoder::new);

        threadPerClientServer.serve();
    }
}
