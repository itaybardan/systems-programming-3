package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;

/**
 * Message Of type FOLLOW of Client-To-Server communication, when a user wants to follow or unfollow certain users
 */
public class Follow extends Message {

    /**
     * Boolean represents whether the Request is for following or unfollowing
     */
    private final boolean isFollowing;

    /**
     * List of String of users that the client want to follow or unfollow
     */
    private final String user;

    /**
     * Default constructor
     *
     * @param isFollowing Byte represents whether the client wants to follow or unfollow.
     * @param user        List of String represents the users the client wants to follow or unfollow.
     */
    public Follow(byte isFollowing, String user) {
        this.opcode = Opcode.FOLLOW;
        if (isFollowing == '\0') {
            //if the given byte is 0 --> follow
            this.isFollowing = true;
        } else {
            //else --> unfollow
            this.isFollowing = false;
        }

        this.user = user;

    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public String getUser() {
        return user;
    }

    /**
     * Convert all the data of this Follow message to a byte array.
     *
     * @return Byte array represent this Follow message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {

        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] isFollowingBytes = new byte[1];
        if (!isFollowing) {
            isFollowingBytes[0] = 1;
        }

        byte[] userInByte = user.getBytes(StandardCharsets.UTF_8);
        int userInByteLength = userInByte.length;

        byte[] output = new byte[opcode.length + isFollowingBytes.length + userInByteLength];
        int index = 0;

        //inserting all the data to single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(isFollowingBytes, output, index);
        index = insertArray(userInByte, output, index);
        output[index] = '\0';

        return output;
    }

    /**
     * Generate matching Ack Message to this Follow Message Message according the Message data and server protocol.
     *
     * @param user String represents the user that was found by the server
     * @return Ack message matching this Follow Message data of this message according to the server protocol.
     */
    public Ack generateAckMessage(String user) {

        byte[][] elements = new byte[3][];
        elements[0] = new byte[2];
        if (isFollowing) elements[0][0] = '0';
        else elements[0][0] = '1';
        elements[0][1] = ' ';
        byte[] toInsert = user.getBytes(StandardCharsets.UTF_8);
        elements[1] = toInsert;
        byte[] separator = {'\0'};
        elements[2] = separator;
        return new Ack(this.opcode, elements);
    }
}
