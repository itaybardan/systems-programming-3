package bgu.spl.net.srv.bidi;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.api.bidi.Messages.Message;
import bgu.spl.net.api.bidi.Messages.Notification;
import bgu.spl.net.api.bidi.User;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DataBase shared between all the Connections for the BGSServer.
 * holds all the information of users that are registered or logged in to the server.
 * in charge of data manipulations on the database
 */
public class DataManager {

    private final AtomicInteger numberOfUsers;

    private final ConcurrentHashMap<String, User> namesToRegisteredUsers;

    private final ConcurrentHashMap<Integer, User> namesToLoginUsers;

    private final List<Notification> messageHistory;

    private ReadWriteLock sendOrLogLock;

    private final Lock sendLock;

    private final Lock logLock;

    private ReadWriteLock registerOrUserListLock;

    private final Lock userListLock;

    private final Lock registerLock;

    public DataManager() {
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

    public void registerUser(String userName, String password) {
        //making sure no one try to register at the same time or use the UserList function at the same time.
        this.registerLock.lock();
        int userNumber = this.generateUserNumber();
        //create new user with the given details and add it to the data base.
        User newUser = new User(userName, password, userNumber);
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


    public List<String> followOrUnfollow(User toCheck, List<String> users, boolean follow) {
        List<String> successful = new Vector<>();
        if (follow) {
            //if it was a follow request
            for (String currentUser : users) {
                //for each name in the given list
                User current = this.namesToRegisteredUsers.get(currentUser);
                if (current != null) {
                    //if the wanted user is registered
                    //updated the toCheck User following database
                    if (!toCheck.getFollowing().contains(current) && !toCheck.getBlockedBy().contains(current)) {
                        toCheck.addFollowing(current);
                        current.addFollower(toCheck);
                        successful.add(currentUser);
                    }
                }
            }
        } else {
            //unfollow
            for (String currentUser : users) {
                User current = this.namesToRegisteredUsers.get(currentUser);
                if (current != null) {
                    if (toCheck.getFollowing().contains(current)) {
                        toCheck.removeFollowing(current);
                        current.removeFollower(toCheck);
                        successful.add(currentUser);
                    }

                }
            }
        }
        return successful;
    }


    public void addToHistory(Notification toSave) {
        this.messageHistory.add(toSave);
    }


    public void sendNotification(Connections<Message> connections, int connectionID, Notification toSend) {
        this.sendLock.lock();
        connections.send(connectionID, toSend);
        this.sendLock.unlock();
    }

    private int generateUserNumber() {
        return this.numberOfUsers.getAndIncrement();
    }


    public List<String> returnRegisteredUsers(User connectedUser) {
        this.userListLock.lock();
        //getting the users and sorting them by their registration order
        List<User> users = new Vector<>(this.namesToRegisteredUsers.values());
        Collections.sort(users);
        List<String> registeredUsers = new Vector<>();
        for (User user : users) {
            //for each user --> add its name to the output list.
            if (!connectedUser.getBlockedBy().contains(user)) {
                registeredUsers.add(user.getUserName());
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
