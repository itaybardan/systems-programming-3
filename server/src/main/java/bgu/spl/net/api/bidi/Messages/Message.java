package bgu.spl.net.api.bidi.Messages;

/**
 * Abstract Class represents a Message that is send from the server to the client or from the client to the server.
 */
public abstract class Message {

    /**
     * Opcode Enum represents the Opcode that defines message type
     */
    protected Opcode opcode;

    /**
     * converting a bytes array to the equivalent short number.
     *
     * @param byteArr Bytes array of size 2 to be converted.
     * @return Short number which represent the two bytes array.
     */
    public static short bytesToShort(byte[] byteArr) {
        short result = (short) ((byteArr[0] & 0xff) << 8);
        result += (short) (byteArr[1] & 0xff);
        return result;
    }

    /**
     * Convert the given number to the matching opcode.
     *
     * @param code Short number to be converted.
     * @return Opcode that is matching the given number.
     */
    public static Opcode convertToOpcode(short code) {
        if (code == 1) {
            return Opcode.REGISTER;
        } else if (code == 2) {
            return Opcode.LOGIN;
        } else if (code == 3) {
            return Opcode.LOGOUT;
        } else if (code == 4) {
            return Opcode.FOLLOW;
        } else if (code == 5) {
            return Opcode.POST;
        } else if (code == 6) {
            return Opcode.PM;
        } else if (code == 7) {
            return Opcode.LOGSTAT;
        } else if (code == 8) {
            return Opcode.STAT;
        } else if (code == 9) {
            return Opcode.NOTIFICATION;
        } else if (code == 10) {
            return Opcode.ACK;
        } else if (code == 11) {
            return Opcode.ERROR;
        } else if (code == 12) {
            return Opcode.BLOCK;
        } else {
            return null;
        }

    }

    /**
     * Converting a short number to a array of bytes.
     *
     * @param num Short number to be converted.
     * @return Array of size 2 represents the given number in bytes.
     */
    public byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte) ((num >> 8) & 0xFF);
        bytesArr[1] = (byte) (num & 0xFF);
        return bytesArr;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public abstract byte[] convertMessageToBytes();

    protected int insertArray(byte[] array, byte[] output, int index) {
        for (byte b : array) {
            output[index] = b;
            index++;
        }
        return index;
    }

    public Ack generateAckMessage() {
        return new Ack(this.opcode, new byte[0][0]);
    }

    public enum Opcode {
        REGISTER,
        LOGIN,
        LOGOUT,
        FOLLOW,
        POST,
        PM,
        LOGSTAT,
        STAT,
        NOTIFICATION,
        ACK,
        ERROR,
        BLOCK;

        public short getCode() {
            if (this == Opcode.REGISTER) {
                return 1;
            } else if (this == Opcode.LOGIN) {
                return 2;
            } else if (this == Opcode.LOGOUT) {
                return 3;
            } else if (this == Opcode.FOLLOW) {
                return 4;
            } else if (this == Opcode.POST) {
                return 5;
            } else if (this == Opcode.PM) {
                return 6;
            } else if (this == Opcode.LOGSTAT) {
                return 7;
            } else if (this == Opcode.STAT) {
                return 8;
            } else if (this == Opcode.NOTIFICATION) {
                return 9;
            } else if (this == Opcode.ACK) {
                return 10;
            } else if (this == Opcode.BLOCK) {
                return 12;
            } else {
                //error message
                return 11;
            }
        }

    }

}
