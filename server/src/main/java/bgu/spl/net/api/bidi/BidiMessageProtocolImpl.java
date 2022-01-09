package bgu.spl.net.api.bidi;
import bgu.spl.net.impl.Messages.Error;
import bgu.spl.net.impl.Messages.*;
import bgu.spl.net.impl.User;
import bgu.spl.net.impl.DataBase;

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

    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionID = connectionId;
        this.connections = connections;
    }

    @Override
    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }

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

    private void registerFunction(Register registerMsg) {
        this.registerOrLogStatLock.writeLock().lock();
        if (this.dataBase.getUserByName(registerMsg.getUsername()) != null) {
            this.connections.send(this.connectionID, new Error(registerMsg.getOpcode()));
        } else {
            this.dataBase.registerUser(registerMsg.getUsername(), registerMsg.getPassword(), registerMsg.getYear(), registerMsg.getMonth(), registerMsg.getDay());
            this.connections.send(this.connectionID, registerMsg.generateAckMessage());
        }
        this.registerOrLogStatLock.writeLock().unlock();
    }

    private void loginFunction(Login loginMsg) {
        this.logOrSendLock.writeLock().lock();
        User checkIfAlreadyConnected = this.dataBase.getConnectedUser(this.connectionID);
        if (checkIfAlreadyConnected != null || loginMsg.captcha == '0') {
            this.connections.send(this.connectionID, new Error(loginMsg.getOpcode()));
        } else {
            User toCheck = this.dataBase.getUserByName(loginMsg.getUsername());
            if ((toCheck == null) || (!toCheck.getPassword().equals(loginMsg.getPassword())) || (toCheck.isConnected())) {
                this.connections.send(this.connectionID, new Error(loginMsg.getOpcode()));
            } else {
                this.connections.send(connectionID, loginMsg.generateAckMessage());
                synchronized (toCheck.getWaitingMessages()) {
                    int size = toCheck.getWaitingMessages().size();
                    for (int i = 0; i < size; i++) {
                        Message current = toCheck.getWaitingMessages().poll(); //Sending awaiting notifications.
                        this.connections.send(this.connectionID, current);
                    }
                    toCheck.login(this.connectionID);
                    this.dataBase.loginUser(toCheck);
                }
            }
        }
        this.logOrSendLock.writeLock().unlock();
    }

    private void logoutFunction(Logout logoutMsg) {
        this.logOrSendLock.writeLock().lock();
        if (this.dataBase.loginIsEmpty()) {
            this.connections.send(this.connectionID, new Error(logoutMsg.getOpcode()));
        } else {
            this.dataBase.logoutUser(this.connectionID);
            this.connections.send(this.connectionID, logoutMsg.generateAckMessage());
            this.connections.disconnect(this.connectionID);
        }
        this.logOrSendLock.writeLock().unlock();
    }

    private void followFunction(Follow followMsg) {
        User toCheck = this.dataBase.getConnectedUser(this.connectionID);
        if (toCheck == null) {
            this.connections.send(this.connectionID, new Error(followMsg.getOpcode()));
        } else {
            Boolean successful = this.dataBase.followOrUnfollow(toCheck, followMsg.getUser(), followMsg.isFollowing());
            if (!successful) {
                this.connections.send(this.connectionID, new Error(followMsg.getOpcode()));
            } else {
                this.connections.send(this.connectionID, followMsg.generateAckMessage(followMsg.getUser()));
            }
        }
    }

    private void postFunction(Post postMsg) {
        this.logOrSendLock.readLock().lock();
        User sender = this.dataBase.getConnectedUser(this.connectionID);
        if (sender == null) {
            this.connections.send(this.connectionID, new Error(postMsg.getOpcode()));
        } else {
            List<User> users = new Vector<>();
            searchingForUsersInMessage(postMsg, sender, users);
            users.addAll(sender.getFollowers());
            Notification toSend = new Notification((byte) 1, sender.getUserName(), postMsg.getContent());
            for (User currentUser : users) {
                if (currentUser.isConnected()) {
                    this.dataBase.sendNotification(this.connections, currentUser.getConnId(), toSend);
                } else {
                    currentUser.getWaitingMessages().add(toSend);
                }
            }
            this.dataBase.addToHistory(toSend);
            this.connections.send(this.connectionID, postMsg.generateAckMessage());
        }
        this.logOrSendLock.readLock().unlock();
    }

    private void searchingForUsersInMessage(Post postMsg, User sender, List<User> users) {
        String[] contentWords = postMsg.getContent().split(" ");
        for (String contentWord : contentWords) {
            if (contentWord.contains("@")) {
                String currentUserName = contentWord.substring(contentWord.indexOf("@") + 1);
                User currentUser = this.dataBase.getUserByName(currentUserName);
                if ((currentUser != null) && (!users.contains(currentUser)) && !sender.getFollowers().contains(currentUser)) {
                    users.add(currentUser);
                }
            }
        }
    }

    private void pmFunction(PM pmMsg) {
        this.logOrSendLock.readLock().lock();
        User sender = this.dataBase.getConnectedUser(this.connectionID);
        User recipient = this.dataBase.getUserByName(pmMsg.getUserName());
        if ((sender == null) || (recipient == null) || sender.getBlockedBy().contains(recipient)) {
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

    private void logstatFunction(LogStat logstatMsg) {
        this.registerOrLogStatLock.readLock().lock();
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
            this.connections.send(this.connectionID, new Error(statMsg.getOpcode()));
        }

    }

    private void blockFunction(Block blockMsg) {
        User currentClient = this.dataBase.getConnectedUser(this.connectionID);
        User user = this.dataBase.getUserByName(blockMsg.getUsername());
        if ((user == null) || (currentClient == null)) {
            this.connections.send(this.connectionID, new Error(blockMsg.getOpcode()));
        } else {

            user.addBlockedBy(currentClient);
            if (currentClient.getFollowers().contains(user))
                currentClient.removeFollower(user);
            if (user.getFollowers().contains(currentClient))
                user.removeFollower(currentClient);

            user.addBlockedBy(currentClient);
            this.connections.send(this.connectionID, blockMsg.generateAckMessage());
        }
    }
}
