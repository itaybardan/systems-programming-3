package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type STAT of Client-To-Server communication, when a user wants to check the Status of a certain user.
 */
public class Stat extends Message{

    //region Fields
    /**
     * String represents the username that the client want to check his status.
     */
    private String username;
    //endregion Fields

    /**
     * Default Constructor.
     * @param username      String represents the username that the client want to check his status.
     */
    public Stat(String username) {
        this.opcode = Opcode.STAT;
        this.username = username;
    }
    //region Getters
    public String getUsername() {
        return username;
    }
    //endregion Getters

    /**
     * Convert all the data of this Stat message to a byte array.
     * @return      Byte array represent this Stat message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        //converting the opcode and username to bytes arrays
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] userNameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] output = new byte[opcode.length + userNameBytes.length + 1];
        int index = 0;
        //inserting all the data of this message to a single byte array to return.
        index = insertArray(opcode,output,index);
        index = insertArray(userNameBytes,output,index);
        output[index] = '\0';
        return output;
    }

    /**
     * Generate matching Ack Message to this Stat Message Message according the Message data and server protocol.
     * @param numberOfPosts                     short number represents the number of posts of the requested user.
     * @param numberOfFollowers                 short number represents the number of followers of the requested user.
     * @param numberOfFollowing                 short number represents the number of users following of the requested user.
     * @return        Ack message matching this Stat Message data of this message according to the server protocol.
     */
    public Ack generateAckMessage(short numberOfPosts, short numberOfFollowers,  short numberOfFollowing) {

            //converting the number of posts, number of followers and number of following to bytes arrays.
            byte[] numberOfPostsBytes = this.shortToBytes(numberOfPosts);
            byte[] numberOfFollowersBytes = this.shortToBytes(numberOfFollowers);
            byte[] numberOfFollowingBytes = this.shortToBytes(numberOfFollowing);
            byte[][] elements = new byte[3][];
            //inserting all the array to the elements 2-D array of bytes
            elements[0] = numberOfPostsBytes;
            elements[1] = numberOfFollowersBytes;
            elements[2] = numberOfFollowingBytes;
            return new Ack(this.opcode,elements);

    }
}
