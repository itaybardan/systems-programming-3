package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

public class Block extends Message {

    private final String username;

    public Block(String username) {
        this.opcode = Opcode.BLOCK;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

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