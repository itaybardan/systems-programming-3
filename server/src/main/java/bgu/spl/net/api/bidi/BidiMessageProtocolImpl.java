package bgu.spl.net.api.bidi;

import bgu.spl.net.api.bidi.Messages.*;
import bgu.spl.net.api.bidi.Messages.Error;
import bgu.spl.net.srv.bidi.DataManager;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BidiMessageProtocolImpl implements BidiMessagingProtocol<Message>  {

    /**
     * Boolean represents if the connection should terminate.
     */
    private boolean shouldTerminate;

    /**
     * Connections<Message> Represents the set of connections (connectionHandlers) currently connected.
     */
    private Connections<Message> connections;

    /**
     * DataManager Represents the shared data object.
     */
    private final DataManager dataManager;

    /**
     * Represents a read and write locks. Login and Logout are write locks, Post and PM are read lock.
     */
    private ReadWriteLock logOrSendLock;

    /**
     * Represents a read and write locks. Register is write lock, Userlist read lock.
     */
    private ReadWriteLock registerOrUserListLock;

    /**
     * Integer represents a personal connection ID of the current connectionHandler that holds this protocol.
     */
    private int connectionID;

    public BidiMessageProtocolImpl(DataManager dataManager, ReadWriteLock logOrSendLock, ReadWriteLock registerOrUserListLock) {
        this.dataManager = dataManager;
        this.registerOrUserListLock = registerOrUserListLock;
        this.logOrSendLock = logOrSendLock;
        this.shouldTerminate = false;
    }

    /**
     * Used to initiate the current client protocol with it's personal connection ID and the connections implementation
     **/
    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionID = connectionId;
        this.connections = connections;
    }

    /**
     * @return true if the connection should be terminated
     */
    @Override
    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }

    /**
     * Processes the given message.
     * @param message   Represents the message to be processed.
     */
    @Override
    public void process(Message message) {
        final Message  msg = message;
        Runnable currentProcess;
        if(msg.getOpcode() == Message.Opcode.REGISTER){
            currentProcess = () -> registerFunction((Register)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.LOGIN){
            currentProcess = () -> loginFunction((Login)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.LOGOUT){
            currentProcess = () -> logoutFunction((Logout)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.FOLLOW){
            currentProcess = () -> followFunction((Follow)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.POST){
            currentProcess = () -> postFunction((Post)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.PM){
            currentProcess = () -> pmFunction((PM)msg);
        }
        else if (msg.getOpcode() == Message.Opcode.USERLIST){
            currentProcess = () -> userListFunction((UserList)msg);
        }
        else{
            //stat message
            currentProcess = () -> statFunction((Stat)msg);
        }
        currentProcess.run();
    }

    /**
     * Is called when user requests to register to the server. Register him unless alreardy been registed.
     * @param registerMsg   Represents a Register message to be processed.
     */
    private void registerFunction (Register registerMsg){
        this.registerOrUserListLock.writeLock().lock(); // Register is considered as a write event.
        if(this.dataManager.getUserByName(registerMsg.getUsername()) != null){
            //if the user is already registered - return error message.
            this.connections.send(this.connectionID,new Error(registerMsg.getOpcode()));
        }
        else{
            this.dataManager.registerUser(registerMsg.getUsername(),registerMsg.getPassword());
            this.connections.send(this.connectionID,registerMsg.generateAckMessage());
        }
        this.registerOrUserListLock.writeLock().unlock();
    }

    /**
     * Is called when user requests to login to the server. Logs in, unless already logged in, not registered or password doesn't match.
     * @param loginMsg   Represents a Login message to be processed.
     */
    private void loginFunction (Login loginMsg){
        this.logOrSendLock.writeLock().lock(); // Login is considered as a write event.
        User checkIfAlreadyConnected = this.dataManager.getConnectedUser(this.connectionID);
        if(checkIfAlreadyConnected != null){
            this.connections.send(this.connectionID,new Error(loginMsg.getOpcode()));
        }
        else{
            User toCheck = this.dataManager.getUserByName(loginMsg.getUsername());
            if((toCheck == null) || (!toCheck.getPassword().equals(loginMsg.getPassword())) || (toCheck.isConnected())){
                //If the user is not registered \ password doesnt match \ is already connected --> return error message.
                this.connections.send(this.connectionID, new Error(loginMsg.getOpcode()));
            }
            else{
                this.connections.send(connectionID,loginMsg.generateAckMessage());
                synchronized (toCheck.getWaitingMessages()){
                    int size = toCheck.getWaitingMessages().size();
                    for(int i = 0; i < size; i++){
                        //Sending to the user all the messages that were waiting for him\her
                        Message current = toCheck.getWaitingMessages().poll();
                        this.connections.send(this.connectionID,current);
                    }
                    //setting the connection value to true
                    toCheck.login(this.connectionID);
                    this.dataManager.loginUser(toCheck);
                }
            }
        }
        this.logOrSendLock.writeLock().unlock();
    }

    /**
     * Is called when user requests to logout of the server. Logs out, unless no user is logged in.
     * @param logoutMsg   Represents a Logout message to be processed.
     */
    private void logoutFunction (Logout logoutMsg){
        this.logOrSendLock.writeLock().lock(); // Logout is considered as a write event.
        if(this.dataManager.loginIsEmpty()){
            this.connections.send(this.connectionID,new Error(logoutMsg.getOpcode()));
        }
        else{
            this.dataManager.logoutUser(this.connectionID);
            this.connections.send(this.connectionID,logoutMsg.generateAckMessage());
            this.connections.disconnect(this.connectionID);
        }
        this.logOrSendLock.writeLock().unlock();
    }

    /**
     * Is called when a user requests to follow or unfollow other users in the server. Follows/unfollows a user, unless
     * already following/unfollowing him, or the user (the message sender) is not logged in.
     * @param followMsg   Represents a Follow message to be processed.
     */
    private void followFunction (Follow followMsg){
        User toCheck = this.dataManager.getConnectedUser(this.connectionID);
        if(toCheck == null){
            this.connections.send(this.connectionID,new Error(followMsg.getOpcode()));
        }
        else{
            List<String> successful = this.dataManager.followOrUnfollow(toCheck,followMsg.getUsers(),followMsg.isFollowing());
            if(successful.isEmpty()){
                //If no one of the requested users were followed \ unfollowed successfully --> send error.
                this.connections.send(this.connectionID,new Error(followMsg.getOpcode()));
            }
            else{
                short amount = (short)successful.size();
                this.connections.send(this.connectionID,followMsg.generateAckMessage(amount,successful));
            }
        }
    }

    /**
     * Is called when a user wants to post a public message. Posts it unless the user isn't logged in.
     * @param postMsg   Represents a Post message to be processed.
     */
    private void postFunction (Post postMsg){
        this.logOrSendLock.readLock().lock(); // Post is considered as a read event.
        User sender = this.dataManager.getConnectedUser(this.connectionID);
        if(sender == null){
            //the user is not logged in --> send error message
            this.connections.send(this.connectionID,new Error(postMsg.getOpcode()));
        }
        else{
            //if the user is logged in
            List<User> users = new Vector<>();
            searchingForUsersInMessage(postMsg, sender, users);
            //adding all the followers of the sender to the list
            users.addAll(sender.getFollowers());
            Notification toSend = new Notification((byte)1,sender.getUserName(),postMsg.getContent());
            for(User currentUser:users){
                //send notification to each user
                if(currentUser.isConnected()){
                    //if the user is connected --> send the notification
                    this.dataManager.sendNotification(this.connections,currentUser.getConnId(),toSend);
                }
                else{
                    //else --> send the message to the waiting queue of that user.
                    currentUser.getWaitingMessages().add(toSend);
                }
            }
            this.dataManager.addToHistory(toSend);
            this.connections.send(this.connectionID,postMsg.generateAckMessage());
        }
        this.logOrSendLock.readLock().unlock();
    }

    /**
     * Goes through the message to be posted and find tagged users .Belongs to "postFunction" function.
     * @param postMsg       Represents the Post message to be searched.
     * @param sender        Represents the user that sent the Post message.
     * @param users         Represents the list of users to send the Post message to.
     */
    private void searchingForUsersInMessage(Post postMsg, User sender, List<User> users) {
        String[] contentWords = postMsg.getContent().split(" ");
        for (String contentWord : contentWords) {
            if (contentWord.contains("@")) {
                //need to search the tagged user
                String currentUserName = contentWord.substring(contentWord.indexOf("@")+1);
                User currentUser = this.dataManager.getUserByName(currentUserName);
                if ((currentUser != null)&&(!users.contains(currentUser))) {
                    //only if the Current tagged user is registered
                    if (!sender.getFollowers().contains(currentUser)) {
                        //only if the current user is not following the sender already
                        users.add(currentUser);
                    }
                }
            }
        }
    }

    /**
     * Is called when a user wants to send a private message. Sends it unless the user isn't logged in or the recipient
     * is not registered.
     * @param pmMsg   Represents a PM message to be processed.
     */
    private void pmFunction (PM pmMsg){
        this.logOrSendLock.readLock().lock(); // PM is considered as a read event.
        User sender = this.dataManager.getConnectedUser(this.connectionID);
        User recipient = this.dataManager.getUserByName(pmMsg.getUserName());
        if((sender == null) || (recipient == null)){
            //the user is not logged in or recipient is not registered --> send error message
            this.connections.send(this.connectionID,new Error(pmMsg.getOpcode()));
        }
        else{
            Notification toSend = new Notification((byte)0,sender.getUserName(),pmMsg.getContent());
            if(recipient.isConnected()){
                this.dataManager.sendNotification(this.connections,recipient.getConnId(),toSend);
            }
            else{
                recipient.getWaitingMessages().add(toSend);
            }
            this.dataManager.addToHistory(toSend);
        }
        this.connections.send(this.connectionID, pmMsg.generateAckMessage());
        this.logOrSendLock.readLock().unlock();
    }

    /**
     * Is called when a user wants to get a list of all registered users. Gets it unless he isn't logged in.
     * @param userListMsg   Represents a Userlist message to be processed.
     */
    private void userListFunction(UserList userListMsg){
        this.registerOrUserListLock.readLock().lock(); // Userlist is considered as a read event.
        User user = this.dataManager.getConnectedUser(this.connectionID);
        if(user == null){
            this.connections.send(this.connectionID,new Error(userListMsg.getOpcode()));
        }
        else{
            List<String> registeredUsers = this.dataManager.returnRegisteredUsers();
            Message toSend = userListMsg.generateAckMessage((short)registeredUsers.size(),registeredUsers);
            this.connections.send(this.connectionID,toSend);
        }
        this.registerOrUserListLock.readLock().unlock();
    }

    /**
     * Is called to receive data on a certain user (number of posts a user posted, number of followers,
     * number of users the user is following). Gets the data unless the user (that asks for the data) isn't logged in.
     * @param statMsg   Represents a Stat message to be processed.
     */
    private void statFunction (Stat statMsg){
        User currentClient = this.dataManager.getConnectedUser(this.connectionID);
        User user = this.dataManager.getUserByName(statMsg.getUsername());
        if((user == null)||(currentClient == null)){
            //if the requesting user is not logged in OR if the user in the request does not exist --> send error
            this.connections.send(this.connectionID, new Error(statMsg.getOpcode()));
        }
        else{
            short numberOfPosts = this.dataManager.returnNumberOfPosts(user.getUserName());
            short followers = (short)user.getFollowers().size();
            short following = (short)user.getFollowing().size();
            this.connections.send(this.connectionID, statMsg.generateAckMessage(numberOfPosts,followers,following));
        }

    }
}
