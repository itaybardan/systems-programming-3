package bgu.spl.net.api.bidi;

import bgu.spl.net.api.bidi.Messages.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


public class User implements Comparable<User> {

    private static final int DISCONNECTED_ID = -1;

    private final int userId;

    private int connId;

    private final String userName;

    private final String password;

    private short age;

    private final Set<User> following;

    private final Set<User> followers;

    private volatile boolean isConnected;

    private final ConcurrentLinkedQueue<Message> waitingMessages;

    private final Set<User> blockedBy;

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

    public int getConnId() {
        return connId;
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
