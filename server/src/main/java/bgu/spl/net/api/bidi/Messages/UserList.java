package bgu.spl.net.api.bidi.Messages;

import bgu.spl.net.api.bidi.User;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Message Of type USERLIST of Client-To-Server communication, when a user wants to see all the client that are registered to the server
 */
public class UserList extends Message {

    /**
     * Default Constructor.
     */
    public UserList() {
        this.opcode = Opcode.USERLIST;
    }


    /**
     * Convert all the data of this UserList message to a byte array.
     * @return      Byte array represent this UserList message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        return new byte[0];
    }


    /**
     * Generate matching Ack Message to this Follow Message Message according the Message data and server protocol.
     * @param numberOfRegisteredUsers               Short number represents the amount of users in the given list
     * @param users                       List of Strings represents the registered users that were found by the server
     * @return              Ack message matching this Follow Message data of this message according to the server protocol.
     */
    public Ack generateAckMessage(short numberOfRegisteredUsers, List<String> users) {

            //converting the number of of users to bytes array.
            byte[] numOfUsersBytes = this.shortToBytes(numberOfRegisteredUsers);
            byte[][] elements = new byte[1 + (2 * users.size())][];
            int index = 0;
            elements[index] = numOfUsersBytes;
            index++;
            for (String user : users) {
                //converting each name in the list to bytes array and add it to elements.
                byte[] currentUser = user.getBytes(StandardCharsets.UTF_8);
                elements[index] = currentUser;
                index++;
                byte[] separator = {'\0'};
                elements[index] = separator;
                index++;
            }
            return new Ack(this.opcode, elements);

    }
}
