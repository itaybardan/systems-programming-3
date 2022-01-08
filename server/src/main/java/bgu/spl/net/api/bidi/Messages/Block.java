package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type STAT of Client-To-Server communication, when a user wants to check the Status of a certain user.
 */
public class Block extends Message {

    /**
     * String represents the username that the client want to check his status.
     */
    private final String username;

    /**
     * Default Constructor.
     *
     * @param username String represents the username that the client want to check his status.
     */
    public Block(String username) {
        this.opcode = Opcode.BLOCK;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }


    /**
     * Convert all the data of this Stat message to a byte array.
     *
     * @return Byte array represent this Stat message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode and username to bytes arrays
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] userNameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + userNameBytes.length + 1];
        int index = 0;
        //inserting all the data of this message to a single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(userNameBytes, output, index);
        output[index] = '\0';
        return output;
    }

}