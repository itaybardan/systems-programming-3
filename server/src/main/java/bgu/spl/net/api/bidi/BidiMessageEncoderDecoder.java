package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.bidi.Messages.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Vector;

/**
 * Encoder and Decoder for the bidi protocol.
 */
public class BidiMessageEncoderDecoder implements MessageEncoderDecoder<Message> {

    //region Fields
    /**
     * Bytes array represents the opcode as bytes.
     */
    private byte[] opcodeBytes;

    /**
     * Integer represents the length of the opcode that was already inserted to the output.
     */
    private int opcodeInsertedLength;

    /**
     * Bytes array represents one field, which is different in each message type, to be inserted to the output.
     */
    private byte[] field1;

    /**
     * Bytes array represents one field, which is different in each message type, to be inserted to the output.
     */
    private byte[] field2;

    /**
     * Integer represents the index of field 1.
     */
    private int field1Index;

    /**
     * Integer represents the index of field 2.
     */
    private int field2Index;

    /**
     * Integer represents the amount of zeroes that were already read.
     */
    private int zeroCounter;

    /**
     * Byte represents the indicator if following or unfollowing.
     */
    private byte followByte;

    /**
     * Opcode Enum represents the current opcode.
     */
    private Message.Opcode currentOpcode;
    //endregion Fields

    //Constructor
    public BidiMessageEncoderDecoder() {
        this.opcodeBytes = new byte[2];
        this.opcodeInsertedLength = 0;
        this.currentOpcode = null;
        this.field1 = new byte[10];
        this.field2 = new byte[10];
        this.field1Index = 0;
        this.field2Index = 0;
        this.zeroCounter = 0;
        this.followByte = 0;

    }

    /**
     * add the next byte to the decoding process
     *
     * @param nextByte the next byte to consider for the currently decoded
     * message
     * @return a message if this byte completes one or null if it doesnt.
     */
    @Override
    public Message decodeNextByte(byte nextByte) {
        if(this.opcodeInsertedLength == 0){
            this.opcodeBytes[0] = nextByte;
            this.opcodeInsertedLength++;
            return null;
        }
        else if(this.opcodeInsertedLength == 1){
            this.opcodeBytes[1] = nextByte;
            this.opcodeInsertedLength++;
            initMessageContentAndLength();
            if((this.currentOpcode == Message.Opcode.LOGOUT) ||
               (this.currentOpcode == Message.Opcode.USERLIST)){
                //The only opcodes who gets just the opcode in the command.
                if(this.currentOpcode== Message.Opcode.LOGOUT){
                    generalVariablesReset();
                    return new Logout();
                }
                else{
                    //The opcode is USERLIST.
                    generalVariablesReset();
                    return new UserList();
                }
            }
            return null;
        }
        else{
            //Reading and generating the rest of the message.
            return readingMessage(nextByte);
        }
    }

    /**
     * Generating a message according to the current opcode.
     * @param nextByte      Byte represents the next byte to decode.
     * @return              Message represents a message from the client.
     */
    private Message readingMessage(byte nextByte) {
        Message output;
        if(this.currentOpcode == Message.Opcode.REGISTER){
            output = readingRegisterOrLoginMessage(Message.Opcode.REGISTER,nextByte);
        }
        else if(this.currentOpcode == Message.Opcode.LOGIN){
            output = readingRegisterOrLoginMessage(Message.Opcode.LOGIN,nextByte);
        }
        else if(this.currentOpcode == Message.Opcode.FOLLOW){
            output = readingFollowMessage(nextByte);
        }
        else if(this.currentOpcode == Message.Opcode.POST){
            output = readingPostMessage(nextByte);
        }
        else if(this.currentOpcode == Message.Opcode.PM){
            output = readingPMMessage(nextByte);
        }
        else if(this.currentOpcode == Message.Opcode.STAT){
            output = readingStatMessage(nextByte);
        }
        else{
            //No other clients to server opcodes in the assignment specifications.
            output = null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to field1, and generating a Stat message by translating field1 to a string.
     * @param nextByte      Represents the next byte to be translated.
     * @return              Message which is a Stat message.
     */
    private Message readingStatMessage(byte nextByte) {
        // field1 = username
        Message output;
        if(nextByte == '\0'){
            checkReduceField1();
            String username = new String(this.field1, StandardCharsets.UTF_8);
            output = new Stat(username);
            this.generalVariablesReset();
        }
        else{
            insertByteToField1(nextByte);
            return null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "PM" command.
     * @param nextByte      Represents the next byte to be translated.
     * @return              Message which is a PM message.
     */
    private Message readingPMMessage(byte nextByte) {
        //field1 = username   | field2 = content
        Message output;
        if(this.zeroCounter == 0){
            //Inserting to username.
            if(nextByte == '\0'){
                this.zeroCounter++;
            }
            else{
                insertByteToField1(nextByte);
            }
            return null;
        }
        else{
            //Inserting content.
            if(nextByte == '\0'){
                output = generatePMMessage();
                this.generalVariablesReset();
            }
            else{
                insertByteToField2(nextByte);
                return null;
            }
        }
        return output;
    }

    /**
     * Generating "PM" message, by translating the arrays of bytes to strings.
     * @return                  Message which is a PM message.
     */
    private Message generatePMMessage() {
        Message output;//finished reading
        checkReduceField1();
        checkReduceField2();
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String content = new String(this.field2,StandardCharsets.UTF_8);
        output = new PM(username,content);
        return output;
    }

    /**
     * Reading bytes and inserting them to field1, and generating a Post message by translating field1 to a string.
     * @param nextByte      Represents the next byte to be translated.
     * @return              Message which is a Post message.
     */
    private Message readingPostMessage(byte nextByte) {
        //field1 = content
        Message output;
        if(nextByte == '\0'){
            //finished reading the message
            checkReduceField1();
            String content = new String(this.field1, StandardCharsets.UTF_8);
            output = new Post(content);
            this.generalVariablesReset();
        }
        else{
            this.insertByteToField1(nextByte);
            return null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "FOLLOW" command.
     * @param nextByte      Represents the next byte to be translated.
     * @return              Message represents the decoded message, or null if not done reading the message.
     */
    private Message readingFollowMessage(byte nextByte){
        //field1 = numberOfUsers  | field2 = usernameList | followbyte = follow \ unfollow | zerocounter = bytesCounter
        if(this.zeroCounter == 0){
            this.field1 = new byte[2];
            this.followByte = nextByte;
            this.zeroCounter++;
            return null;
        }
        else if((this.zeroCounter > 0) && (this.zeroCounter < 3)){
            //The amount of bytes that were read is 1 or 2 which means (according to the assignment notifications)
            // the next bytes are the number of users.
            insertToNumberOfUsers(nextByte);
            return null;
        }
        else{
            short numberOfUsers = Message.bytesToShort(this.field1);
            insertByteToField2(nextByte);
            if(nextByte == '\0'){
                // The current username ended.
                this.zeroCounter++;
                //to reduce the first three bytes of the follow \ unfollow and numberOfUsers bytes
                if(this.zeroCounter-3 == numberOfUsers){
                    return generateFollowMessage(numberOfUsers);
                }
                else{
                    return null;
                }
            }
            else{
                return null;
            }
        }
    }

    /**
     * Inserts the next byte (which represents the number of users) to field1 in place 0 or 1 according to the amount
     * of bytes that were read.
     * @param nextByte      Represents the byte that needs to be inserted.
     */
    private void insertToNumberOfUsers(byte nextByte){
        if(this.zeroCounter == 1){
            this.field1[0] = nextByte;
            this.zeroCounter++;
        }
        else{
            this.field1[1] = nextByte;
            this.zeroCounter++;
        }
    }

    /**
     * Generating "Follow" message, by translating the arrays of bytes to strings.
     * @param numberOfUsers     Represents the number of users.
     * @return                  Message which is a Follow message.
     */
    private Message generateFollowMessage(int numberOfUsers) {
        Message output;//collected all the necessary users --> convert them to string and generate a FollowMessage
        checkReduceField2();
        Vector<String> allUsers = new Vector<>();
        int start = 0;
        for(int i = 0; i < field2Index; i++){
            if(field2[i] == '\0'){
                //finished a single user;
                byte[] userByte = Arrays.copyOfRange(field2,start,i);
                String user = new String (userByte,StandardCharsets.UTF_8);
                allUsers.add(user);
                start = i + 1;
            }
        }
        output = new Follow(this.followByte, (short)numberOfUsers, allUsers);
        generalVariablesReset();
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "REGISTER" and "LOGIN" commands.
     * @param outputOpcode  Represents the current opcode of the command.
     * @param nextByte      Represents the next byte to be translated.
     * @return              Message represents the decoded message, or null if not done reading the message.
     */
    private Message readingRegisterOrLoginMessage(Message.Opcode outputOpcode, byte nextByte) {
        //Field1 = username | Field2 = password
        if(this.zeroCounter == 0){
            //The next byte going to be to the userName
            if(nextByte == '\0'){
                // The current username ended.
                checkReduceField1();
                this.zeroCounter++;
                return null;
            }
            insertByteToField1(nextByte);
            return null;
        }
        else{
            //The next byte is going to be to the password.
            if(nextByte == '\0'){
                checkReduceField2();
                //Creating the Register or Login Message
                return generateRegisterOrLoginMessage(outputOpcode);
            }
            insertByteToField2(nextByte);
            return null;
        }
    }

    /**
     * Generating "Register" or "Login" messages according to the current opcode, by translating the arrays of bytes
     * to strings.
     * @param outputOpcode      Represents the current opcode of the command.
     * @return                  Message which is a Register or Login message.
     */
    private Message generateRegisterOrLoginMessage(Message.Opcode outputOpcode) {
        Message output;
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String password = new String(this.field2, StandardCharsets.UTF_8);
        if(outputOpcode == Message.Opcode.REGISTER){
             output = new Register(username, password);
        }
        else{
            //The opcode is login.
            output = new Login(username, password);
        }
        generalVariablesReset();
        return output;
    }

    /**
     * Resets the fields to the original values. Belongs to "decodeNextByte" function.
     */
    private void generalVariablesReset() {
        this.opcodeBytes = new byte[2];
        this.currentOpcode = null;
        this.field1 = new byte[10];
        this.field2 = new byte[10];
        this.field1Index = 0;
        this.field2Index = 0;
        this.zeroCounter = 0;
        this.opcodeInsertedLength = 0;
    }

    /**
     * Initiates the current opcode. Belongs to "decodeNextByte" function.
     */
    private void initMessageContentAndLength() {
        this.currentOpcode = Message.convertToOpcode(Message.bytesToShort(this.opcodeBytes));
    }

    /**
     * Extends the length of the given array.
     * @param array     Represents the array that needs it's length to be extended.
     * @return          The array with the extended length.
     */
    private byte[] extendArray(byte[] array){
        int size = array.length;
        byte[] temp = new byte[size*2];
        for(int i = 0; i < size; i++){
            temp[i] = array[i];
        }
        return temp;
    }

    /**
     * Reduces the length of a given array to it's real size.
     * @param toReduce      Represents the array that needs to be down-sized.
     * @param realSize      Represents the size the array should be down-sized to.
     * @return              The array after being down-sized.
     */
    private byte[] reduceToGivenSize(byte[] toReduce,int realSize){
        byte[] temp = new byte[realSize];
        for(int i = 0; i < realSize;i++){
            temp[i] = toReduce[i];
        }
        return temp;
    }

    /**
     * Inserting the byte that was read to field1.
     * @param nextByte      Represents the byte that was read.
     */
    private void insertByteToField1(byte nextByte) {
        this.field1[this.field1Index] = nextByte;
        this.field1Index++;
        if(this.field1Index == this.field1.length){
            this.field1 = extendArray(this.field1);
        }
    }

    /**
     * Inserting the byte that was read to field2.
     * @param nextByte      Represents the byte that was read.
     */
    private void insertByteToField2(byte nextByte) {
        this.field2[this.field2Index] = nextByte;
        this.field2Index++;
        if(this.field2Index == this.field2.length){
            this.field2 = extendArray(this.field2);
        }
    }

    /**
     * Checks if needs to reduce the length of field1, and reduce it if so.
     */
    private void checkReduceField1() {
        if(this.field1Index!=this.field1.length){
            this.field1 = reduceToGivenSize(this.field1,this.field1Index);
        }
    }

    /**
     * Checks if needs to reduce the length of field2, and reduce it if so.
     */
    private void checkReduceField2() {
        if(this.field2Index!=this.field2.length){
            this.field2 = reduceToGivenSize(this.field2,this.field2Index);
        }
    }

    /**
     * encodes the given message to bytes array
     *
     * @param message the message to encode
     * @return the encoded bytes
     */
    @Override
    public byte[] encode(Message message) {
        return message.convertMessageToBytes();
    }
}
