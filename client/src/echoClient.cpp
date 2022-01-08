#include <iostream>
#include <stdlib.h>
#include <thread>
#include <condition_variable>
#include "../include/connectionHandler.h"
#include <boost/algorithm/string.hpp>




std::condition_variable condition;


class KeyboardReader {



private: ConnectionHandler *handler;
    bool &terminate;
    std::mutex& readLock;
public: KeyboardReader(ConnectionHandler* _handler, bool &_terminate, std::mutex& _readLock):handler(_handler), terminate(_terminate), readLock(_readLock){};

    //T2
    void run() {
        while (!terminate) {
            std::string input;
            std::getline(std::cin, input);
            if (input == "LOGOUT") {
                logoutCheck(input);
            }

            else if (!handler->sendLine(input)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                terminate=true;
            }
        }

        std::cout << "T2 end" << std::endl;
    }
    void logoutCheck(std::string input){
        std::unique_lock <std::mutex> lock(readLock);
        if (!handler->sendLine(input)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            terminate=true;
            return;
        }
        condition.wait(lock); //waits for update from the main thread
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
    std::mutex readLock;
    bool terminate = false;
    KeyboardReader reader(&connectionHandler, terminate, readLock);
    std::thread T2(&KeyboardReader::run,&reader);

    std::cout << "Welcome! Please register if you're not yet registered, otherwise please login to your account." << std::endl;

    while(!terminate){
        std::string answer;
        connectionHandler.getLine(answer);

        std::cout << answer << std::endl;
        if (answer == "ACK 3" || answer == "ERROR 3") {
            std::lock_guard<std::mutex> lock(readLock);
            if (answer == "ACK 3")
                terminate = true;
            condition.notify_all();
        }

    }

    T2.join();
    std::cout << "Main end" << std::endl;
    return 0;
}
