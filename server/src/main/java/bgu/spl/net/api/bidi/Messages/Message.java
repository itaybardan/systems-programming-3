package bgu.spl.net.api.bidi.Messages;

/**
 * Abstract Class represents a Message that is send from the server to the client or from the client to the server.
 */
public abstract class Message {

    //region Fields
    /**
     * Opcode Enum represents the Opcode that defines message type
     */
    protected Opcode opcode;

    //endregion Fields

    //region Opcode Enum
    /**
     * Enum to represents the Opcode the defines Each message
     */
    public enum Opcode {
        REGISTER,
        LOGIN,
        LOGOUT,
        FOLLOW,
        POST,
        PM,
        USERLIST,
        STAT,
        NOTIFICATION,
        ACK,
        ERROR;

        public short getCode(){
            if(this == Opcode.REGISTER){
               return 1;
            }
            else if(this == Opcode.LOGIN){
                return 2;
            }
            else if(this == Opcode.LOGOUT){
                return 3;
            }
            else if(this == Opcode.FOLLOW){
                return 4;
            }
            else if(this == Opcode.POST){
                return 5;
            }
            else if(this == Opcode.PM){
                return 6;
            }
            else if(this == Opcode.USERLIST){
                return 7;
            }
            else if(this == Opcode.STAT){
                return 8;
            }
            else if(this == Opcode.NOTIFICATION){
                return 9;
            }
            else if(this == Opcode.ACK){
                return 10;
            }
            else{
                //error message
                return 11;
            }
        }
    }
    //endregion Opcode Enum



    /**
     * Converting a short number to a array of bytes.
     * @param num           Short number to be converted.
     * @return      Array of size 2 represents the given number in bytes.
     */
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    //region Getters
    public Opcode getOpcode() {
        return opcode;
    }
    //endregion Getters

    /**
     * converting a bytes array to the equivalent short number.
     * @param byteArr           Bytes array of size 2 to be converted.
     * @return          Short number which represent the two bytes array.
     */
    public static short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    /**
     * Convert the given number to the matching opcode.
     * @param code              Short number to be converted.
     * @return          Opcode that is matching the given number.
     */
    public static Opcode convertToOpcode(short code){
        if(code == 1){
            return Opcode.REGISTER;
        }
        else if(code == 2){
            return Opcode.LOGIN;
        }
        else if(code == 3){
            return Opcode.LOGOUT;
        }
        else if(code == 4){
            return Opcode.FOLLOW;
        }
        else if(code == 5){
            return Opcode.POST;
        }
        else if(code == 6){
            return Opcode.PM;
        }
        else if(code == 7){
            return Opcode.USERLIST;
        }
        else if(code == 8){
            return Opcode.STAT;
        }
        else if(code == 9){
            return Opcode.NOTIFICATION;
        }
        else if(code == 10){
            return Opcode.ACK;
        }
        else if(code == 11){
            return Opcode.ERROR;
        }
        else {
            return null;
        }

    }

    /**
     * Convert all the data of a certain message to a byte array.
     * @return      Byte array represent the current message in the right order according to the server protocol
     */
    public abstract byte[] convertMessageToBytes();

    /**
     * Inserting array of bytes into the end of another Array bytes, from the given index.
     * @param array             Bytes array to insert to the "output" array.
     * @param output            Bytes array that the "array" is inserting bytes to.
     * @param index             Integer represent the current next free index to insert the new bytes from .
     * @return          Integer represent the new free index of the output array after the insertion
     */
    protected int insertArray(byte[] array, byte[] output, int index) {
        for (int i = 0; i < array.length; i++) {
            output[index] = array[i];
            index++;
        }
        return index;
    }

    /**
     * Generate matching Ack Message to the Current Message according the Message data and server protocol.
     * @return              Ack message matching the data of this message according to the server protocol.
     */
    public Ack generateAckMessage(){
        return new Ack(this.opcode,new byte[0][0]);
    }

}
