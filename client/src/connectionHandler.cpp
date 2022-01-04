#include <../include/connectionHandler.h>
 
using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
using std::vector;

/**
 * Default constructor
 */
ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(),
socket_(io_service_),endDec(),currentLogoutStatus(PENDING){
    //initialising the values of the encoder decoder
    this->endDec.init();
}
/**
* Default destructor
*/
ConnectionHandler::~ConnectionHandler() {
    close();
}
 
bool ConnectionHandler::connect() {
    std::cout << "Starting connect to " 
        << host_ << ":" << port_ << std::endl;
    try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
//region Getters&Setters

/**
 * Return the current logout status
 * @return      LogoutStatus Enum represents whether the communication with the server is still going, pending, or terminated
 */
LogoutStatus ConnectionHandler::getLogoutStatus() {
    return this->currentLogoutStatus;
}

/**
 * Setting the logoutStatus of this ConnectionHandler
 * @param newStatus             LogoutStatus enum to set
 */
void ConnectionHandler::setLogoutStatus(LogoutStatus newStatus) {
    this->currentLogoutStatus = newStatus;
}

//endregion Getters&Setters
 
bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
			tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);

        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character.
    // Notice that the null character is not appended to the frame string.
    try {
		do{
			getBytes(&ch, 1);
            frame.append(1, ch);
        }while (delimiter != ch);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

//region translating message from server functions
/**
 * Translating the incoming message from the server to string
 * @return          String representation of the message received by the server
 */
std::string ConnectionHandler::translateMessage() {
    std::string output;
    char ch;
    //getting the first two bytes --> the opcode of the message
    std::vector<char> message;
    for(int i = 0;i < 2; i++){
        getBytes(&ch,1);
        message.push_back(ch);
    }
    char ch_tempArray[2] = {message[0],message[1]};
    short opcode = this->endDec.bytesToShort(ch_tempArray);
    //opcode could be of a ACK , ERROR or Notification
    if(opcode == ACK){
        return translatingAckMessage(output, ch, message, ch_tempArray, opcode);
    }
    else if(opcode == ERROR){
        return translatingErrorMessage(output, ch, message, ch_tempArray, opcode);
    }
    else{
        //notification case
        return translatingNotificationMessage(output, ch);
    }
}

/**
 * Part of the "translateMessage" function.
 * Translating a Ack message that was received from the server to string.
 * @param output                        String to return to the client screen.
 * @param ch                            Char to use to read one char at a time from the server.
 * @param message                       Vector of chars represent the chars that were read from the server so far.
 * @param ch_tempArray                  Char array to translate to short numbers.
 * @param opcode                        opcode of the message.
 * @return                  String representation of the ACK message that was received by the server.
 */
string ConnectionHandler::translatingAckMessage(string &output, char &ch, vector<char> &message, char *ch_tempArray,
                                                short opcode) {
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    //getting resolved opcode
    ch_tempArray[0] = message[2];
    ch_tempArray[1] = message[3];
    opcode = endDec.bytesToShort(ch_tempArray);
    if(opcode == FOLLOW || opcode == USERLIST){
        translateACKFollowOrUserList(output, ch, message, ch_tempArray, opcode);
        return output;
    }
    else if(opcode == STAT){
        return translatingAckStatMessage(output, ch, message, ch_tempArray);
    }
    else{
     //in case it's one of the following: 1.Register | 2.Login | 3.Logout | 4.Post | 5.Pm
    return translatingGeneralAckMessage(output, opcode);
    }
}

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
string ConnectionHandler::translatingAckStatMessage(string &output, char &ch, vector<char> &message, char *ch_tempArray) {
    output.append("ACK 8 ");
    //getting the next two bytes --> number of posts
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[4];
    ch_tempArray[1] = message[5];
    output.append(std::to_string(endDec.bytesToShort(ch_tempArray)));
    output.append(" ");
    //getting the next two bytes --> number of followers
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[6];
    ch_tempArray[1] = message[7];
    output.append(std::to_string(endDec.bytesToShort(ch_tempArray)));
    output.append(" ");
    //getting the next two bytes --> number of following
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[8];
    ch_tempArray[1] = message[9];
    output.append(std::to_string(endDec.bytesToShort(ch_tempArray)));
    return output;
}

/**
 * Part of the "translateMessage" Function.
 * converting the incoming message from the server to a string to display to the user,
 * when the message is ACK follow or ACK userList
 *
 * @param output                String to return to the client screen
 * @param ch                    Char to read each byte
 * @param message               Vector of chars represents the message that was received from the server
 * @param ch_tempArray          Char array used to convert chars to short number
 * @param opcode                Short represent the opcode of the received message
 */
void ConnectionHandler::translateACKFollowOrUserList(string &output, char &ch, vector<char> &message,
                                                     char *ch_tempArray, short opcode) {
    //adding the ack with matching opcode
    output.append("ACK ");
    if(opcode == FOLLOW){
        output.append("4 ");
    }
    else{
        output.append("7 ");
    }
    //getting the next two bytes to see how many names to read
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[4];
    ch_tempArray[1] = message[5];
    short numberOfUsers = endDec.bytesToShort(ch_tempArray);
    //adding number of users
    output.append(std::to_string(numberOfUsers));
    //getting all the users
    for(int i = 0; i < numberOfUsers; i++){
        //adding each name to the string
        output.append(" ");
        string currentName;
        getFrameAscii(currentName, '\0');
        output.append(currentName.substr(0,currentName.length()-1));
    }
}

/**
 * Part of the "translatingAckMessage".
 * Translating ack message of register,login,logout,post,pm
 * @param output                        String to return to the client screen.
 * @param opcode                        Char to use to read one char at a time from the server.
 * @return                  String representation of the Ack Message that was sent by the server.
 */
string ConnectionHandler::translatingGeneralAckMessage(string &output, short opcode) const {
    output.append("ACK ");
    output.append(std::to_string(opcode));
    return output;
}

/**
 * Part of the "translatingMessage" function.
 * translating a Error Message that was received by the server to string
 * @param output                        String to return to the client screen.
 * @param ch                            Char to use to read one char at a time from the server.
 * @param message                       Vector of chars represent the chars that were read from the server so far.
 * @param ch_tempArray                  Char array to translate to short numbers.
 * @param opcode                        Opcode of the message.
 * @return                 String representation of the Error message that was received by the server.
 */
string ConnectionHandler::translatingErrorMessage(string &output, char &ch, vector<char> &message, char *ch_tempArray,
                                                  short opcode) {
    output.append("ERROR ");
    for(int i = 0; i < 2; i++){
            getBytes(&ch, 1);
            message.push_back(ch);
        }
    //getting resolved opcode
    ch_tempArray[0] = message[2];
    ch_tempArray[1] = message[3];
    opcode = endDec.bytesToShort(ch_tempArray);
    output.append(std::to_string(opcode));
    return output;
}

/**
 * Part of the "translatingMessage" function.
 * translating a notification message from the server to String
 * @param output                String representation of the message that was received by the server
 * @param ch                    Char to use to read from the server.
 * @return              String represents the Notification Message that was sent from the server.
 */
string ConnectionHandler::translatingNotificationMessage(string &output, char &ch) {
    output.append("NOTIFICATION ");
    getBytes(&ch, 1);
    //adding type of Notification
    output.append(ch == '\0' ? "PM " : "Public ");
    string postingUser;
    getFrameAscii(postingUser, '\0');
    //adding posting userName
    output.append(postingUser.substr(0,postingUser.length()-1));
    output.append(" ");
    string content;
    getFrameAscii(content, '\0');
    //adding the content of the notification
    output.append(content.substr(0,content.length()-1));
    return output;
}
//endregion Translate Message From Server Functions


/**
 * Send the user request to the server, as bytes array according to server protocol
 * @param userInput                     String represent the user input
 * @return              true if the message was sent successfully , false otherwise.
 */
bool ConnectionHandler::sendUserInput(std::string userInput){
    std::vector<char> toConvert = this->endDec.stringToMessage(userInput);
    toConvert.shrink_to_fit();
    char toSend[toConvert.size()];
    for(unsigned long i = 0 ; i <toConvert.size(); i++){
        toSend[i] = toConvert[i];
    }
    return sendBytes(toSend, (int)toConvert.size());
}


 
// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}






