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

    /**
     * Atomic Integer represent the number of users that are currently registered to the Server.
     */
    private AtomicInteger numberOfUsers;

    /**
     * ConcurrentHashMap from String keys(UserNames) to User Objects,
     * represents the data base of registered users in the server.
     */
    private ConcurrentHashMap<String, User> namesToRegisteredUsers;
    /**
     * ConcurrentHashMap from Integer keys(connection id) to User Objects,
     * represents the data base of the users who are currently logged to the server.
     */
    private ConcurrentHashMap<Integer, User> namesToLoginUsers;

    /**
     * List Of Notifications represents all the PM and Posts messages that were sent to the Server so far..
     */
    private List<Notification> messageHistory;

    /**
     * ReadWriteLock to synchronized between sending a message event to login or logout event.
     */
    private ReadWriteLock sendOrLogLock;

    /**
     * Read Lock of the sendOrLogLock -> the send event is registered as a "read" event.
     */
    private Lock sendLock;

    /**
     * Write Lock of the sendOrLogLock -> the login and logout events are registered as a "write" events.
     */
    private Lock logLock;

    /**
     * ReadWriteLock to synchronized between sending a message event to login or logout event.
     */
    private ReadWriteLock registerOrUserListLock;
    /**
     * Read Lock of the registerOrUserListLock -> the UserList event is registered as a "read" event.
     */
    private Lock userListLock;
    /**
     * Write Lock of the registerOrUserListLock -> the Register event is registered as a "write" event.
     */
    private Lock registerLock;

    /**
     * Default Constructor
     */
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

    /**
     * Get the User Object That match the given name.
     * @param name          String represents the wanted UserName.
     * @return          User that match the given user name.
     *                  If the user was not found in the data base --> returns null.
     */
    public User getUserByName(String name){
        return this.namesToRegisteredUsers.get(name);
    }

    /**
     * Creates and add a new user to the registered users data base.
     * @param userName          String represents the new user Name.
     * @param password          String represents the new user password.
     */
    public void registerUser(String userName, String password){
        //making sure no one try to register at the same time or use the UserList function at the same time.
        this.registerLock.lock();
        int userNumber = this.generateUserNumber();
        //create new user with the given details and add it to the data base.
        User newUser = new User(userName,password,userNumber);
        this.namesToRegisteredUsers.put(userName, newUser);
        this.registerLock.unlock();
    }


    /**
     * add a certain user to the Logged in users data base.
     * @param toLogin           User Object to add to the logged in Users data base.
     */
    public void loginUser(User toLogin){
        this.logLock.lock();
        this.namesToLoginUsers.put(toLogin.getConnId(),toLogin);
        this.logLock.unlock();
    }

    /**
     * Removing the User who matches the given connection Id from the logged in data base.
     * @param connId
     */
    public void logoutUser(int connId){
        this.logLock.lock();
        this.namesToLoginUsers.get(connId).logout();
        this.namesToLoginUsers.remove(connId);
        this.logLock.unlock();
    }

    /**
     * checks if there is any user logged in to the server.
     * @return      'true' if the there are no users currently connected, 'else' otherwise.
     */
    public boolean loginIsEmpty(){
        return this.namesToLoginUsers.isEmpty();
    }

    /**
     * Get a logged in User that matches the given connection id
     * @param connId            Integer represents the connection Id of the wanted user.
     * @return      Logged in User object that matches the given connection id.
     *              If there is no user that matches the given id --> return null.
     *
     */
    public User getConnectedUser(int connId){
        return this.namesToLoginUsers.get(connId);
    }

    /**
     * Editing a given user to Follow or Unfollow a collection of users according to the given parameters.
     * @param toCheck               User Object to edit his Follow and Unfollow List.
     * @param users                 List of String represents UserNames  to follow or unfollow.
     * @param follow                Boolean represents whether to follow or unfollow the users in the list.
     * @return          List of String represents all the users which was successfully followed or unfollowed
     */
    public List<String> followOrUnfollow(User toCheck,List<String> users,boolean follow){
        List<String> successful = new Vector<>();
        if(follow){
            //if it was a follow request
            for(String currentUser:users){
                //for each name in the given list
                User current = this.namesToRegisteredUsers.get(currentUser);
                if(current != null){
                    //if the wanted user is registered
                        //updated the toCheck User following database
                        if(!toCheck.getFollowing().contains(current)){
                            toCheck.addFollowing(current);
                            current.addFollower(toCheck);
                            successful.add(currentUser);
                    }
                }
            }
        }
        else{
            //unfollow
            for(String currentUser:users){
                User current = this.namesToRegisteredUsers.get(currentUser);
                if(current != null){
                        if(toCheck.getFollowing().contains(current)){
                            toCheck.removeFollowing(current);
                            current.removeFollower(toCheck);
                            successful.add(currentUser);
                    }

                }
            }
        }
        return successful;
    }

    /**
     * Save a Message that was sent by a certain user in the MessageHistory database
     * @param toSave        Notification Message represents the message to Save in the Message history database
     */
    public void addToHistory(Notification toSave){
        this.messageHistory.add(toSave);
    }

    /**
     * Send Notification message from to a certain client
     * @param connections                   Connections object that holds all the connections handler of the server.
     * @param connectionID                  Integer represents the id of the recipient client in the connections object.
     * @param toSend                        Notification message to send.
     */
    public void sendNotification(Connections<Message> connections, int connectionID, Notification toSend){
        this.sendLock.lock();
        connections.send(connectionID,toSend);
        this.sendLock.unlock();
    }

    /**
     * Generate a unique new User number
     * @return              Integer represents the unique id of the current connecting user.
     */
    private int generateUserNumber(){
        return this.numberOfUsers.getAndIncrement();
    }

    /**
     * get all the UserNames of the users that are currently registered to the server.
     * @return      List of Strings represents the names of all the current registered users.
     */
    public List<String> returnRegisteredUsers(){
        this.userListLock.lock();
        //getting the users and sorting them by their registration order
        List<User> users = new Vector<>(this.namesToRegisteredUsers.values());
        Collections.sort(users);
        List<String> registeredUsers = new Vector<>();
        for (User user:users) {
            //for each user --> add it's name to the output list.
            registeredUsers.add(user.getUserName());
        }
        this.userListLock.unlock();
        return registeredUsers;
    }

    /**
     * calculates the number of posts of a certain user.
     * @param postingUser       String represents the user that the function needs to calculate his post number in the server.
     * @return           Short number represents the amount of posts the posting user posted.
     */
    public short returnNumberOfPosts(String postingUser){
        short output = 0;
        for(Notification msg:messageHistory){
            if((msg.getPrivateMessageOrPublicPost() == 1) && (msg.getPostingUser().equals(postingUser))){
                output++;
            }
        }
        return output;
    }

}
