//
// Created by tomergon@wincs.cs.bgu.ac.il on 1/5/19.
//

#ifndef BOOST_ECHO_CLIENT_CLIENTREQUESTTASK_H
#define BOOST_ECHO_CLIENT_CLIENTREQUESTTASK_H

#include "ConnectionHandler.h"
/**
 * Class represents a Task to run by the bgs client main:
 * This task is sending the user request input from the keyboard to the server.
 */
class ClientRequestTask {
private:
    /**
     * Connection Handler to send the user requests.
     */
    ConnectionHandler *ch;
public:
    /**
     * Default constructor.
     * @param connectionHandler     ConnectionHandler to handler user requests.
     */
    ClientRequestTask(ConnectionHandler *connectionHandler);
    /**
     * Run method to run in a separate thread, that will read user input from the keyboard and send it to the server,
     * as long as the client is connected to the server.
     */
    void run();
};

#endif //BOOST_ECHO_CLIENT_CLIENTREQUESTTASK_H
