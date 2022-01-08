package bgu.spl.net.api.bidi.Messages;

public class Stat extends Message {

    private final char SEPARATOR = '|';

    private final String[] users;

    public Stat(String usersString) {
        this.opcode = Opcode.STAT;

        int usersAmm = (int) usersString.chars().filter(ch -> ch == SEPARATOR).count();
        int start = 0;
        int counter = 0;
        this.users = new String[usersAmm];
        for (int i = 0; i < usersString.length(); i++) {
            if (usersString.charAt(i) == SEPARATOR) {
                users[counter] = usersString.substring(start, i);
                counter++;
                start = i + 1;
            }
        }
    }

    public String[] getUsers() {
        return users;
    }

    @Override
    public byte[] convertMessageToBytes() {
        return null;
    }

    public Ack generateAckMessage(int size, short[] age, short[] numberOfPosts, short[] numberOfFollowers, short[] numberOfFollowing) {
        //converting the number of posts, number of followers and number of following to bytes arrays.
        int sizeActual = 1 + 5 * size;
        int j = 0;
        byte[][] elements = new byte[sizeActual][];
        elements[0] = shortToBytes((short) size);


        for (int i = 0; i < sizeActual - 1; i += 5) {
            byte[] ageBytes = this.shortToBytes(age[j]);
            byte[] numberOfPostsBytes = this.shortToBytes(numberOfPosts[j]);
            byte[] numberOfFollowersBytes = this.shortToBytes(numberOfFollowers[j]);
            byte[] numberOfFollowingBytes = this.shortToBytes(numberOfFollowing[j]);
            elements[i + 1] = ageBytes;
            elements[i + 2] = numberOfPostsBytes;
            elements[i + 3] = numberOfFollowersBytes;
            elements[i + 4] = numberOfFollowingBytes;

            byte[] separator = {'\0'};
            elements[i + 5] = separator;
            j++;
        }

        return new Ack(this.opcode, elements);

    }
}
