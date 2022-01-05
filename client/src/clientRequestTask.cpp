#include <boost/algorithm/string.hpp>
#include "../include/ClientRequestTask.h"

/**
* Default constructor.
* @param connectionHandler     ConnectionHandler to handler user requests.
*/
ClientRequestTask::ClientRequestTask(ConnectionHandler *connectionHandler) : ch(connectionHandler) {}

/**
* Run method to run in a separate thread, that will read user input from the keyboard and send it to the server,
* as long as the client is connected to the server.
*/
void ClientRequestTask::run() {
    while (true) {
        std::string userRequest;
        std::getline(std::cin, userRequest);
        if (!ch->sendUserInput(userRequest)) {
            //if there was a connection error --> exiting the program
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        if ((boost::to_upper_copy<std::string>(userRequest).compare("LOGOUT")) == 0) {
            //if the user requested to logout
            while (this->ch->getLogoutStatus() == PENDING) {
                //busy wait for logout result --> ACK or ERROR
            }
            if (this->ch->getLogoutStatus() == PROCEED) {
                //the logout was not successful --> continue reading from keyboard
                this->ch->setLogoutStatus(PENDING);
            } else {
                //if the logout was successful --> break from the loop
                break;
            }
        }
    }
}