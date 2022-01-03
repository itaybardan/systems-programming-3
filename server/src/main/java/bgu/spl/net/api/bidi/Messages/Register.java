package bgu.spl.net.api.bidi.Messages;
import java.nio.charset.StandardCharsets;

/**
 * Message Of type REGISTER of Client-To-Server communication, when a user wants to register to the server
 */
public class Register extends Message {

    //region Fields
    /**
     * String represents the Username who wants to register to the server.
     */
    private String username;

    /**
     * String represents the Password of the4 user who wants to register to the server.
     */
    private String password;
    //endregion Fields


    /**
     * Default constructor.
     * @param username      String represents the Username who wants to register to the server.
     * @param password      String represents the Password of the4 user who wants to register to the server.
     */
    public Register(String username, String password) {
        this.opcode = Opcode.REGISTER;
        this.username = username;
        this.password = password;
    }

    //region Getters.
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    //endregion Getters

    /**
     * Convert all the data of this Register message to a byte array.
     * @return      Byte array represent this Register message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, username and password to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = this.password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + usernameBytes.length+passwordBytes.length+2];
        int index = 0;
        //inserting the data of this message to a single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(usernameBytes, output, index);
        output[index] = '\0';
        index++;
        index = insertArray(passwordBytes, output, index);
        output[index] = '\0';
        return output;
    }
}
