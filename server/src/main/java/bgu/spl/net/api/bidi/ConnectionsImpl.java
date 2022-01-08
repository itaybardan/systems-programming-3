package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;

import java.util.HashMap;
import java.util.Map;

public class ConnectionsImpl<T> implements Connections<T> {

    private final Map<Integer, ConnectionHandler<T>> connectionHandlerMap;

    public ConnectionsImpl() {
        this.connectionHandlerMap = new HashMap<>();
    }

    public void addConnection(int currentId, ConnectionHandler<T> toAdd) {
        if (!this.connectionHandlerMap.containsValue(toAdd)) {
            this.connectionHandlerMap.put(currentId, toAdd);
        }
    }


    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> sender = this.connectionHandlerMap.get(connectionId);
        if (sender == null) {
            return false;
        } else {
            sender.send(msg);
            return true;
        }
    }

    @Override
    public void broadcast(T msg) {
        for (Map.Entry<Integer, ConnectionHandler<T>> entry : this.connectionHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().send(msg);
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        this.connectionHandlerMap.remove(connectionId);
    }
}
