package bgu.spl.net.api.bidi.Messages;


/**
 * Message Of type ERROR of Server-To-Client communication, when a message request when wrong
 */
public class Error extends Message {

    //region Fields
    /**
     * Opcode of the Message which was canceled.
     */
    private Opcode errorMessageOpcode;
    //endregion Fields

    /**
     * Default Constructor.
     * @param errorMessageOpcode       Opcode of the Message which was canceled.
     */
    public Error(Opcode errorMessageOpcode) {
        this.opcode = Opcode.ERROR;
        this.errorMessageOpcode = errorMessageOpcode;
    }

    /**
     * Convert all the data of this Error message to a byte array.
     * @return      Byte array represent this Error message in the right order according to the server protocol.
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the error message fields to bytes array
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] errorOpcode = this.shortToBytes(this.errorMessageOpcode.getCode());
        byte[] output = new byte[opcode.length + errorOpcode.length];
        int index = 0;
        //inserting the error opcode.
        index = insertArray(opcode,output,index);
        //inserting the opcode of the message that was canceled.
        index = insertArray(errorOpcode,output,index);
        return output;
    }

    /**
     *No need to return an Ack message to an Error message
     */
    @Override
    public Ack generateAckMessage() {
        return null;
    }
}
