//
// Created by tomergon@wincs.cs.bgu.ac.il on 1/5/19.
//


#include "../include/ServerListenerTask.h"
/**
 * Default constructor.
 * @param connectionHandler        Connection Handler to receive messages from the server to this client.
 */
ServerListenerTask::ServerListenerTask(ConnectionHandler* connectionHandler):ch(connectionHandler){}
/**
* Run method to run in a separate thread, that will read incoming messages from the server,
* as long as the client is connected to the server.
*/
void ServerListenerTask::run() {
    while(true){
        std::string answer = ch->translateMessage();
        std::cout << answer << std::endl;
        if (answer == "ACK 3") {
            //if the log out was successful --> tell the "Client Request Task" thread to terminate, and break from the loop
            this->ch->setLogoutStatus(TERMINATE);
            break;
        }
        else if(answer == "ERROR 3"){
            //if the logout didn't succeeded --> tell the "Client Request Task" thread to continue working.
            this->ch->setLogoutStatus(PROCEED);
        }
    }
}