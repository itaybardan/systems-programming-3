package bgu.spl.net.api.bidi.Messages;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

/**
 * Message Of type FOLLOW of Client-To-Server communication, when a user wants to follow or unfollow certain users
 */
public class Follow extends Message {

    //region Fields
    /**
     * Boolean represents whether the Request is for following or unfollowing
     */
    private boolean isFollowing;
    /**
     * Short number represent the number of users in the Follow request
     */
    private short numberOfUsers;
    /**
     * List of String of users that the client want to follow or unfollow
     */
    private List<String> users;
    //endregion Fields

    /**
     * Default constructor
     * @param isFollowing           Byte represents whether the client wants to follow or unfollow.
     * @param numberOfUsers         Short number represents the number of users in the follow request.
     * @param users                 List of String represents the users the client wants to follow or unfollow.
     */
    public Follow(byte isFollowing, short numberOfUsers, List<String> users) {
        this.opcode = Opcode.FOLLOW;
        if(isFollowing == '\0'){
            //if the given byte is 0 --> follow
            this.isFollowing = true;
        }
        else{
            //else --> unfollow
            this.isFollowing = false;
        }
        this.numberOfUsers = numberOfUsers;
        this.users = users;

    }

    //region Getters
    public boolean isFollowing() {
        return isFollowing;
    }

    public short getNumberOfUsers() {
        return numberOfUsers;
    }

    public List<String> getUsers() {
        return users;
    }
    //endregion Getters

    /**
     * Convert all the data of this Follow message to a byte array.
     * @return      Byte array represent this Follow message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        byte[] opcode = this.shortToBytes(this.opcode.getCode());
        byte[] isFollowingBytes = new byte[1];
        if(isFollowing){
            //if the user requested to follow --> return 0 byte
            isFollowingBytes[0] = '\0';
        }
        else{
            //else --> return 1 byte.
            isFollowingBytes[0] = 1;
        }
        //converting the number of users to bytes array
        byte[] numberOfUsersBytes = this.shortToBytes((short)this.numberOfUsers);
        Vector<byte[]> usersNames = new Vector<>();
        int totalBytesOfUsers = 0;
        // converting each username to a byte array.
        for (String currentName:this.users) {
            byte[] currentNameArr = currentName.getBytes(StandardCharsets.UTF_8);
            usersNames.add(currentNameArr);
            totalBytesOfUsers += currentNameArr.length;
        }
        byte[] output = new byte[opcode.length + isFollowingBytes.length +
                numberOfUsersBytes.length + totalBytesOfUsers + this.numberOfUsers];
        int index = 0;
        //inserting all the data to single byte array to return.
        index = insertArray(opcode,output,index);
        index = insertArray(isFollowingBytes,output,index);
        index = insertArray(numberOfUsersBytes,output,index);
        for (byte[] currentUser:usersNames) {
            index = insertArray(currentUser,output,index);
            output[index] = '\0';
            index++;
        }
        return output;
    }

    /**
     * Generate matching Ack Message to this Follow Message Message according the Message data and server protocol.
     * @param numberOfUsers               Short number represents the amount of users in the given list
     * @param users                       List of Strings represents the users that were found by the server
     * @return              Ack message matching this Follow Message data of this message according to the server protocol.
     */
    public Ack generateAckMessage(short numberOfUsers,List<String> users ) {

            //converting the number of users to bytes array
            byte[] numberOfUsersBytes = this.shortToBytes(numberOfUsers);
            byte[][] elements = new byte[1 + (numberOfUsers*2)][];
            int index = 0;
            elements[index] = numberOfUsersBytes;
            index++;
            for(String currentUser : users){
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
