package bgu.spl.net.api.bidi.Messages;

/**
 * Message Of type LOGOUT of Client-To-Server communication, when a user wants to logout from the server
 */
public class Logout extends Message{

    /**
     * Default Constructor
     */
    public Logout() {
        this.opcode = Opcode.LOGOUT;
    }

    /**
     * Convert all the data of this Logout message to a byte array.
     * @return      Byte array represent this Logout message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        return this.shortToBytes(this.opcode.getCode());
    }

}
