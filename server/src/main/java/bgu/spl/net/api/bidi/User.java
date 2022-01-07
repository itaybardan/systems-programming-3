package bgu.spl.net.api.bidi;

import bgu.spl.net.api.bidi.Messages.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class represents a user that is registered or connected to the  BGSServer
 */
public class User implements Comparable<User> {

    //region Fields

    /**
     * Integer represents a Disconnected from the server status.
     */
    private static final int DISCONNECTED_ID = -1;

    /**
     * Integer represents the unique id of this user. used to sort users by their registration order.
     */
    private final int userId;

    /**
     * Integer represents the Connection Id of the connection handler that this user is currently connected to.
     */
    private int connId;

    /**
     * String represent this User Name
     */
    private final String userName;

    /**
     * String represents this User Password.
     */
    private final String password;

    private short age;

    /**
     * Set Of User Objects represents the Users this User is Following after.
     */
    private final Set<User> following;

    /**
     * Set Of User Objects represent the Users that follows this user.
     */
    private final Set<User> followers;

    /**
     * Boolean represent whether this user is connected or not.
     */
    private volatile boolean isConnected;

    /**
     * Queue of Message Objects represents the Messages that was sent to this user when he\she was logged out.
     * those messages will be sent to him\her in the next login.
     */
    private final ConcurrentLinkedQueue<Message> waitingMessages;

    private final Set<User> blockedBy;

    //endregion Fields

    /**
     * Default Constructor.
     *
     * @param userName String represents this User Name.
     * @param password String represents this user Password.
     * @param userNum  Integer represents the unique id of this user.
     */
    public User(String userName, String password, int userNum, short age) {
        this.connId = DISCONNECTED_ID;
        this.userName = userName;
        this.password = password;
        this.age = age;
        this.isConnected = false;
        this.following = new HashSet<>();
        this.followers = new HashSet<>();
        this.waitingMessages = new ConcurrentLinkedQueue<>();
        this.userId = userNum;
        this.blockedBy = new HashSet<>();

    }

    //region Getters
    public int getConnId() {
        return connId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public short getAge() { return age;}

    /**
     * Return a copy of the current users this user is following
     *
     * @return Set Of User Objects that this user is following.
     */
    public Set<User> getFollowing() {
        return new HashSet<>(following);
    }

    public short getFollowingAmm() {return (short) following.size();}

    public short getFollowersAmm() {return (short) followers.size();}

    /**
     * Return a copy of the current users that follows this user.
     *
     * @return Set Of User Objects that follows this user.
     */
    public Set<User> getFollowers() {
        return new HashSet<>(followers);
    }


    public boolean isConnected() {
        return isConnected;
    }

    public ConcurrentLinkedQueue<Message> getWaitingMessages() {
        return waitingMessages;
    }

    /**
     * Add a User to the Followers list.
     *
     * @param toAdd User object represents user that currently follow this User
     */
    public synchronized void addFollower(User toAdd) {
        this.followers.add(toAdd);
    }

    /**
     * Add a User to the Following list.
     *
     * @param toAdd User Objects represents a user that this User is currently following
     */
    public synchronized void addFollowing(User toAdd) {
        this.following.add(toAdd);
    }

    /**
     * remove a User to the Followers list.
     *
     * @param toRemove User object represents user that currently unfollow this User
     */
    public synchronized void removeFollower(User toRemove) {
        this.followers.remove(toRemove);
    }

    /**
     * remove a User to the Followers list.
     *
     * @param toRemove User Object represents a user that this user is currently unfllowed
     */
    public synchronized void removeFollowing(User toRemove) {
        this.following.remove(toRemove);
    }

    //endregion Getters

    public Set<User> getBlockedBy() {
        return this.blockedBy;
    }

    public void addBlockedBy(User user) {
        this.blockedBy.add(user);
    }

    public Boolean isBlockedBy(User user){
        return this.blockedBy.contains(user);
    }

    /**
     * updating fields of this user after logging out.
     */
    public void logout() {
        this.isConnected = false;
        this.connId = DISCONNECTED_ID;
    }

    /**
     * updates the fields of this user after logging in
     *
     * @param connId Integer represents the connection id of the Connections handler this clients is using in the server.
     */
    public void login(int connId) {
        this.isConnected = true;
        this.connId = connId;
    }

    /**
     * Compares to Users according to their registration order
     *
     * @param user User Object to compare to this one.
     * @return '-1' if this user registered before the other user,
     * '0' if they registered in the same time
     * '1' if this user registered after the other user.
     */
    @Override
    public int compareTo(User user) {
        return Integer.compare(this.userId, user.userId);
    }

}
