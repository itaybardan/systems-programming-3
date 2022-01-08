package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

public class Follow extends Message {

    private final boolean isFollowing;
    private final short numberOfUsers;
    private final List<String> users;

    public Follow(byte isFollowing, short numberOfUsers, List<String> users) {
        this.opcode = Opcode.FOLLOW;
        if (isFollowing == '\0') {
            //if the given byte is 0 --> follow
            this.isFollowing = true;
        } else {
            //else --> unfollow
            this.isFollowing = false;
        }
        this.numberOfUsers = numberOfUsers;
        this.users = users;

    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public short getNumberOfUsers() {
        return numberOfUsers;
    }

    public List<String> getUsers() {
        return users;
    }

    @Override
    public byte[] convertMessageToBytes() {
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] isFollowingBytes = new byte[1];
        if (isFollowing) {
            //if the user requested to follow --> return 0 byte
            isFollowingBytes[0] = '\0';
        } else {
            //else --> return 1 byte.
            isFollowingBytes[0] = 1;
        }
        //converting the number of users to bytes array
        byte[] numberOfUsersBytes = this.shortToBytes((short) this.numberOfUsers);
        Vector<byte[]> usersNames = new Vector<>();
        int totalBytesOfUsers = 0;
        // converting each username to a byte array.
        for (String currentName : this.users) {
            byte[] currentNameArr = currentName.getBytes(StandardCharsets.UTF_8);
            usersNames.add(currentNameArr);
            totalBytesOfUsers += currentNameArr.length;
        }
        byte[] output = new byte[opcode.length + isFollowingBytes.length +
                numberOfUsersBytes.length + totalBytesOfUsers + this.numberOfUsers];
        int index = 0;
        //inserting all the data to single byte array to return.
        index = insertArray(opcode, output, index);
        index = insertArray(isFollowingBytes, output, index);
        index = insertArray(numberOfUsersBytes, output, index);
        for (byte[] currentUser : usersNames) {
            index = insertArray(currentUser, output, index);
            output[index] = '\0';
            index++;
        }
        return output;
    }

    public Ack generateAckMessage(short numberOfUsers, List<String> users) {

        //converting the number of users to bytes array
        byte[] numberOfUsersBytes = this.shortToBytes(numberOfUsers);
        byte[][] elements = new byte[1 + (numberOfUsers * 2)][];
        int index = 0;
        elements[index] = numberOfUsersBytes;
        index++;
        for (String currentUser : users) {
            //converting each name in the list to bytes array and add it to the elements array.
            byte[] toInsert = currentUser.getBytes(StandardCharsets.UTF_8);
            elements[index] = toInsert;
            index++;
            byte[] separator = {'\0'};
            elements[index] = separator;
            index++;
        }
        Ack output = new Ack(this.opcode, elements);
        return output;
    }
}
