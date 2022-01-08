package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;


public class PM extends Message {
    public static String[] forbiddenWords = new String[]{"fuck", "dinner"};

    private final String userName;

    private final String content;

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

    public String getFilteredContent() {
        String content = this.content;
        for (String word : PM.forbiddenWords) {
            content = content.replace(word, "<filtered>");
        }
        return content;
    }

    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode,  username and content to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.userName.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + usernameBytes.length + contentBytes.length + 2];
        int index = 0;
        //inserting the data of this message to a single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(usernameBytes, output, index);
        output[index] = '\0';
        index++;
        index = insertArray(contentBytes, output, index);
        output[index] = '\0';
        return output;

    }

}
