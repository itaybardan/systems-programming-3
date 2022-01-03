package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type POST of Client-To-Server communication, when a user wants to Post a message to all its followers.
 */
public class Post extends Message {

    //region Fields
    /**
     * String represents the content of the post.
     */
    private String content;
    //endregion Fields

    /**
     * Default Constructor.
     * @param content       String represents the content of the post.
     */
    public Post(String content) {
        this.opcode = Opcode.POST;
        this.content = content;
    }

    //region Getters
    public String getContent() {
        return content;
    }
    //endregion Getters

    /**
     * Convert all the data of this Post message to a byte array.
     * @return      Byte array represent this Post message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, and content to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] contentsBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] separator = {'\0'};
        byte[] output = new byte[opcode.length + contentsBytes.length + separator.length];
        int index = 0;
        //inserting the data of this message to a single byte array to return
        index = insertArray(opcode, output, index);
        index = insertArray(contentsBytes, output, index);
        index = insertArray(separator, output, index);
        return output;
    }

}
