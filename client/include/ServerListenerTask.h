//
// Created by tomergon@wincs.cs.bgu.ac.il on 1/5/19.
//

#ifndef BOOST_ECHO_CLIENT_SERVERLISTENERTASK_H
#define BOOST_ECHO_CLIENT_SERVERLISTENERTASK_H

#include "connectionHandler.h"

/**
 * Class represents a Task to run by the bgs client main:
 * This task is reading messages that was sent by the server to this client.
 */
class ServerListenerTask{
private:
    /**
     * Connection Handler to receive messages from the server to this client.
     */
    ConnectionHandler* ch;
public:
    /**
     * Default constructor.
     * @param connectionHandler        Connection Handler to receive messages from the server to this client.
     */
    ServerListenerTask(ConnectionHandler* connectionHandler);
    /**
     * Run method to run in a separate thread, that will read incoming messages from the server,
     * as long as the client is connected to the server.
     */
    void run();
};

#endif //BOOST_ECHO_CLIENT_SERVERLISTENERTASK_H
