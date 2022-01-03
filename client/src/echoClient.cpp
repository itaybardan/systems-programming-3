//
// Created by tomer on 01/01/19.
//

#include <stdlib.h>
#include <iostream>
#include <thread>
#include <boost/thread.hpp>
#include "ConnectionHandler.h"
#include <boost/algorithm/string.hpp>
#include <ClientRequestTask.h>
#include <ServerListenerTask.h>

using namespace boost;
using namespace std;
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;



int main(int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    auto port = (short)atoi(argv[2]);
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    //initialising IO Thread
    ClientRequestTask clientRequestTask(&connectionHandler);
    std::thread clientRequestTaskThread(&ClientRequestTask::run,&clientRequestTask);
    //initialising communication thread
    ServerListenerTask serverListenerTask(&connectionHandler);
    std::thread serverListenerTaskThread(&ServerListenerTask::run, &serverListenerTask);
    //terminating threads
    clientRequestTaskThread.join();
    serverListenerTaskThread.join();
    return 0;
}