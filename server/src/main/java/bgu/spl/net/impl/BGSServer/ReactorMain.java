package bgu.spl.net.impl.BGSServer;
import bgu.spl.net.api.bidi.BidiMessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessageProtocolImpl;
import bgu.spl.net.impl.Messages.Message;
import bgu.spl.net.impl.DataBase;
import bgu.spl.net.srv.Server;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReactorMain {
    public static void main(String[] args) {
        if (args[0] == null) {
            System.out.println("Please enter legal port number");
            return;
        }
        if (args[1] == null) {
            System.out.println("Please enter legal number indicating the number of running threads.");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int numOfThreads = Integer.parseInt(args[1]);
        DataBase dataBase = new DataBase();
        ReadWriteLock logOrSendLock = new ReentrantReadWriteLock(true);
        ReadWriteLock registerOrLogStat = new ReentrantReadWriteLock(true);

        Server<Message> reactorServer = Server.reactor(
                numOfThreads,
                port,
                () -> new BidiMessageProtocolImpl(dataBase, logOrSendLock, registerOrLogStat),
                BidiMessageEncoderDecoder::new);
        reactorServer.serve();
    }
}
