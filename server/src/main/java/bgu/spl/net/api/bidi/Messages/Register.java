package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type REGISTER of Client-To-Server communication, when a user wants to register to the server
 */
public class Register extends Message {

    /**
     * String represents the Username who wants to register to the server.
     */
    private final String username;

    /**
     * String represents the Password of the4 user who wants to register to the server.
     */
    private final String password;

    public short getYear() {
        return year;
    }
    public short getMonth() {
        return month;
    }
    public short getDay() {
        return day;
    }

    private final short year;
    private final short month;
    private final short day;


    /**
     * Default constructor.
     *
     * @param username String represents the Username who wants to register to the server.
     * @param password String represents the Password of the4 user who wants to register to the server.
     */
    public Register(String username, String password, short year, short month, short day) {
        this.opcode = Opcode.REGISTER;
        this.username = username;
        this.password = password;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Convert all the data of this Register message to a byte array.
     *
     * @return Byte array represent this Register message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode, username and password to bytes arrays.
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = this.password.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + usernameBytes.length + passwordBytes.length + 2];
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
