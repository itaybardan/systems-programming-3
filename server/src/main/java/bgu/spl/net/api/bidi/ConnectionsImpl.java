package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.bidi.ConnectionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {

    /**
     * Map of ids as keys to connectionHandlers as values.
     */
    private Map<Integer,ConnectionHandler<T>> connectionHandlerMap;

    public ConnectionsImpl() {
        this.connectionHandlerMap = new HashMap<>();
    }

    /**
     * Adding new connectionHandler with it's matching id.
     * @param currentId     Integer represents the unique id of the given connectionHandler.
     * @param toAdd         ConnectionHandler to add to this connections object.
     */
    public void addConnection(int currentId,ConnectionHandler<T> toAdd){
        if(!this.connectionHandlerMap.values().contains(toAdd)){
            this.connectionHandlerMap.put(currentId,toAdd);
        }
    }

    /**
     * Sends the given message to the the connectionHandler that matches the given id.
     * @param connectionId      Integer represents the connectionId of the desired connectionHandler.
     * @param msg               T object to send.
     * @return                  True if the message was sent successfully, false otherwise.
     */
    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> sender = this.connectionHandlerMap.get(connectionId);
        if(sender == null){
            return false;
        }
        else{
            sender.send(msg);
            return true;
        }
    }

    /**
     * Sends the given message to all available connectionHandlers.
     * @param msg               T object to send.
     */
    @Override
    public void broadcast(T msg) {
        for(Map.Entry<Integer, ConnectionHandler<T>> entry:this.connectionHandlerMap.entrySet()) {
            if(entry.getValue() != null){
                entry.getValue().send(msg);
            }
        }
    }

    /**
     * Disconnects the connectionHandler with the matching given connectionId.
     * @param connectionId      Integer represents the connectionId of the desired connectionHandler.
     */
    @Override
    public void disconnect(int connectionId) {
        this.connectionHandlerMap.remove(connectionId);
    }
}
