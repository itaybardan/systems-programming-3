package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    this.protocol.process(nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Sends the given message to the client that is connected to this Connections handler
     *
     * @param msg T object to send back to the client.
     */
    @Override
    public void send(T msg) {
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException ignored) {

        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    /**
     * initialising the protocol of this connection Handler with the connection object and this ConnectionHandler unique id.
     *
     * @param connectionID Integer represents this ConnectionHandler unique id in the connections objects.
     * @param connections  Connections Object to reference to the protocol of this ConnectionHandler.
     */
    public void start(int connectionID, Connections<T> connections) {
        this.protocol.start(connectionID, connections);
    }
}
