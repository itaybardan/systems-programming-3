//
// Created by tomer on 02/01/19.
//

#ifndef BOOST_ECHO_CLIENT_ENCODERDECODER_H
#define BOOST_ECHO_CLIENT_ENCODERDECODER_H

#include <string>
#include <vector>
#include <map>

/**
 * Enum to identify each Opcode
 */
enum Opcode{
    REGISTER = 1,
    LOGIN = 2,
    LOGOUT = 3,
    FOLLOW = 4,
    POST = 5,
    PM = 6,
    USERLIST = 7,
    STAT = 8,
    NOTIFICATION = 9,
    ACK = 10,
    ERROR = 11,
    BLOCK = 12
};

/**
 * Class In Charge of Encoding messages to The server and Decoding messages from the server
 */
class EncoderDecoder{

    public:
        /**
         * Default constructor.
         */
        EncoderDecoder();

       /**
        * Initialising the delimiter, and the values of the Messages's opcodes.
        */
        void init();

        /**
         * default Destructor.
         */
        ~EncoderDecoder() = default;

        /**
         * Convert the input string from the client to a char array to send to the server.
         * @param input         String represent the user input.
         * @return              Char* represent a bytes array to send to the server to process.
         */
        std::vector<char> stringToMessage(std::string input);

        /**
         * Converting Char array to a Short number.
         * @param bytesArr              Char array to convert.
         * @return        Short number that is the bytes value of what was in the bytes array.
         */
        short bytesToShort(char* bytesArr);

        /**
         * Converting a short number to array of chars.
         * @param num               short number to convert.
         * @param bytesArr          Char array to put the number into.
         */
        void shortToBytes(short num, char bytesArr[]);

    private:

        //region Fields

        /**
         * Map of string as keys, and short as values, to connect between type of user request to it's matching OpCode.
         */
        std::map<std::string,short> commandDictionary;
        /**
         * Char of '0' used as delimiter in the communication between the client and the server.
         */
        char zeroDelimiter;

        //endregion Fields

        //region Encoding Functions
        /**
        * Part of the "stringToMessage" Function.
        * Decides how to convert the user input according to the opcode of the message
        * @param input             String represents the user input
        * @param ch_Opcode         Char array represents opcode as an array
        * @param output            Vector of chars reprsents the vector to send to the server as an array.
        * @param opcode            Short represent the opcode as a number
        * @return                  Vector of chars which represents the user input as a byte array to the server.
        */
    std::vector<char> & convertingToMessageByType(std::string &input, char *ch_Opcode, std::vector<char> &output, short opcode);

        /**
         * Part of the StringToMessage function
         * when the message is identified as a Login or Register request -->
         * processing the rest of the string by the user as a Login or Register (according to the OpCode) Message Type.
         * @param input                 String represent the input that was entered by the user.
         * @param ch_Opcode             char array represents the Opcode of this message.
         * @return      Char Array that represents the final Login or Register message
         */
        void registerAndLoginToMessage(std::string input, char *ch_Opcode, std::vector<char> &output);
        /**
         * Part of the StringToMessage function
         * when the message is identified as a follow request --> processing the rest of the string by the user as a Follow Message Type.
         * @param input                 String represent the input that was entered by the user.
         * @param ch_Opcode             char array represents the Opcode of this message.
         * @return      Char Array that represents the final follow message
         */
        void followToMessage(std::string input, char *ch_Opcode, std::vector<char> &output);

        /**
         * Part of the "followToMessage" Function
         * Inserting all the encoded data to the output vector.
         * @param ch_Opcode                 Char Array that represents the opcode of the follow message
         * @param output                    Vector of chars represents the vector to send to the server as array
         * @param yesOrNo                   Char represent whether the user wants to follow someone or not
         * @param ch_numberOfUsers          Char Array that represents the number of user the client wants to follow \ unfollow
         * @param names                     Vector of strings represents the names to follow or unfollow
         */
        void followInsertingDataToOutput(char *ch_Opcode, std::vector<char> &output, char yesOrNo, char *ch_numberOfUsers,
                                     std::vector<std::string> &names);

        /**
         * Part of the StringToMessage function
         * when the message is identified as a Stat or Post request -->
         * processing the rest of the string by the user as a PM or Stat (according to the OpCode) Message Type.
         * @param input                 String represent the input that was entered by the user.
         * @param ch_Opcode             char array represents the Opcode of this message.
         * @return      Char Array that represents the final PM or Stat message
         */
        void postOrStatToMessage(std::string input, char *ch_Opcode, std::vector<char> &output);
        /**
         * Part of the StringToMessage function
         * when the message is identified as a PM request --> processing the rest of the string by the user as a PM Message Type.
         * @param input                 String represent the input that was entered by the user.
         * @param ch_Opcode             char array represents the Opcode of this message.
         * @return      Char Array that represents the final PM message
         */
         void pmToMessage(std::string input, char *ch_Opcode, std::vector<char> &output);

         void blockToMessage(std::string input, char *ch_Opcode, std::vector<char> &output);
        //endregion Encoding Functions

};



#endif //BOOST_ECHO_CLIENT_ENCODERDECODER_H
