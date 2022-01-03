package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type LOGIN of Client-To-Server communication, when a user wants to login to the server.
 */
public class Login extends Message {

    //region Fields
    /**
     * String represents the username who requested to login.
     */
    private String username;

    /**
     * String represents the password of the user who requested to login.
     */
    private String password;
    //endregion Fields

    /**
     * Default Constructor.
     * @param username      String represents the username who requested to login.
     * @param password      String represents the password of the user who requested to login.
     */
    public Login(String username, String password) {
        this.opcode=Opcode.LOGIN;
        this.username = username;
        this.password = password;
    }

    //region Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    //endregion Getters

    /**
     * Convert all the data of this Login message to a byte array.
     * @return      Byte array represent this Login message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, username and password to bytes array.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = this.password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + usernameBytes.length+passwordBytes.length+2];
        int index = 0;
        //inserting the data in a single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(usernameBytes, output, index);
        output[index] = '\0';
        index++;
        index = insertArray(passwordBytes, output, index);
        output[index] = '\0';
        return output;
    }

}
