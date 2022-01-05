#include <utility>

//
// Created by tomer on 02/01/19.
//

#include <../include/EncoderDecoder.h>
#include <boost/algorithm/string.hpp>
#include <locale>

using namespace std;


EncoderDecoder::EncoderDecoder() : commandDictionary(), zeroDelimiter('\0') {}

/**
 * Initialising the delimiter, and the values of the Messages's opcodes
 */
void EncoderDecoder::init() {
    //Register request = 1
    this->commandDictionary.insert(std::pair<string, short>("REGISTER", REGISTER));
    //Login request = 2
    this->commandDictionary.insert(std::pair<string, short>("LOGIN", LOGIN));
    //Logout request = 3
    this->commandDictionary.insert(std::pair<string, short>("LOGOUT", LOGOUT));
    //Follow request = 4
    this->commandDictionary.insert(std::pair<string, short>("FOLLOW", FOLLOW));
    //Post request = 5
    this->commandDictionary.insert(std::pair<string, short>("POST", POST));
    //PM request = 6
    this->commandDictionary.insert(std::pair<string, short>("PM", PM));
    //UserList request = 7
    this->commandDictionary.insert(std::pair<string, short>("USERLIST", USERLIST));
    //Stat request = 8
    this->commandDictionary.insert(std::pair<string, short>("STAT", STAT));

    // BLOCK request = 12
    this->commandDictionary.insert(std::pair<string, short>("BLOCK", BLOCK));

    this->zeroDelimiter = '\0';
}

//region Encoding Functions

/**
* Converting a short number to array of chars
* @param num               short number to convert
* @param bytesArr          Char array to put the number into
*/
void EncoderDecoder::shortToBytes(short num, char bytesArr[]) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}


/**
* Convert the input string from the client to a char array to send to the server.
* @param input         String represent the user input
* @return              Char* represent a bytes array to send to the server to process
*/
std::vector<char> EncoderDecoder::stringToMessage(std::string input) {
    char ch_Opcode[2];
    std::vector<char> output;
    // taking the first word of the sentence to process which kind of request it is from the user
    std::string command = boost::to_upper_copy<std::string>(input.substr(0, input.find_first_of(" ")));
    input = input.substr(input.find_first_of(" ") + 1);
    //translating the first word to a Opcode using the commandDictionary.
    short opcode = this->commandDictionary.at(command);
    this->shortToBytes(opcode, ch_Opcode);
    output = convertingToMessageByType(input, ch_Opcode, output, opcode);
    return output;
}

/**
 * Part of the "stringToMessage" Function.
 * Decides how to convert the user input according to the opcode of the message
 * @param input             String represents the user input
 * @param ch_Opcode         Char array represents opcode as an array
 * @param output            Vector of chars reprsents the vector to send to the server as an array.
 * @param opcode            Short represent the opcode as a number
 * @return                  Vector of chars which represents the user input as a byte array to the server.
 */
vector<char> &
EncoderDecoder::convertingToMessageByType(string &input, char *ch_Opcode, vector<char> &output, short opcode) {
    switch (opcode) {
        case REGISTER:
            registerAndLoginToMessage(input, ch_Opcode, output);
            break;
        case LOGIN:
            registerAndLoginToMessage(input, ch_Opcode, output);
            break;
        case LOGOUT:
            output.push_back(ch_Opcode[0]);
            output.push_back(ch_Opcode[1]);
            break;
        case FOLLOW:
            followToMessage(input, ch_Opcode, output);
            break;
        case POST:
            postOrStatToMessage(input, ch_Opcode, output);
            break;
        case PM:
            pmToMessage(input, ch_Opcode, output);
            break;
        case USERLIST:
            output.push_back(ch_Opcode[0]);
            output.push_back(ch_Opcode[1]);
            break;
        case BLOCK:
            blockToMessage(input, ch_Opcode, output);
            break;
        default:
            //stat case
            postOrStatToMessage(input, ch_Opcode, output);
            break;
    }
    return output;
}

/**
 * Part of the StringToMessage function
 * when the message is identified as a Login or Register request -->
 * processing the rest of the string by the user as a Login or Register (according to the OpCode) Message Type.
 * @param input                 String represent the input that was entered by the user.
 * @param ch_Opcode             char array represents the Opcode of this message.
 * @return      Char Array that represents the final Login or Register message
 */
void EncoderDecoder::registerAndLoginToMessage(std::string input, char *ch_Opcode, std::vector<char> &output) {
    //register case
    std::string userName(input.substr(0, input.find_first_of(" ")));
    input = input.substr(input.find_first_of(" ") + 1);
    std::string password(input.substr(0, input.find_first_of(" ")));
    //creating temp char* in the size of the opcode,username and password
    output.push_back(ch_Opcode[0]);
    output.push_back(ch_Opcode[1]);
    for (char i: userName) {
        //inserting all the chars of the user name
        output.push_back(i);
    }
    output.push_back(this->zeroDelimiter);
    for (char i: password) {
        //inserting all the chars from the password
        output.push_back(i);
    }
    output.push_back(this->zeroDelimiter);
}

/**
 * Part of the StringToMessage function
 * when the message is identified as a follow request --> processing the rest of the string by the user as a Follow Message Type.
 * @param input                 String represent the input that was entered by the user.
 * @param ch_Opcode             char array represents the Opcode of this message.
 * @return      Char Array that represents the final follow message
 */
void EncoderDecoder::followToMessage(std::string input, char *ch_Opcode, std::vector<char> &output) {
    char yesOrNo;
    //getting the "follow or not follow" from the string
    std::string followOrNot = input.substr(0, input.find_first_of(" "));
    input = input.substr(input.find_first_of(" ") + 1);
    if (followOrNot == "0") {
        //follow case
        yesOrNo = 0;
    } else {
        //unfollow case
        yesOrNo = 1;
    }
    //taking the number of users in the list from the user input
    //short numberOfUsers = (short)std::stoi(input.substr(0,input.find_first_of(" ")));
    //input = input.substr(input.find_first_of(" ") + 1);
    char ch_numberOfUsers[2];
    this->shortToBytes(1, ch_numberOfUsers);
    //creating a vector to hold the usernames to search in the server
    std::vector <string> names;
    int counter = 0;
    while (counter < 1) {
        //as long as there is still a user left to read --> adding it to the names vector
        std::string current = input.substr(0, input.find_first_of(" "));
        input = input.substr(input.find_first_of(" ") + 1);
        names.push_back(current);
        counter++;
    }
    followInsertingDataToOutput(ch_Opcode, output, yesOrNo, ch_numberOfUsers, names);
}

/**
* Part of the "followToMessage" Function
* Inserting all the encoded data to the output vector.
* @param ch_Opcode                 Char Array that represents the opcode of the follow message
* @param output                    Vector of chars represents the vector to send to the server as array
* @param yesOrNo                   Char represent whether the user wants to follow someone or not
* @param ch_numberOfUsers          Char Array that represents the number of user the client wants to follow \ unfollow
* @param names                     Vector of strings represents the names to follow or unfollow
*/
void
EncoderDecoder::followInsertingDataToOutput(char *ch_Opcode, vector<char> &output, char yesOrNo, char *ch_numberOfUsers,
                                            vector <string> &names) {

    //inserting the opCode
    output.push_back(ch_Opcode[0]);
    output.push_back(ch_Opcode[1]);
    //inserting the yesOrNo char
    output.push_back(yesOrNo);
    output.push_back(ch_numberOfUsers[0]);
    output.push_back(ch_numberOfUsers[1]);
    for (auto &name: names) {
        //for each name in the vector
        for (char j: name) {
            //inserting all the letters of the user
            output.push_back(j);
        }
        //after each name --> putting the '\0' delimiter.
        output.push_back(zeroDelimiter);
    }
}


/**
 * Part of the StringToMessage function
 * when the message is identified as a Stat or Post request -->
 * processing the rest of the string by the user as a PM or Stat (according to the OpCode) Message Type.
 * @param input                 String represent the input that was entered by the user.
 * @param ch_Opcode             char array represents the Opcode of this message.
 * @return      Char Array that represents the final PM or Stat message
 */
void EncoderDecoder::postOrStatToMessage(std::string input, char *ch_Opcode, std::vector<char> &output) {

    //inserting the opcode to the array
    output.push_back(ch_Opcode[0]);
    output.push_back(ch_Opcode[1]);
    //inserting:
    //1. the useranme if it's a Stat message
    //2. the content if it's a post
    for (char i: input) {
        output.push_back(i);
    }
    //adding the '\0' delimiter in the end of the message
    output.push_back(this->zeroDelimiter);
}

/**
 * Part of the StringToMessage function
 * when the message is identified as a Block request -->
 * @param input                 String represent the input that was entered by the user.
 * @param ch_Opcode             char array represents the Opcode of this message.
 * @return      Char Array that represents the final PM or Stat message
 */
void EncoderDecoder::blockToMessage(std::string input, char *ch_Opcode, std::vector<char> &output) {

    //inserting the opcode to the array
    output.push_back(ch_Opcode[0]);
    output.push_back(ch_Opcode[1]);
    //inserting the username
    for (char i: input) {
        output.push_back(i);
    }
    //adding the '\0' delimiter in the end of the message
    output.push_back(this->zeroDelimiter);
}

/**
 * Part of the StringToMessage function
 * when the message is identified as a PM request --> processing the rest of the string by the user as a PM Message Type.
 * @param input                 String represent the input that was entered by the user.
 * @param ch_Opcode             char array represents the Opcode of this message.
 * @return      Char Array that represents the final PM message
 */
void EncoderDecoder::pmToMessage(std::string input, char *ch_Opcode, std::vector<char> &output) {
    //getting the user name to search in the server
    std::string userName(input.substr(0, input.find_first_of(" ")));
    //creating the output array
    input = input.substr(input.find_first_of(" ") + 1);
    //inserting the opcode to the output array
    output.push_back(ch_Opcode[0]);
    output.push_back(ch_Opcode[1]);
    //inserting the user name to the array
    for (char i: userName) {
        output.push_back(i);
    }
    //adding '\0' delimiter between the username to the content of the message
    output.push_back(this->zeroDelimiter);
    //adding all the content of the message to the output array
    for (char i: input) {
        output.push_back(i);
    }
    //adding the '\0' delimiter
    output.push_back(this->zeroDelimiter);
}

//endregion Encoding Functions

//region Decoding Functions

/**
* Converting Char array to a Short number
* @param bytesArr              Char array to convert
* @return        Short number that is the bytes value of what was in the bytes array
*/
short EncoderDecoder::bytesToShort(char *bytesArr) {
    short result = (short) ((bytesArr[0] & 0xff) << 8);
    result += (short) (bytesArr[1] & 0xff);
    return result;
}

//endregion Decoding Functions