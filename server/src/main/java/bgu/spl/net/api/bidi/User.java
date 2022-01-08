package bgu.spl.net.api.bidi;

import bgu.spl.net.api.bidi.Messages.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


public class User implements Comparable<User> {

    private static final int DISCONNECTED_ID = -1;

    private final int userId;
    private final String userName;
    private final String password;
    private final Set<User> following;
    private final Set<User> followers;
    private final ConcurrentLinkedQueue<Message> waitingMessages;
    private final Set<User> blockedBy;
    private int connId;
    private final short age;
    private volatile boolean isConnected;

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

    public short getAge() {
        return age;
    }

    /**
     * Return a copy of the current users this user is following
     *
     * @return Set Of User Objects that this user is following.
     */
    public Set<User> getFollowing() {
        return new HashSet<>(following);
    }

    public short getFollowingAmm() {
        return (short) following.size();
    }

    public short getFollowersAmm() {
        return (short) followers.size();
    }

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

    public synchronized void addFollower(User toAdd) {
        this.followers.add(toAdd);
    }


    public synchronized void addFollowing(User toAdd) {
        this.following.add(toAdd);
    }

    public synchronized void removeFollower(User toRemove) {
        this.followers.remove(toRemove);
    }

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

    public Boolean isBlockedBy(User user) {
        return this.blockedBy.contains(user);
    }

    public void logout() {
        this.isConnected = false;
        this.connId = DISCONNECTED_ID;
    }


    public void login(int connId) {
        this.isConnected = true;
        this.connId = connId;
    }

    @Override
    public int compareTo(User user) {
        return Integer.compare(this.userId, user.userId);
    }

}
