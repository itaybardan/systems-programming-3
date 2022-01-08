package bgu.spl.net.srv;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.api.bidi.Messages.Message;
import bgu.spl.net.api.bidi.Messages.Notification;
import bgu.spl.net.api.bidi.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataBase {
    private final AtomicInteger numberOfUsers;
    private final short YEAR;
    private final short MONTH;
    private final short DAY;

    private final ConcurrentHashMap<String, User> namesToRegisteredUsers;

    private final ConcurrentHashMap<Integer, User> namesToLoginUsers;

    private final List<Notification> messageHistory;
    private final Lock sendLock;
    private final Lock logLock;
    private final Lock userListLock;
    private final Lock registerLock;
    private final ReadWriteLock sendOrLogLock;
    private final ReadWriteLock registerOrUserListLock;

    public DataBase() {
        LocalDateTime now = LocalDateTime.now();
        this.YEAR = (short) now.getYear();
        this.MONTH = (short) now.getMonth().getValue();
        this.DAY = (short) now.getDayOfMonth();
        this.namesToRegisteredUsers = new ConcurrentHashMap<>();
        this.namesToLoginUsers = new ConcurrentHashMap<>();
        this.sendOrLogLock = new ReentrantReadWriteLock(true);
        this.sendLock = this.sendOrLogLock.readLock();
        this.logLock = this.sendOrLogLock.writeLock();
        this.registerOrUserListLock = new ReentrantReadWriteLock(true);
        this.userListLock = this.registerOrUserListLock.readLock();
        this.registerLock = this.registerOrUserListLock.writeLock();
        this.numberOfUsers = new AtomicInteger(0);
        this.messageHistory = new Vector<>();
    }

    public User getUserByName(String name) {
        return this.namesToRegisteredUsers.get(name);
    }

    public void registerUser(String userName, String password, short birthYear, short birthMonth, short birthDay) {
        this.registerLock.lock();
        int userNumber = this.generateUserNumber();

        //create new user with the given details and add it to the data base.
        short userAge = (short) (this.YEAR - birthYear);
        if (birthMonth > this.MONTH || (birthMonth == this.MONTH && birthDay > this.DAY)) userAge--;
        User newUser = new User(userName, password, userNumber, userAge);

        this.namesToRegisteredUsers.put(userName, newUser);
        this.registerLock.unlock();
    }

    public void loginUser(User toLogin) {
        this.logLock.lock();
        this.namesToLoginUsers.put(toLogin.getConnId(), toLogin);
        this.logLock.unlock();
    }

    public void logoutUser(int connId) {
        this.logLock.lock();
        this.namesToLoginUsers.get(connId).logout();
        this.namesToLoginUsers.remove(connId);
        this.logLock.unlock();
    }

    public boolean loginIsEmpty() {
        return this.namesToLoginUsers.isEmpty();
    }

    public User getConnectedUser(int connId) {
        return this.namesToLoginUsers.get(connId);
    }

    public Boolean followOrUnfollow(User toCheck, String user, boolean follow) {
        User current = this.namesToRegisteredUsers.get(user);
        if (current == null) return false;

        if (follow) {

            //if the wanted user is registered
            //updated the toCheck User following database
            if (!toCheck.getFollowing().contains(current) && !toCheck.getBlockedBy().contains(current)) {
                toCheck.addFollowing(current);
                current.addFollower(toCheck);
                return true;
            }

        } else {
            //unfollow

            if (toCheck.getFollowing().contains(current)) {
                toCheck.removeFollowing(current);
                current.removeFollower(toCheck);
                return true;
            }

        }
        return false;
    }

    /**
     * Save a Message that was sent by a certain user in the MessageHistory database
     *
     * @param toSave Notification Message represents the message to Save in the Message history database
     */
    public void addToHistory(Notification toSave) {
        this.messageHistory.add(toSave);
    }

    /**
     * Send Notification message from to a certain client
     *
     * @param connections  Connections object that holds all the connections handler of the server.
     * @param connectionID Integer represents the id of the recipient client in the connections object.
     * @param toSend       Notification message to send.
     */
    public void sendNotification(Connections<Message> connections, int connectionID, Notification toSend) {
        this.sendLock.lock();
        connections.send(connectionID, toSend);
        this.sendLock.unlock();
    }

    /**
     * Generate a unique new User number
     *
     * @return Integer represents the unique id of the current connecting user.
     */
    private int generateUserNumber() {
        return this.numberOfUsers.getAndIncrement();
    }

    /**
     * get all the UserNames of the users that are currently registered to the server.
     *
     * @return List of Strings represents the names of all the current registered users.
     */
    public List<User> returnRegisteredUsers(User connectedUser) {
        this.userListLock.lock();
        //getting the users and sorting them by their registration order
        List<User> users = new Vector<>(this.namesToRegisteredUsers.values());
        Collections.sort(users);
        List<User> registeredUsers = new Vector<>();
        for (User user : users) {
            //for each user --> add its name to the output list.
            if (!connectedUser.getBlockedBy().contains(user)) {
                registeredUsers.add(user);
            }
        }
        this.userListLock.unlock();
        return registeredUsers;
    }

    /**
     * calculates the number of posts of a certain user.
     *
     * @param postingUser String represents the user that the function needs to calculate his post number in the server.
     * @return Short number represents the amount of posts the posting user posted.
     */
    public short returnNumberOfPosts(String postingUser) {
        short output = 0;
        for (Notification msg : messageHistory) {
            if ((msg.getPrivateMessageOrPublicPost() == 1) && (msg.getPostingUser().equals(postingUser))) {
                output++;
            }
        }
        return output;
    }

}
