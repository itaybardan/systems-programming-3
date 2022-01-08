#include <../include/connectionHandler.h>


using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
using std::vector;


ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(),
socket_(io_service_){}

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

short ConnectionHandler::bytesToShort(char *bytesArr) {
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

void ConnectionHandler::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short ConnectionHandler::getOpCode(string inputType) {
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

bool ConnectionHandler::sendLine(std::string& line) { //TODO CHANGE JAVA CODE TO ACCOMMODATE THE DELIMITER (';') CORRECTLY
    return sendFrameAscii(line, '\0');

}

void ConnectionHandler::encodeMessage(std::string &message, short opcode){
    switch(opcode){
        case(1):
        case(2): std::replace(message.begin(), message.end() , ' ', '\0');
            break;
        case(3): message.clear();
            break;
        case(4):
            break;
        case(6): {
            int index = message.find(" ");
            message[index] = '\0';
        }
            break;
        case(7): message.clear();
            break;
        case(8):message = message + '|';
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
        char* follow = new char[1];
        if(frame[0] == '0') follow[0] = 0;
        else follow[0] = 1;
        bool result1v2 = sendBytes(follow, 1);
        delete[] follow;
        if(!result1v2) return false;
        frame = frame.substr(2);
    }


    bool result2=sendBytes(frame.c_str(),frame.length()); //sending the rest of the frame
    if(!result2) return false;

    if(opcode == 3 || opcode==7) return true;
    sendBytes(&delimiter,1);
    delimiter = ';';
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
bool ConnectionHandler::getLine(std::string &output) {

    char ch;
    std::vector<char> message;
    for(int i = 0;i < 2; i++){ //opcode
        getBytes(&ch,1);
        message.push_back(ch);
    }

    char ch_tempArray[2] = {message[0],message[1]};
    short opcode = bytesToShort(ch_tempArray);


    if(opcode == 10){
        getMessageAck(output, ch, message, ch_tempArray);
    }
    else if(opcode == 11){
        getErrorAck(output, ch, message, ch_tempArray, opcode);
    }
    else{//notification
        getNotifyAck(output, ch);
    }
    return true;
}

string ConnectionHandler::getMessageAck(string &output, char &ch, vector<char> &message, char *temp) {

    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    //getting resolved opcode
    temp[0] = message[2];
    temp[1] = message[3];
    short opcode = bytesToShort(temp);
    output.append("ACK " + std::to_string(opcode));
    if(opcode == 4){
        return getFollowAck(output, ch, message);;
    }
    else if(opcode ==7){
        getLogstatAck(output, ch, message, temp);
        return output;
    }
    else if(opcode == 8){
        getStatAck(output, ch, message, temp);
        return output;
    }
    else{
     //for Register, Login, Logout, Post, Pm
    return output;
    }
}

string ConnectionHandler::getFollowAck(string &output, char &ch, vector<char> &message){

    string loggedIn;
    getFrameAscii(loggedIn, '\0');
    output.append(" " + loggedIn.substr(0, loggedIn.length() - 1));
    return output;
    }

void ConnectionHandler::getLogstatAck(string &output, char &ch, vector<char> &message,char *temp) {
    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    temp[0] = message[4];
    temp[1] = message[5];
    short numberOfUsers = bytesToShort(temp);

    char* numberToProcess= new char[2];
    for(int i = 0; i < numberOfUsers; i++){
        output.append(" ");


        string currentName;
        for (int j = 0; j < 4; ++j) {
            getBytes(numberToProcess, 2);
            short num = bytesToShort(numberToProcess);
            currentName = currentName + std::to_string(num) + " ";
        }
        output.append(currentName);
        getBytes(temp, 1);
    }
    delete[] numberToProcess;
}

void ConnectionHandler::getStatAck(string &output, char &ch, vector<char> &message, char *temp) {

    for(int i = 0; i < 2; i++){
        getBytes(&ch, 1);
        message.push_back(ch);
    }
    temp[0] = message[4];
    temp[1] = message[5];
    short numberOfUsers = bytesToShort(temp);

    char* numberToProcess= new char[2];
    for(int i = 0; i < numberOfUsers; i++){
        output.append(" ");

        string currentName;
        for (int j = 0; j < 4; ++j) {
            getBytes(numberToProcess, 2);
            short num = bytesToShort(numberToProcess);
            currentName = currentName + std::to_string(num) + " ";
        }
        output.append(currentName);
        getBytes(temp, 1);
    }
    delete[] numberToProcess;
}

string ConnectionHandler::getNotifyAck(string &output, char &ch) {
    output.append("NOTIFICATION ");
    getBytes(&ch, 1);

    output.append(ch == '\0' ? "PM " : "Public ");
    string postingUser;
    getFrameAscii(postingUser, '\0');

    output.append(postingUser.substr(0,postingUser.length()-1));
    output.append(" ");
    string content;
    getFrameAscii(content, '\0');

    output.append(content.substr(0,content.length()-1));
    return output;
}

string ConnectionHandler::getErrorAck(string &output, char &ch, vector<char> &message, char *ch_tempArray,
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
