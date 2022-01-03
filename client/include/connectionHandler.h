#ifndef CONNECTION_HANDLER__
#define CONNECTION_HANDLER__
                                           
#include <string>
#include <map>
#include <iostream>
#include <boost/asio.hpp>
#include "EncoderDecoder.h"

using boost::asio::ip::tcp;

/**
 * Enum for Logout current status
 */
enum LogoutStatus{

    /**
     * PROCEED --> The logout was not successful, threads should keep running.
     */
    PROCEED = 1,
    /**
     *TERMINATE --> The logout was successful , thread should terminate.
     */
    TERMINATE = -1,
    /**
     * PENDING --> logout status is still unknown
     */
    PENDING = 0
};


class ConnectionHandler {
private:

	const std::string host_;
	const short port_;
	boost::asio::io_service io_service_;   // Provides core I/O functionality
	tcp::socket socket_;
	/**
	 * EncoderDecoder Object to to help translating from \ to the server
	 */
	EncoderDecoder endDec;
	/**
	 * LogoutStatus rnum for Logout procedures
	 */
    LogoutStatus currentLogoutStatus;
 
public:
    ConnectionHandler(std::string host, short port);
    virtual ~ConnectionHandler();
 
    // Connect to the remote machine
    bool connect();

    //region Getters&Setters
   /**
    * Return the current logout status
    * @return      LogoutStatus Enum represents whether the communication with the server is still going, pending, or terminated
    */
    LogoutStatus getLogoutStatus();

    /**
     * Setting the logoutStatus of this ConnectionHandler
     * @param newStatus             LogoutStatus enum to set
     */
    void setLogoutStatus(LogoutStatus newStatus);

    //endregion Getters&Setters

    // Read a fixed number of bytes from the server - blocking.
    // Returns false in case the connection is closed before bytesToRead bytes can be read.
    bool getBytes(char bytes[], unsigned int bytesToRead);
 
	// Send a fixed number of bytes from the client - blocking.
    // Returns false in case the connection is closed before all the data is sent.
    bool sendBytes(const char bytes[], int bytesToWrite);

    // Get Ascii data from the server until the delimiter character
    // Returns false in case connection closed before null can be read.
    bool getFrameAscii(std::string& frame, char delimiter);

    // Close down the connection properly.
    void close();

    //region Translate Message From Server Functions
    /**
     * Translating the incoming message from the server to string
     * @return          String representation of the message recieved by the server
     */
    std::string translateMessage();

    /**
     * Part of the "translateMessage" function.
     * Translating a Ack message that was recieved from the server to string.
     * @param output                        String to return to the client screen.
     * @param ch                            Char to use to read one char at a time from the server.
     * @param message                       Vector of chars represent the chars that were read from the server so far.
     * @param ch_tempArray                  Char array to translate to short numbers.
     * @param opcode                        opcode of the message.
     * @return                  String representation of the ACK message that was recieved by the server.
     */
    std::string translatingAckMessage(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray, short opcode);

    /**
     * Part of the "translateMessage" Function.
     * converting the incoming message from the server to a string to display to the user,
     * when the message is ACK follow or ACK userList
     *
     * @param output                String to return to the client screen
     * @param ch                    Char to read each byte
     * @param message               Vector of chars represents the message that was recieved from the server
     * @param ch_tempArray          Char array used to convert chars to short number
     * @param opcode                Short represent the opcode of the recived message
     */
    void translateACKFollowOrUserList(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray,
                                      short opcode);

    /**
     * Part Of the "translatingAckMessage".
     * translating the stat Ack message that was recieved from the server
     *
     * @param output                        String to return to the client screen.
     * @param ch                            Char to use to read one char at a time from the server.
     * @param message                       Vector of chars represent the chars that were read from the server so far.
     * @param ch_tempArray                  Char array to translate to short numbers.
     * @return              String represents the Stat Ack message that was sent by the server
     */
    std::string translatingAckStatMessage(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray);


    /**
     * Part of the "translatingAckMessage".
     * Translating ack message of register,login,logout,post,pm
     * @param output                        String to return to the client screen.
     * @param opcode                        Char to use to read one char at a time from the server.
     * @return                  String representation of the Ack Message that was sent by the server.
     */
    std::string translatingGeneralAckMessage(std::string &output, short opcode) const;

    /**
     * Part of the "translatingMessage" function.
     * translating a notification message from the server to String
     * @param output                String representation of the message that was recieved by the server
     * @param ch                    Char to use to read from the server.
     * @return              String represents the Notification Message that was sent from the server.
     */
    std::string translatingNotificationMessage(std::string &output, char &ch);

    /**
     * Part of the "translatingMessage" function.
     * translating a Error Message that was recieved by the server to string
     * @param output                        String to return to the client screen.
     * @param ch                            Char to use to read one char at a time from the server.
     * @param message                       Vector of chars represent the chars that were read from the server so far.
     * @param ch_tempArray                  Char array to translate to short numbers.
     * @param opcode                        Opcode of the message.
     * @return                 String representation of the Error message that was recieved by the server.
     */
    std::string translatingErrorMessage(std::string &output, char &ch, std::vector<char> &message, char *ch_tempArray, short opcode);

    //endregion Translate Message From Server Functions

    /**
     * Send the user request to the server, as bytes array according to server protocol
     * @param userInput                     String represent the user input
     * @return              true if the message was sent successfully , false otherwise.
     */
    bool sendUserInput(std::string userInput);
}; //class ConnectionHandler
 
#endif