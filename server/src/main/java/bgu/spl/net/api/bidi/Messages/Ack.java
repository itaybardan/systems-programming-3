package bgu.spl.net.api.bidi.Messages;

/**
 * Class represents Ack Messages From server to the client.
 */
public class Ack extends Message {

    /**
     * 2-D array of Bytes Represents additional/Optional Elements to add to the Ack message.
     */
    byte[][] messageElements;
    /**
     * Opcode of the message that was acknowledged by the server.
     */
    private Opcode resolvedOpcode;


    /**
     * Default Constructor
     *
     * @param resolvedOpcode Opcode of the acknowledged message.
     * @param elements       2-D array of bytes of elements to add to the Ack message.
     */
    public Ack(Opcode resolvedOpcode, byte[][] elements) {
        this.opcode = Opcode.ACK;
        this.resolvedOpcode = resolvedOpcode;
        this.messageElements = elements;
    }

    /**
     * get the Resolved Message opcode.
     *
     * @return Opcode of the Resolved Message of this Ack message.
     */
    public Opcode getResolvedOpcode() {
        return resolvedOpcode;
    }

    /**
     * get the Additional elements of this Ack message
     *
     * @return 2-D Bytes array of the additional elements of this Ack Message
     */
    public byte[][] getMessageElements() {
        return messageElements;
    }

    /**
     * Convert all the data of this Ack message to a byte array.
     *
     * @return Byte array represent this Ack message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //ack opcode
        byte[] ackOpcode = this.shortToBytes(this.opcode.getCode());
        //resolved opcode
        byte[] resolvedOpcode = this.shortToBytes(this.resolvedOpcode.getCode());
        int numberOfBytes = 0;

        if (this.messageElements != null) {
            //if this ack code contains more than "ACK + number" --> add it to the ack message
            for (int i = 0; i < messageElements.length; i++) {
                //for each element in the additional message elements --> add it to the output array
                numberOfBytes += this.messageElements[i].length;
            }
        }
        byte[] output = new byte[ackOpcode.length + resolvedOpcode.length + numberOfBytes];
        int index = 0;
        //inserting op code and resolved opcode to message
        index = insertArray(ackOpcode, output, index);
        index = insertArray(resolvedOpcode, output, index);
        if (this.messageElements != null) {
            //for eac additional message int the array --> add it to the bytes array
            for (int i = 0; i < messageElements.length; i++) {
                index = insertArray(this.messageElements[i], output, index);
            }
        }
        return output;
    }

    /**
     * No need to return an ack message to an ack message.
     */
    @Override
    public Ack generateAckMessage() {
        return null;
    }
}
