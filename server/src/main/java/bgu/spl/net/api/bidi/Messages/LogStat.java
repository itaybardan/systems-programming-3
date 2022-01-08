package bgu.spl.net.api.bidi.Messages;

import bgu.spl.net.api.bidi.User;

import java.util.List;

/**
 * Message Of type USERLIST of Client-To-Server communication, when a user wants to see all the client that are registered to the server
 */
public class LogStat extends Message {

    /**
     * Default Constructor.
     */
    public LogStat() {
        this.opcode = Opcode.LOGSTAT;
    }


    /**
     * Convert all the data of this UserList message to a byte array.
     *
     * @return Byte array represent this UserList message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        return null;
    }


    /**
     * Generate matching Ack Message to this Follow Message Message according the Message data and server protocol.
     *
     * @param users List of Strings represents the registered users that were found by the server
     * @return Ack message matching this Follow Message data of this message according to the server protocol.
     */
    public Ack generateAckMessage(List<User> users, short[] numberOfPosts) {

        byte[][] elements = new byte[1 + 5 * users.size()][];
        short numOfUsers = (short) numberOfPosts.length;
        elements[0] = shortToBytes(numOfUsers);

        int index = 1;
        int i = 0;

        for (User user : users) {
            //converting each name in the list to bytes array and add it to elements.
            byte[] ageBytes = this.shortToBytes(user.getAge());
            byte[] numberOfPostsBytes = this.shortToBytes(numberOfPosts[i]);
            byte[] numberOfFollowersBytes = this.shortToBytes(user.getFollowersAmm());
            byte[] numberOfFollowingBytes = this.shortToBytes(user.getFollowingAmm());

            elements[index] = ageBytes;
            index++;
            elements[index] = numberOfPostsBytes;
            index++;
            elements[index] = numberOfFollowersBytes;
            index++;
            elements[index] = numberOfFollowingBytes;
            index++;
            byte[] separator = {'\0'};
            elements[index] = separator;
            index++;
            i++;
        }
        return new Ack(this.opcode, elements);

    }
}
