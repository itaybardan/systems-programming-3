package bgu.spl.net.impl.BGSServer;
import bgu.spl.net.api.bidi.BidiMessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessageProtocolImpl;
import bgu.spl.net.impl.Messages.Message;
import bgu.spl.net.impl.DataBase;
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
        DataBase dataBase = new DataBase();
        ReadWriteLock logOrSendLock = new ReentrantReadWriteLock(true);
        ReadWriteLock registerOrLogStat = new ReentrantReadWriteLock(true);

        Server<Message> threadPerClientServer = Server.threadPerClient(
                port,
                () -> new BidiMessageProtocolImpl(dataBase, logOrSendLock, registerOrLogStat),
                BidiMessageEncoderDecoder::new);
        threadPerClientServer.serve();
    }
}
