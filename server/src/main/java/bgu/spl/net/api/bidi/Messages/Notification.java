package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type NOTIFICATION of Server-To-Client communication, when a client recive a message from the server from other user.
 */
public class Notification extends Message {

    /**
     * Byte declaring whether it's a private or public message.
     */
    private byte privateMessageOrPublicPost;

    /**
     * String represents the user Name who post this message.
     */
    private String postingUser;

    /**
     * String represent the content of this message.
     */
    private String content;

    /**
     * Default Constructor
     * @param privateMessageOrPublicPost            Byte declaring whether it's a private or public message.
     * @param postingUser                           String represents the user Name who post this message.
     * @param content                               String represent the content of this message.
     */
    public Notification(byte privateMessageOrPublicPost, String postingUser, String content) {
        this.opcode = Opcode.NOTIFICATION;
        this.privateMessageOrPublicPost = privateMessageOrPublicPost;
        this.postingUser = postingUser;
        this.content = content;
    }

    //region Getters
    public byte getPrivateMessageOrPublicPost() {
        return privateMessageOrPublicPost;
    }

    public String getPostingUser() {
        return postingUser;
    }

    public String getContent() {
        return content;
    }
    //endregion Getters

    /**
     * Convert all the data of this Notification message to a byte array.
     * @return      Byte array represent this Notification message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, posting user and content of the message to bytes arrays
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] postingUserBytes = this.postingUser.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + 1 + postingUserBytes.length + contentBytes.length + 2];
        int index = 0;
        //inserting the data of this message to a single byte array to return.
        index = insertArray(opcode,output,index);
        output[index] = this.privateMessageOrPublicPost;
        index++;
        index = insertArray(postingUserBytes,output,index);
        output[index] = '\0';
        index++;
        index = insertArray(contentBytes,output,index);
        output[index] = '\0';
        return output;
    }

    /**
     *No Need to return an Ack Message to a Notification message
     */
    @Override
    public Ack generateAckMessage() {
        return null;
    }
}
