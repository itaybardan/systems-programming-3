package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type PM of Client-To-Server communication, when a user wants to send a private message to other user.
 */
public class PM extends Message {

    //region Fields
    /**
     * String represents the username that needs to get this message.
     */
    private String userName;

    /**
     * String represents the Content of this message.
     */
    private String content;
    //endregion Fields

    /**
     * Default constructor.
     * @param userName      String represents the username that needs to get this message.
     * @param content       String represents the Content of this message.
     */
    public PM(String userName, String content) {
        this.opcode = Opcode.PM;
        this.userName = userName;
        this.content = content;
    }

    //region Getters
    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }
    //endregion Getters

    /**
     * Convert all the data of this PM message to a byte array.
     * @return      Byte array represent this PM message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode,  username and content to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.userName.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length+usernameBytes.length+contentBytes.length+2];
        int index = 0;
        //inserting the data of this message to a single byte array to return.
        index = insertArray(opcode,output,index);
        index = insertArray(usernameBytes,output,index);
        output[index] = '\0';
        index++;
        index = insertArray(contentBytes,output,index);
        output[index] = '\0';
        return output;

    }

}
