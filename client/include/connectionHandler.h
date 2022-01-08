#ifndef CONNECTION_HANDLER__
#define CONNECTION_HANDLER__
                                           
#include <string>
#include <iostream>
#include <boost/asio.hpp>
#include <map>

using boost::asio::ip::tcp;

class ConnectionHandler {
private:
    const std::string host_;
    const short port_;
    boost::asio::io_service io_service_;   // Provides core I/O functionality
    tcp::socket socket_;
public:
    ConnectionHandler(std::string host, short port);
    virtual ~ConnectionHandler();

    // Connect to the remote machine
    bool connect();

    // Read a fixed number of bytes from the server - blocking.
    // Returns false in case the connection is closed before bytesToRead bytes can be read.
    bool getBytes(char bytes[], unsigned int bytesToRead);

    // Send a fixed number of bytes from the client - blocking.
    // Returns false in case the connection is closed before all the data is sent.
    bool sendBytes(const char bytes[], int bytesToWrite);

    // Read an ascii line from the server
    // Returns false in case connection closed before a newline can be read.
    bool getLine(std::string& line);

    // Send an ascii line from the server
    // Returns false in case connection closed before all the data is sent.
    bool sendLine(std::string& line);

    // Get Ascii data from the server until the delimiter character
    // Returns false in case connection closed before null can be read.
    bool getFrameAscii(std::string& frame, char delimiter);

    // Send a message to the remote host.
    // Returns false in case connection is closed before all the data is sent.
    bool sendFrameAscii(std::string& frame, char delimiter);

    // Close down the connection properly.
    void close();

    void encodeMessage(std::string &message, short opcode);

    short getOpCode(std::string inputType);

    std::string getMessageAck(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray, short opcode);

    std::string getFollowAck(std::string &output, char &ch, std::vector<char> &message);

    void getLogstatAck(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray);

    void getStatAck(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray);

    std::string getOtherAck(std::string &output, short opcode) const;

    std::string getNotifyAck(std::string &output, char &ch);

    std::string getErrorAck(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray, short opcode);

    void shortToBytes(short num, char *bytesArr);

    short bytesToShort(char *bytesArr);

};
 
#endif
