package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

public class Notification extends Message {

    private final byte privateMessageOrPublicPost;

    private final String postingUser;

    private final String content;

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

    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, posting user and content of the message to bytes arrays
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] postingUserBytes = this.postingUser.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + 1 + postingUserBytes.length + contentBytes.length + 2];
        int index = 0;
        //inserting the data of this message to a single byte array to return.
        index = insertArray(opcode, output, index);
        output[index] = this.privateMessageOrPublicPost;
        index++;
        index = insertArray(postingUserBytes, output, index);
        output[index] = '\0';
        index++;
        index = insertArray(contentBytes, output, index);
        output[index] = '\0';
        return output;
    }

    @Override
    public Ack generateAckMessage() {
        return null;
    }
}
