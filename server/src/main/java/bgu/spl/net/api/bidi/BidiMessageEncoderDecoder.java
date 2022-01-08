package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.bidi.Messages.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Encoder and Decoder for the bidi protocol.
 */
public class BidiMessageEncoderDecoder implements MessageEncoderDecoder<Message> {

    private byte[] opcodeBytes;

    private int opcodeInsertedLength;

    private byte[] field1;

    private byte[] field2;

    private int field1Index;

    private int field2Index;

    private int zeros;

    private byte aByte;

    private Message.Opcode currentOpcode;

    public BidiMessageEncoderDecoder() {
        this.opcodeBytes = new byte[2];
        this.opcodeInsertedLength = 0;
        this.currentOpcode = null;
        this.field1 = new byte[10];
        this.field2 = new byte[10];
        this.field1Index = 0;
        this.field2Index = 0;
        this.zeros = 0;
        this.aByte = 0;

    }

    /**
     * add the next byte to the decoding process
     *
     * @param nextByte the next byte to consider for the currently decoded
     *                 message
     * @return a message if this byte completes one or null if it doesnt.
     */
    @Override
    public Message decodeNextByte(byte nextByte) {

        if (nextByte == ';') {
            return null;
        }

        if (this.opcodeInsertedLength == 0) {
            this.opcodeBytes[0] = nextByte;
            this.opcodeInsertedLength++;
            return null;
        } else if (this.opcodeInsertedLength == 1) {
            this.opcodeBytes[1] = nextByte;
            this.opcodeInsertedLength++;
            initMessageContentAndLength();
            if ((this.currentOpcode == Message.Opcode.LOGOUT) ||
                    (this.currentOpcode == Message.Opcode.LOGSTAT)) {
                if (this.currentOpcode == Message.Opcode.LOGOUT) {
                    generalVariablesReset();
                    return new Logout();
                } else {
                    generalVariablesReset();
                    return new LogStat();
                }
            }
            return null;
        } else {
            //Reading and generating the rest of the message.
            return readingMessage(nextByte);
        }
    }

    /**
     * Generating a message according to the current opcode.
     *
     * @param nextByte Byte represents the next byte to decode.
     * @return Message represents a message from the client.
     */
    private Message readingMessage(byte nextByte) {
        Message output;
        if (this.currentOpcode == Message.Opcode.REGISTER) {
            output = readingRegisterMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.LOGIN) {
            output = readingLoginMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.FOLLOW) {
            output = readingFollowMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.POST) {
            output = readingPostMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.PM) {
            output = readingPMMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.STAT) {
            output = readingStatMessage(nextByte);
        } else if (this.currentOpcode == Message.Opcode.BLOCK) {
            output = readingBlockMessage(nextByte);
        } else {
            //No other clients to server opcodes in the assignment specifications.
            output = null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to field1, and generating a Stat message by translating field1 to a string.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message which is a Stat message.
     */
    private Message readingStatMessage(byte nextByte) {
        // field1 = username
        Message output;
        if (nextByte == '\0') {
            checkReduceField1();
            String username = new String(this.field1, StandardCharsets.UTF_8);
            output = new Stat(username);
            this.generalVariablesReset();
        } else {
            insertByteToField1(nextByte);
            return null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "PM" command.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message which is a PM message.
     */
    private Message readingPMMessage(byte nextByte) {
        //field1 = username   | field2 = content
        Message output;
        if (this.zeros == 0) {
            //Inserting to username.
            if (nextByte == '\0') {
                this.zeros++;
            } else {
                insertByteToField1(nextByte);
            }
            return null;
        } else {
            //Inserting content.
            if (nextByte == '\0') {
                output = generatePMMessage();
                this.generalVariablesReset();
            } else {
                insertByteToField2(nextByte);
                return null;
            }
        }
        return output;
    }

    /**
     * Generating "PM" message, by translating the arrays of bytes to strings.
     *
     * @return Message which is a PM message.
     */
    private Message generatePMMessage() {
        Message output;//finished reading
        checkReduceField1();
        checkReduceField2();
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String content = new String(this.field2, StandardCharsets.UTF_8);
        output = new PM(username, content);
        return output;
    }

    /**
     * Reading bytes and inserting them to field1, and generating a Post message by translating field1 to a string.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message which is a Post message.
     */
    private Message readingPostMessage(byte nextByte) {
        //field1 = content
        Message output;
        if (nextByte == '\0') {
            //finished reading the message
            checkReduceField1();
            String content = new String(this.field1, StandardCharsets.UTF_8);
            output = new Post(content);
            this.generalVariablesReset();
        } else {
            this.insertByteToField1(nextByte);
            return null;
        }
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "FOLLOW" command.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message represents the decoded message, or null if not done reading the message.
     */
    private Message readingFollowMessage(byte nextByte) {
        //field1 = null  | field2 = usernameList | followbyte = follow \ unfollow | zerocounter = bytesCounter
        if (this.zeros == 0) {
            this.aByte = nextByte;
            this.zeros++;
            return null;
        } else {
            insertByteToField2(nextByte);
            if (nextByte == '\0') {
                return generateFollowMessage();
            } else {
                return null;
            }
        }
    }

    private Message readingBlockMessage(byte nextByte) {
        // field1 = username
        Message output;
        if (nextByte == '\0') {
            checkReduceField1();
            String username = new String(this.field1, StandardCharsets.UTF_8);
            output = new Block(username);
            this.generalVariablesReset();
        } else {
            insertByteToField1(nextByte);
            return null;
        }
        return output;
    }


    /**
     * Generating "Follow" message, by translating the arrays of bytes to strings.
     *
     * @return Message which is a Follow message.
     */
    private Message generateFollowMessage() {
        Message output;
        checkReduceField2();

        byte[] userByte = Arrays.copyOfRange(field2, 0, field2Index - 1);
        output = new Follow(this.aByte, new String(userByte, StandardCharsets.UTF_8));
        generalVariablesReset();
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "REGISTER" commands.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message represents the decoded message, or null if not done reading the message.
     */
    private Message readingRegisterMessage(byte nextByte) {
        //Field1 = username/0password | Field2 = birth date
        if (this.zeros <= 1) {
            //The next byte going to be to the userName or passName
            if (nextByte == '\0') {
                this.zeros++;
                if (this.zeros == 2) {
                    checkReduceField1();
                    return null;
                }

            }
            insertByteToField1(nextByte);
            return null;
        } else {
            //The next byte is going to be to the date.
            if (nextByte == '\0') {

                checkReduceField2();
                //Creating the Register or Login Message
                return generateRegisterMessage();
            }
            insertByteToField2((byte) (nextByte - 48));
            return null;
        }
    }

    /**
     * Generating "Register"  messages according to the current opcode, by translating the arrays of bytes
     * to strings.
     *
     * @return Message which is a Register message.
     */

    private Message generateRegisterMessage() { //TODO separate into register.
        Message output;
        int separator = 0;
        for (int i = 0; i < field1Index; i++) {
            if (field1[i] == '\0') {
                separator = i;
                break;
            }
        }

        String username = new String(Arrays.copyOfRange(field1, 0, separator), StandardCharsets.UTF_8);
        String password = new String(Arrays.copyOfRange(field1, separator + 1, field1Index), StandardCharsets.UTF_8);

        //init date
        short day = (short) (field2[0] * 10 + field2[1]);
        short month = (short) (field2[3] * 10 + field2[4]);
        short year = (short) (field2[6] * 1000 + field2[7] * 100 + field2[8] * 10 + field2[9]);

        output = new Register(username, password, year, month, day);

        generalVariablesReset();
        return output;
    }

    /**
     * Reading bytes and inserting them to the fields according to the specifications for "Login" commands.
     *
     * @param nextByte Represents the next byte to be translated.
     * @return Message represents the decoded message, or null if not done reading the message.
     */
    private Message readingLoginMessage(byte nextByte) {
        //Field1 = username | Field2 = password
        if (this.zeros == 0) {
            //The next byte going to be to the userName
            if (nextByte == '\0') {
                // The current username ended.
                checkReduceField1();
                this.zeros++;
                return null;
            }
            insertByteToField1(nextByte);
            return null;
        } else if (this.zeros == 1) {
            //The next byte is going to be to the password.
            if (nextByte == '\0') {
                checkReduceField2();
                this.zeros++;
                return null;
            }
            insertByteToField2(nextByte);
            return null;
        } else if (nextByte != '\0') {
            aByte = nextByte;
            return null;
        }
        return generateLoginMessage((char) aByte);
    }

    /**
     * Generating "Login"  messages according to the current opcode, by translating the arrays of bytes
     * to strings.
     *
     * @return Message which is a Login message.
     */

    private Message generateLoginMessage(char captcha) {
        Message output;
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String password = new String(this.field2, StandardCharsets.UTF_8);
        output = new Login(username, password, captcha);

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
        this.zeros = 0;
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
     *
     * @param array Represents the array that needs it's length to be extended.
     * @return The array with the extended length.
     */
    private byte[] extendArray(byte[] array) {
        int size = array.length;
        byte[] temp = new byte[size * 2];
        System.arraycopy(array, 0, temp, 0, size);
        return temp;
    }

    /**
     * Reduces the length of a given array to it's real size.
     *
     * @param toReduce Represents the array that needs to be down-sized.
     * @param realSize Represents the size the array should be down-sized to.
     * @return The array after being down-sized.
     */
    private byte[] reduceToGivenSize(byte[] toReduce, int realSize) {
        byte[] temp = new byte[realSize];
        System.arraycopy(toReduce, 0, temp, 0, realSize);
        return temp;
    }

    /**
     * Inserting the byte that was read to field1.
     *
     * @param nextByte Represents the byte that was read.
     */
    private void insertByteToField1(byte nextByte) {
        this.field1[this.field1Index] = nextByte;
        this.field1Index++;
        if (this.field1Index == this.field1.length) {
            this.field1 = extendArray(this.field1);
        }
    }

    /**
     * Inserting the byte that was read to field2.
     *
     * @param nextByte Represents the byte that was read.
     */
    private void insertByteToField2(byte nextByte) {
        this.field2[this.field2Index] = nextByte;
        this.field2Index++;
        if (this.field2Index == this.field2.length) {
            this.field2 = extendArray(this.field2);
        }
    }

    /**
     * Checks if needs to reduce the length of field1, and reduce it if so.
     */
    private void checkReduceField1() {
        if (this.field1Index != this.field1.length) {
            this.field1 = reduceToGivenSize(this.field1, this.field1Index);
        }
    }

    /**
     * Checks if needs to reduce the length of field2, and reduce it if so.
     */
    private void checkReduceField2() {
        if (this.field2Index != this.field2.length) {
            this.field2 = reduceToGivenSize(this.field2, this.field2Index);
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
