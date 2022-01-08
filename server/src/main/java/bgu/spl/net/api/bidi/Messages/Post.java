package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;


public class Post extends Message {

    private final String content;

    public Post(String content) {
        this.opcode = Opcode.POST;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, and content to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] contentsBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] separator = {'\0'};
        byte[] output = new byte[opcode.length + contentsBytes.length + separator.length];
        int index = 0;
        index = insertArray(opcode, output, index);
        index = insertArray(contentsBytes, output, index);
        insertArray(separator, output, index);
        return output;
    }

}
