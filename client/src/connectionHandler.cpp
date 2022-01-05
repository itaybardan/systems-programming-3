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
socket_(io_service_){}
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

short bytesToShort(char *bytesArr) {
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

void shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}
short getOpCode(string inputType) {
    short opcode=11;
    if(inputType == "REGISTER") opcode=1;
    else if(inputType == "LOGIN") opcode=2;
    else if(inputType == "LOGOUT") opcode=3;
    else if(inputType == "FOLLOW") opcode=4;
    else if(inputType == "POST") opcode=5;
    else if(inputType == "PM") opcode=6;
    else if(inputType == "LOGSTAT") opcode=7;
    else if(inputType == "STAT") opcode=8;
    else if(inputType == "BLOCK") opcode=12;

    return opcode;
}

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

bool ConnectionHandler::getLine(std::string& line) {
    return getFrameAscii(line, ';');
}

bool ConnectionHandler::sendLine(std::string& line) { //TODO CHANGE JAVA CODE TO ACCOMMODATE THE DELIMITER (';') CORRECTLY
    return sendFrameAscii(line, '\0');
}

void encodeMessage(std::string &message, short opcode){
    switch(opcode){
        case(1): std::replace(message.begin(), message.end() , ' ', '\0');
            break;
        case(2): std::replace(message.begin(), message.end() , ' ', '\0');
            break;
        case(3): message.clear();
            break;
        case(4):
            break;
        case(6): {
            std::replace(message.begin(), message.end(), ' ', '\0');
            break;
        }
        case(7): message.clear();
            break;
        case(8): std::replace(message.begin(), message.end() , ' ', '|');
            break;
        case(12): std::replace(message.begin(), message.end(), ' ', '\0');
            break;
    }
}

bool ConnectionHandler::sendFrameAscii(std::string& frame, char delimiter) {
    short opcode = getOpCode(frame.substr(0,frame.find_first_of(" ")));
    char* opCodeToByte = new char[2];



    shortToBytes(opcode, opCodeToByte);
    bool result1= sendBytes(opCodeToByte, 2); //sending opcode
    delete[] opCodeToByte;
    if(!result1) return false;

    frame = frame.substr(frame.find_first_of(" ") + 1); //we removed the code type from the line
    encodeMessage(frame, opcode);


    if(opcode == 4) {
        opCodeToByte = new char[2];
        char* follow = new char[3];

        std::cout << frame[0] << std::endl;
        if(frame[0] == '0') follow[0] = 0;
        else follow[0] = 1;

        std::cout << follow[0] << std::endl;

        shortToBytes(1,opCodeToByte);
        follow[1] = opCodeToByte[0];
        follow[2] = opCodeToByte[1];
        bool result1v2 = sendBytes(follow, 3);
        delete[] opCodeToByte;
        delete[] follow;
        if(!result1v2) return false;
        frame = frame.substr(2);
    }


    bool result2=sendBytes(frame.c_str(),frame.length()); //sending the rest of the frame
    if(!result2) return false;

    if(opcode == 3 || opcode==7) return true; //TODO every post needs to end with delimiter ';'.
    return sendBytes(&delimiter,1);
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

void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
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
    short opcode = bytesToShort(ch_tempArray);
    //opcode could be of a ACK , ERROR or Notification
    if(opcode == 10){
        return translatingAckMessage(output, ch, message, ch_tempArray, opcode);
    }
    else if(opcode == 11){
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
    opcode = bytesToShort(ch_tempArray);
    if(opcode == 4 || opcode == 7){
        translateACKFollowOrLogstat(output, ch, message, ch_tempArray, opcode);
        return output;
    }
    else if(opcode == 8){
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
    output.append(std::to_string(bytesToShort(ch_tempArray)));
    output.append(" ");
    //getting the next two bytes --> number of followers
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[6];
    ch_tempArray[1] = message[7];
    output.append(std::to_string(bytesToShort(ch_tempArray)));
    output.append(" ");
    //getting the next two bytes --> number of following
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    ch_tempArray[0] = message[8];
    ch_tempArray[1] = message[9];
    output.append(std::to_string(bytesToShort(ch_tempArray)));
    return output;
}

/**
 * Part of the "translateMessage" Function.
 * converting the incoming message from the server to a string to display to the user,
 * when the message is ACK follow or ACK logstat.
 *
 * @param output                String to return to the client screen
 * @param ch                    Char to read each byte
 * @param message               Vector of chars represents the message that was received from the server
 * @param ch_tempArray          Char array used to convert chars to short number
 * @param opcode                Short represent the opcode of the received message
 */
void ConnectionHandler::translateACKFollowOrLogstat(string &output, char &ch, vector<char> &message,
                                                    char *ch_tempArray, short opcode) {
    //adding the ack with matching opcode
    output.append("ACK ");
    if(opcode == 4){
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
    short numberOfUsers = bytesToShort(ch_tempArray);
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
    opcode = bytesToShort(ch_tempArray);
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
