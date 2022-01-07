package bgu.spl.net.api.bidi.Messages;

/**
 * Message Of type STAT of Client-To-Server communication, when a user wants to check the Status of certain users.
 */
public class Stat extends Message {

    //region Fields
    /**
     * String represents the usersthat the client want to check.
     */
    private String[] users;
    private final char SEPARATOR = '|';
    //endregion Fields

    /**
     * Default Constructor.
     *
     * @param usersString String that represents the users that the client want to check.
     */
    public Stat(String usersString) {
        this.opcode = Opcode.STAT;

        int usersAmm = (int)usersString.chars().filter(ch -> ch == SEPARATOR).count();
        int start=0;
        int counter=0;
        this.users = new String[usersAmm];
        for (int i = 0; i < usersString.length(); i++) {
            if(usersString.charAt(i)== SEPARATOR) {
                users[counter] = usersString.substring(start, i);
                counter++;
                start = i+1;
            }
        }
    }

    //region Getters
    public String[] getUsers() {
        return users;
    }
    //endregion Getters

    /**
     * Convert all the data of this Stat message to a byte array.
     *
     * @return Byte array represent this Stat message in the right order according to the server protocol
     */
    @Override
    public byte[] convertMessageToBytes() {
        return null;
    }

    /**
     * Generate matching Ack Message to this Stat Message Message according the Message data and server protocol.
     *
     * @param numberOfPosts     short[] represents the number of posts of the requested users.
     * @param numberOfFollowers short[] represents the number of followers of the requested users.
     * @param numberOfFollowing short[] represents the number of users following of the requested users.
     * @return Ack message matching this Stat Message data of this message according to the server protocol.
     *
     */
    public Ack generateAckMessage(int size, short[] age, short[] numberOfPosts, short[] numberOfFollowers, short[] numberOfFollowing) {
        //converting the number of posts, number of followers and number of following to bytes arrays.
        int sizeActual =1 + 5*size;
        int j=0;
        byte[][] elements = new byte[sizeActual][];
        elements[0] = shortToBytes((short)size);


        for (int i = 0; i < sizeActual-1; i+=5) {
            byte[] ageBytes = this.shortToBytes(age[j]);
            byte[] numberOfPostsBytes = this.shortToBytes(numberOfPosts[j]);
            byte[] numberOfFollowersBytes = this.shortToBytes(numberOfFollowers[j]);
            byte[] numberOfFollowingBytes = this.shortToBytes(numberOfFollowing[j]);
            elements[i+1] = ageBytes;
            elements[i+2] = numberOfPostsBytes;
            elements[i+3] = numberOfFollowersBytes;
            elements[i+4] = numberOfFollowingBytes;

            byte[] separator = {'\0'};
            elements[i+5] = separator;
            j++;
        }

        //inserting all the array to the elements 2-D array of bytes
        return new Ack(this.opcode, elements);

    }
}
