package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;


public class Login extends Message {
    public final char captcha;
    private final String username;
    private final String password;

    public Login(String username, String password, char captcha) {
        this.opcode = Opcode.LOGIN;
        this.username = username;
        this.password = password;
        this.captcha = captcha;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


    @Override
    public byte[] convertMessageToBytes() {
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = this.password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + usernameBytes.length + passwordBytes.length + 2];
        int index = 0;
        index = insertArray(opcode, output, index);
        index = insertArray(usernameBytes, output, index);
        output[index] = '\0';
        index++;
        index = insertArray(passwordBytes, output, index);
        output[index] = '\0';
        return output;
    }

}
