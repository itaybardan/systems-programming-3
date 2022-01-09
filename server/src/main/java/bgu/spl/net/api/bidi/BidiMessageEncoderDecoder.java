package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.Messages.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BidiMessageEncoderDecoder implements MessageEncoderDecoder<Message> {

    private byte[] opcodeBytes;

    private int opcodeInsertedLength;

    private byte[] field1;

    private byte[] field2;

    private int index1;

    private int index2;

    private int zeros;

    private byte aByte;

    private Message.Opcode opcodeEnum;

    public BidiMessageEncoderDecoder() {
        this.opcodeBytes = new byte[2];
        this.opcodeInsertedLength = 0;
        this.opcodeEnum = null;
        this.field1 = new byte[10];
        this.field2 = new byte[10];
        this.index1 = 0;
        this.index2 = 0;
        this.zeros = 0;
        this.aByte = 0;

    }

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
            if ((this.opcodeEnum == Message.Opcode.LOGOUT) ||
                    (this.opcodeEnum == Message.Opcode.LOGSTAT)) {
                if (this.opcodeEnum == Message.Opcode.LOGOUT) {
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

    private Message readingMessage(byte nextByte) {
        Message output;
        if (this.opcodeEnum == Message.Opcode.REGISTER) {
            output = readingRegisterMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.LOGIN) {
            output = readingLoginMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.FOLLOW) {
            output = readingFollowMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.POST) {
            output = readingPostMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.PM) {
            output = readingPMMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.STAT) {
            output = readingStatMessage(nextByte);
        } else if (this.opcodeEnum == Message.Opcode.BLOCK) {
            output = readingBlockMessage(nextByte);
        } else {
            //No other clients to server opcodes in the assignment specifications.
            output = null;
        }
        return output;
    }

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

    private Message generatePMMessage() {
        Message output;//finished reading
        checkReduceField1();
        checkReduceField2();
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String content = new String(this.field2, StandardCharsets.UTF_8);
        output = new PM(username, content);
        return output;
    }

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

    private Message generateFollowMessage() {
        Message output;
        checkReduceField2();

        byte[] userByte = Arrays.copyOfRange(field2, 0, index2 - 1);
        output = new Follow(this.aByte, new String(userByte, StandardCharsets.UTF_8));
        generalVariablesReset();
        return output;
    }

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
    private Message generateRegisterMessage() { //TODO separate into register.
        Message output;
        int separator = 0;
        for (int i = 0; i < index1; i++) {
            if (field1[i] == '\0') {
                separator = i;
                break;
            }
        }

        String username = new String(Arrays.copyOfRange(field1, 0, separator), StandardCharsets.UTF_8);
        String password = new String(Arrays.copyOfRange(field1, separator + 1, index1), StandardCharsets.UTF_8);

        //init date
        short day = (short) (field2[0] * 10 + field2[1]);
        short month = (short) (field2[3] * 10 + field2[4]);
        short year = (short) (field2[6] * 1000 + field2[7] * 100 + field2[8] * 10 + field2[9]);

        output = new Register(username, password, year, month, day);

        generalVariablesReset();
        return output;
    }

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

    private Message generateLoginMessage(char captcha) {
        Message output;
        String username = new String(this.field1, StandardCharsets.UTF_8);
        String password = new String(this.field2, StandardCharsets.UTF_8);
        output = new Login(username, password, captcha);

        generalVariablesReset();
        return output;
    }

    private void generalVariablesReset() {
        this.opcodeBytes = new byte[2];
        this.opcodeEnum = null;
        this.field1 = new byte[12];
        this.field2 = new byte[12];
        this.index1 = 0;
        this.index2 = 0;
        this.zeros = 0;
        this.opcodeInsertedLength = 0;
    }

    private void initMessageContentAndLength() {
        this.opcodeEnum = Message.convertToOpcode(Message.bytesToShort(this.opcodeBytes));
    }

    private byte[] extendArray(byte[] array) {
        int size = array.length;
        byte[] temp = new byte[size * 2];
        System.arraycopy(array, 0, temp, 0, size);
        return temp;
    }

    private byte[] reduceToGivenSize(byte[] toReduce, int realSize) {
        byte[] temp = new byte[realSize];
        System.arraycopy(toReduce, 0, temp, 0, realSize);
        return temp;
    }

    private void insertByteToField1(byte nextByte) {
        this.field1[this.index1] = nextByte;
        this.index1++;
        if (this.index1 == this.field1.length) {
            this.field1 = extendArray(this.field1);
        }
    }

    private void insertByteToField2(byte nextByte) {
        this.field2[this.index2] = nextByte;
        this.index2++;
        if (this.index2 == this.field2.length) {
            this.field2 = extendArray(this.field2);
        }
    }

    private void checkReduceField1() {
        if (this.index1 != this.field1.length) {
            this.field1 = reduceToGivenSize(this.field1, this.index1);
        }
    }

    private void checkReduceField2() {
        if (this.index2 != this.field2.length) {
            this.field2 = reduceToGivenSize(this.field2, this.index2);
        }
    }

    @Override
    public byte[] encode(Message message) {
        return message.convertMessageToBytes();
    }
}
