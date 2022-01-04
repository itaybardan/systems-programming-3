#include <iostream>
#include <stdlib.h>
#include <thread>
#include "../include/connectionHandler.h"
#include <boost/algorithm/string.hpp>


std::mutex readLock;
std::condition_variable condition;

class KeyboardReader {

    private: ConnectionHandler *handler;
             bool &terminate;
    public: KeyboardReader(ConnectionHandler* _handler, bool &_terminate):handler(_handler), terminate(_terminate){};

            //T2
            void run() {
                 while (!terminate) {
                     std::cout << "Give message:" << std::endl;
                    std::string input;
                     std::getline(std::cin, input);
                     if (input == "LOGOUT") {
                         logoutCheck(input);
                         std::cout << terminate << std::endl;
                     }

                     else if (!handler->sendLine(input)) {
                           std::cout << "Disconnected. Exiting...\n" << std::endl;
                           terminate=true;
                     }
                 }

                std::cout << "T2 end" << std::endl;
            }
            void logoutCheck(std::string input){
                std::unique_lock <std::mutex> lock(readLock); //waits for update from the main thread
                if (!handler->sendLine(input)) {
                    std::cout << "Disconnected. Exiting...\n" << std::endl;
                    terminate=true;
                    return;
                }
                condition.wait(lock);
            }
};


//T1 - Main
int main(int argc, char *argv[]) {

    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    ConnectionHandler connectionHandler(host, port); //Building the connection handler via the host and port args
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    bool terminate = false;
    KeyboardReader reader(&connectionHandler, terminate);
    std::thread T2(&KeyboardReader::run,&reader);

    while(!terminate){
        std::string answer = connectionHandler.translateMessage(); //TODO change to getLine
        std::cout << answer << std::endl;
        if (answer == "ACK 3" || answer == "ERROR 3") {
                std::lock_guard<std::mutex> lock(readLock);
                if (answer == "ACK 3")
                    terminate = true;
                condition.notify_all();
        }

    }

    std::cout << "Main end" << std::endl;
    T2.join();
    return 0;
}