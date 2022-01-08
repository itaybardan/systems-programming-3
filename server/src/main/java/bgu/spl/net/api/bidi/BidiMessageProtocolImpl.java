package bgu.spl.net.api.bidi;

import bgu.spl.net.api.bidi.Messages.Error;
import bgu.spl.net.api.bidi.Messages.*;
import bgu.spl.net.srv.DataBase;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;

public class BidiMessageProtocolImpl implements BidiMessagingProtocol<Message> {


    private final DataBase dataBase;

    private final boolean shouldTerminate;

    private Connections<Message> connections;

    private final ReadWriteLock logOrSendLock;

    private final ReadWriteLock registerOrLogStatLock;

    private int connectionID;

    public BidiMessageProtocolImpl(DataBase dataBase, ReadWriteLock logOrSendLock, ReadWriteLock registerOrLogStatLock) {
        this.dataBase = dataBase;
        this.registerOrLogStatLock = registerOrLogStatLock;
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
     *
     * @param message Represents the message to be processed.
     */
    @Override
    public void process(Message message) {
        final Message msg = message;
        Runnable currentProcess;
        if (msg.getOpcode() == Message.Opcode.REGISTER) {
            currentProcess = () -> registerFunction((Register) msg);
        } else if (msg.getOpcode() == Message.Opcode.LOGIN) {
            currentProcess = () -> loginFunction((Login) msg);
        } else if (msg.getOpcode() == Message.Opcode.LOGOUT) {
            currentProcess = () -> logoutFunction((Logout) msg);
        } else if (msg.getOpcode() == Message.Opcode.FOLLOW) {
            currentProcess = () -> followFunction((Follow) msg);
        } else if (msg.getOpcode() == Message.Opcode.POST) {
            currentProcess = () -> postFunction((Post) msg);
        } else if (msg.getOpcode() == Message.Opcode.PM) {
            currentProcess = () -> pmFunction((PM) msg);
        } else if (msg.getOpcode() == Message.Opcode.LOGSTAT) {
            currentProcess = () -> logstatFunction((LogStat) msg);
        } else if (msg.getOpcode() == Message.Opcode.STAT) {
            currentProcess = () -> statFunction((Stat) msg);
        } else {
            currentProcess = () -> blockFunction((Block) msg);
        }
        currentProcess.run();
    }

    /**
     * Is called when user requests to register to the server. Register him unless alreardy been registed.
     *
     * @param registerMsg Represents a Register message to be processed.
     */
    private void registerFunction(Register registerMsg) {
        this.registerOrLogStatLock.writeLock().lock(); // Register is considered as a write event.
        if (this.dataBase.getUserByName(registerMsg.getUsername()) != null) {
            //if the user is already registered - return error message.
            this.connections.send(this.connectionID, new Error(registerMsg.getOpcode()));
        } else {
            this.dataBase.registerUser(registerMsg.getUsername(), registerMsg.getPassword(), registerMsg.getYear(), registerMsg.getMonth(), registerMsg.getDay());
            this.connections.send(this.connectionID, registerMsg.generateAckMessage());
        }
        this.registerOrLogStatLock.writeLock().unlock();
    }

    /**
     * Is called when user requests to login to the server. Logs in, unless already logged in, not registered or password doesn't match.
     *
     * @param loginMsg Represents a Login message to be processed.
     */
    private void loginFunction(Login loginMsg) {
        this.logOrSendLock.writeLock().lock(); // Login is considered as a write event.
        User checkIfAlreadyConnected = this.dataBase.getConnectedUser(this.connectionID);
        if (checkIfAlreadyConnected != null || loginMsg.captcha == '0') {
            this.connections.send(this.connectionID, new Error(loginMsg.getOpcode()));
        } else {
            User toCheck = this.dataBase.getUserByName(loginMsg.getUsername());
            if ((toCheck == null) || (!toCheck.getPassword().equals(loginMsg.getPassword())) || (toCheck.isConnected())) {
                //If the user is not registered \ password doesnt match \ is already connected --> return error message.
                this.connections.send(this.connectionID, new Error(loginMsg.getOpcode()));
            } else {
                this.connections.send(connectionID, loginMsg.generateAckMessage());
                synchronized (toCheck.getWaitingMessages()) {
                    int size = toCheck.getWaitingMessages().size();
                    for (int i = 0; i < size; i++) {
                        //Sending to the user all the messages that were waiting for him\her
                        Message current = toCheck.getWaitingMessages().poll();
                        this.connections.send(this.connectionID, current);
                    }
                    //setting the connection value to true
                    toCheck.login(this.connectionID);
                    this.dataBase.loginUser(toCheck);
                }
            }
        }
        this.logOrSendLock.writeLock().unlock();
    }

    /**
     * Is called when user requests to logout of the server. Logs out, unless no user is logged in.
     *
     * @param logoutMsg Represents a Logout message to be processed.
     */
    private void logoutFunction(Logout logoutMsg) {
        this.logOrSendLock.writeLock().lock(); // Logout is considered as a write event.
        if (this.dataBase.loginIsEmpty()) {
            this.connections.send(this.connectionID, new Error(logoutMsg.getOpcode()));
        } else {
            this.dataBase.logoutUser(this.connectionID);
            this.connections.send(this.connectionID, logoutMsg.generateAckMessage());
            this.connections.disconnect(this.connectionID);
        }
        this.logOrSendLock.writeLock().unlock();
    }

    /**
     * Is called when a user requests to follow or unfollow other users in the server. Follows/unfollows a user, unless
     * already following/unfollowing him, or the user (the message sender) is not logged in.
     *
     * @param followMsg Represents a Follow message to be processed.
     */
    private void followFunction(Follow followMsg) {
        User toCheck = this.dataBase.getConnectedUser(this.connectionID);
        if (toCheck == null) {
            this.connections.send(this.connectionID, new Error(followMsg.getOpcode()));
        } else {
            Boolean successful = this.dataBase.followOrUnfollow(toCheck, followMsg.getUser(), followMsg.isFollowing());
            if (!successful) {
                //If no one of the requested users were followed \ unfollowed successfully --> send error.
                this.connections.send(this.connectionID, new Error(followMsg.getOpcode()));
            } else {
                this.connections.send(this.connectionID, followMsg.generateAckMessage(followMsg.getUser()));
            }
        }
    }

    /**
     * Is called when a user wants to post a public message. Posts it unless the user isn't logged in.
     *
     * @param postMsg Represents a Post message to be processed.
     */
    private void postFunction(Post postMsg) {
        this.logOrSendLock.readLock().lock(); // Post is considered as a read event.
        User sender = this.dataBase.getConnectedUser(this.connectionID);
        if (sender == null) {
            //the user is not logged in --> send error message
            this.connections.send(this.connectionID, new Error(postMsg.getOpcode()));
        } else {
            //if the user is logged in
            List<User> users = new Vector<>();
            searchingForUsersInMessage(postMsg, sender, users);
            //adding all the followers of the sender to the list
            users.addAll(sender.getFollowers());
            Notification toSend = new Notification((byte) 1, sender.getUserName(), postMsg.getContent());
            for (User currentUser : users) {
                //send notification to each user
                if (currentUser.isConnected()) {
                    //if the user is connected --> send the notification
                    this.dataBase.sendNotification(this.connections, currentUser.getConnId(), toSend);
                } else {
                    //else --> send the message to the waiting queue of that user.
                    currentUser.getWaitingMessages().add(toSend);
                }
            }
            this.dataBase.addToHistory(toSend);
            this.connections.send(this.connectionID, postMsg.generateAckMessage());
        }
        this.logOrSendLock.readLock().unlock();
    }

    /**
     * Goes through the message to be posted and find tagged users .Belongs to "postFunction" function.
     *
     * @param postMsg Represents the Post message to be searched.
     * @param sender  Represents the user that sent the Post message.
     * @param users   Represents the list of users to send the Post message to.
     */
    private void searchingForUsersInMessage(Post postMsg, User sender, List<User> users) {
        String[] contentWords = postMsg.getContent().split(" ");
        for (String contentWord : contentWords) {
            if (contentWord.contains("@")) {
                //need to search the tagged user
                String currentUserName = contentWord.substring(contentWord.indexOf("@") + 1);
                User currentUser = this.dataBase.getUserByName(currentUserName);
                if ((currentUser != null) && (!users.contains(currentUser))) {
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
     *
     * @param pmMsg Represents a PM message to be processed.
     */
    private void pmFunction(PM pmMsg) {
        this.logOrSendLock.readLock().lock(); // PM is considered as a read event.
        User sender = this.dataBase.getConnectedUser(this.connectionID);
        User recipient = this.dataBase.getUserByName(pmMsg.getUserName());
        if ((sender == null) || (recipient == null) || sender.getBlockedBy().contains(recipient)) {
            //the user is not logged in or recipient is not registered --> send error message
            this.connections.send(this.connectionID, new Error(pmMsg.getOpcode()));
            this.logOrSendLock.readLock().unlock();
            return;
        } else {
            Notification toSend = new Notification((byte) 0, sender.getUserName(), pmMsg.getFilteredContent());
            if (recipient.isConnected()) {
                this.dataBase.sendNotification(this.connections, recipient.getConnId(), toSend);
            } else {
                recipient.getWaitingMessages().add(toSend);
            }
            this.dataBase.addToHistory(toSend);
        }
        this.connections.send(this.connectionID, pmMsg.generateAckMessage());
        this.logOrSendLock.readLock().unlock();
    }

    /**
     * Is called when a user wants to get a list of all registered users. Gets it unless he isn't logged in.
     *
     * @param logstatMsg Represents a Logstat message to be processed.
     */
    private void logstatFunction(LogStat logstatMsg) {
        this.registerOrLogStatLock.readLock().lock(); // Logstat is considered as a read event.
        User currentUser = this.dataBase.getConnectedUser(this.connectionID);
        if (currentUser == null) {
            this.connections.send(this.connectionID, new Error(logstatMsg.getOpcode()));
        } else {
            List<User> registeredUsers = this.dataBase.returnRegisteredUsers(currentUser);
            short[] numOfPosts = new short[registeredUsers.size()];
            int i = 0;
            for (User user : registeredUsers) {
                numOfPosts[i] = this.dataBase.returnNumberOfPosts(user.getUserName());
                i++;
            }

            this.connections.send(this.connectionID, logstatMsg.generateAckMessage(registeredUsers, numOfPosts));
        }
        this.registerOrLogStatLock.readLock().unlock();
    }

    /**
     * Is called to receive data on a certain user (number of posts a user posted, number of followers,
     * number of users the user is following). Gets the data unless the user (that asks for the data) isn't logged in.
     *
     * @param statMsg Represents a Stat message to be processed.
     */
    private void statFunction(Stat statMsg) {
        User currentClient = this.dataBase.getConnectedUser(this.connectionID);
        if (currentClient != null) {

            String[] users = statMsg.getUsers();
            int size = users.length;
            short[] ages = new short[size];
            short[] numberOfPosts = new short[size];
            short[] followers = new short[size];
            short[] following = new short[size];

            int legalSize = 0;
            for (int i = 0; i < size; i++) {
                User user = this.dataBase.getUserByName(users[i]);
                if (user == null) {
                    this.connections.send(this.connectionID, new Error(statMsg.getOpcode()));
                    return;
                }
                if (!currentClient.isBlockedBy(user)) {
                    ages[legalSize] = user.getAge();
                    numberOfPosts[legalSize] = this.dataBase.returnNumberOfPosts(user.getUserName());
                    followers[legalSize] = user.getFollowersAmm();
                    following[legalSize] = user.getFollowingAmm();
                    legalSize++;
                }
            }
            this.connections.send(this.connectionID, statMsg.generateAckMessage(legalSize, ages, numberOfPosts, followers, following));

        } else {
            //if the requesting user is not logged in OR if the user in the request does not exist --> send error
            this.connections.send(this.connectionID, new Error(statMsg.getOpcode()));
        }

    }

    /**
     * Is called when a user blocks all connection from another user.
     * Sends error if user is not registered.
     *
     * @param blockMsg Represents a BLOCK message to be processed.
     */

    private void blockFunction(Block blockMsg) {
        User currentClient = this.dataBase.getConnectedUser(this.connectionID);
        User user = this.dataBase.getUserByName(blockMsg.getUsername());
        if ((user == null) || (currentClient == null)) {
            //if the requesting user is not logged in OR if the user in the request does not exist --> send error
            this.connections.send(this.connectionID, new Error(blockMsg.getOpcode()));
        } else {
            user.addBlockedBy(currentClient);
            // blocking and blocked users should unfollow each other
            if (currentClient.getFollowers().contains(user))
                currentClient.removeFollower(user);
            if (user.getFollowers().contains(currentClient))
                user.removeFollower(currentClient);

            user.addBlockedBy(currentClient);
            this.connections.send(this.connectionID, blockMsg.generateAckMessage());
        }
    }
}
