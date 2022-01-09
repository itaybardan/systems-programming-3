package bgu.spl.net.impl.Messages;

public class Ack extends Message {

    byte[][] messageElements;

    private Opcode resolvedOpcode;

    public Ack(Opcode resolvedOpcode, byte[][] elements) {
        this.opcode = Opcode.ACK;
        this.resolvedOpcode = resolvedOpcode;
        this.messageElements = elements;
    }

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
