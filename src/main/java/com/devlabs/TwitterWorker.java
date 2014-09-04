package com.devlabs;

import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterWorker implements Runnable {
    private static final String API_KEY = "FAmL4zQ6comRjfaUf4f5ZXJDl";
    private static final String API_SECRET = "DuIRot4G0AGJAvVIeQwQrM9qjyFT62zzWLGbi9t50W1AFi1AST";
    private static final String ACCESS_TOKEN = "2788385629-6nspNqRRETknMlxTCjhG0qO1fdlsGpzjPSAtJGg";
    private static final String ACCESS_TOKEN_SECRET = "UoMTnyOydylWkYfsA7kwdZzU9xxhKLBXZs6X2qMV4840Z";
    private static final String ACCOUNT_NAME = "groestltip";

    private static final double FEE = 1.0;

    private String accessTokenKey;
    private String accessTokenSecret;

    private ResponseList<DirectMessage> directMessages;
    private LinkedList<Pair<String, String>> pendingDirectMessages;
    private Paging directMessagesPaging;
    private Paging mentionsPaging;
    private long lastDirectMessageID;
    private long lastMentionID;

    private Pattern mentionPattern;

    private Twitter twitter;
    private DBWorker db;
    private Preferences preferences;
    private Wallet wallet;

    public TwitterWorker() throws SQLException, ClassNotFoundException {
        twitter = TwitterFactory.getSingleton();
        preferences = Preferences.userRoot().node("zeitbot");
        directMessagesPaging = new Paging();
        mentionsPaging = new Paging();
        pendingDirectMessages = new LinkedList<>();
        mentionPattern = Pattern.compile("(?i)@" + ACCOUNT_NAME + "\\s+\\d+(\\.\\d+)?\\s+to\\s+@\\w+");
        directMessages = null;
        db = new DBWorker();
        db.ConnectToDB();
        wallet = new Wallet();
        LoadPreferences();
    }

    public void LoadPreferences() {
        accessTokenKey = preferences.get("AccessTokenKey", ACCESS_TOKEN);
        accessTokenSecret = preferences.get("AccessTokenSecret", ACCESS_TOKEN_SECRET);
        lastDirectMessageID = preferences.getLong("LastDirectMessageID", 1);
        lastMentionID = preferences.getLong("LastMentionsID", 1);
    }

    public void Authorize() throws TwitterException, IOException {
        twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(API_KEY, API_SECRET);
        twitter.setOAuthAccessToken(new AccessToken(accessTokenKey, accessTokenSecret));
    }

    @Override
    public void run() {
        do {
            try {
                ProcessPendingMessages();
                UpdateFriendship();
                ProcessDirectMessages();
                ProcessMentions();
                Thread.sleep(60000);
            } catch (TwitterException e) {
                System.out.println(e.getErrorMessage());
                if (e.exceededRateLimitation()) {
                    try {
                        System.out.println(e.getMessage());
                        long millis = e.getRateLimitStatus().getSecondsUntilReset() * 1000;
                        Thread.sleep(millis);
                    } catch (InterruptedException ex) {
                        HandleInterruptedException(ex);
                    }
                }
            } catch (InterruptedException e) {
                HandleInterruptedException(e);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        while (!Thread.currentThread().isInterrupted());
    }

    private void ProcessDirectMessages() throws TwitterException, InterruptedException, SQLException {
        directMessagesPaging.setSinceId(lastDirectMessageID);
//      directMessagesPaging.setMaxId(lastDirectMessageID + 100);
//      directMessagesPaging.setCount(100);
        directMessages = twitter.getDirectMessages(directMessagesPaging);
        DirectMessage message;
        for (int i = directMessages.size() - 1; i >= 0; i--) {
            message = directMessages.get(i);
            ParseMessage(message.getText(), message.getSenderScreenName());
            lastDirectMessageID = message.getId();
            preferences.putLong("LastDirectMessageID", lastDirectMessageID);
            Thread.sleep(1000);
        }
        directMessages = null;
    }

    private void ProcessPendingMessages() throws TwitterException, InterruptedException {
        Pair<String, String> message;
        while (!pendingDirectMessages.isEmpty()) {
            message = pendingDirectMessages.pollFirst();
            SendMessage(message.getLeft(), message.getRight());
            Thread.sleep(1000);
        }
    }

    private void HandleInterruptedException(InterruptedException e) {
        System.out.println(e.getMessage());
        twitter.shutdown();
        try {
            db.CloseConnection();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    private void UpdateFriendship() throws TwitterException, InterruptedException {
        ArrayList<Long> friendsID = new ArrayList<>();
        long followersCursor = -1;
        long friendsCursor = -1;
        IDs followers;
        IDs friends;
        do {
            // Twitter.getIncomingFriendships not working. Why?
            friends = twitter.getFriendsIDs(friendsCursor);
            for (long id : friends.getIDs()) {
                friendsID.add(id);
            }
        } while ((friendsCursor = friends.getNextCursor()) != 0);

        do {
            followers = twitter.getFollowersIDs(followersCursor);
            for (long id : followers.getIDs()) {
                if (!friendsID.contains(id)) {
                    twitter.createFriendship(id);
                }
            }
        } while ((followersCursor = followers.getNextCursor()) != 0);
    }

    private void ProcessMentions() throws TwitterException, InterruptedException {
        mentionsPaging.setSinceId(lastMentionID);
//        mentionsPaging.setMaxId(lastMentionID + 100);
//        mentionsPaging.setCount(100);
        ResponseList<Status> mentions = twitter.getMentionsTimeline(mentionsPaging);
        for (int i = mentions.size() - 1; i >= 0; i--) {
            Matcher mentionMatcher = mentionPattern.matcher(mentions.get(i).getText());
            if (mentionMatcher.find()) {
                String[] command = mentionMatcher.group().split("\\s+");
                BigDecimal amount = new BigDecimal(command[1]);
                amount = amount.setScale(8, RoundingMode.HALF_DOWN);
                String toUser = command[3].substring(1);
                String fromUser = mentions.get(i).getUser().getScreenName();
                TransferCoins(fromUser, toUser, amount);
            }
            lastMentionID = mentions.get(i).getId();
            preferences.putLong("LastMentionsID", lastMentionID);
            Thread.sleep(1000);
        }
    }

    public boolean TransferCoins(String fromUser, String toUser, BigDecimal amount) {
        try {
            if (!db.Exists(fromUser)) {
                NewPendingMessage(fromUser, "Sorry. You not registered here");
                return false;
            }
            if (!db.Exists(toUser)) {
                NewPendingMessage(fromUser,
                        "Sorry. @" + toUser + " not registered here");
                return false;
            }
            if (!db.ReduceUserCoin(fromUser, amount))
                return false;
            if (!db.AddCoinsToUser(toUser, amount)) {
                db.AddCoinsToUser(fromUser, amount);
                return false;
            }
            System.out.println("transfer " + amount + " to @" + toUser + " from @" + fromUser);
            NewPendingMessage(toUser, "You are received " + amount.toString() + " from " + fromUser);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void ParseMessage(String message, String user) throws SQLException {
        String[] messageParts = message.split("\\s+");
        switch (messageParts[0]) {
            case "!info":
                InfoMessage(user);
                break;
            case "!withdraw":
                if (messageParts.length < 3 || !messageParts[2].matches("(?i)(\\d+(\\.\\d+)?)|all"))
                    NewPendingMessage(user, "Wrong syntax");
                else {
                    BigDecimal amount;
                    if (messageParts[2].matches("(?i)all")) {
                        amount = db.GetUserCoins(user);
                        amount = amount.subtract(new BigDecimal(FEE).setScale(8,
                                RoundingMode.HALF_DOWN));
                    }
                    else {
                        amount = new BigDecimal(messageParts[2]);
                    }
                    amount = amount.setScale(8, RoundingMode.HALF_DOWN);
                    WithdrawMessage(messageParts[1], amount, user);
                }
                break;
            case "!help":
                NewPendingMessage(user, "See http://goo.gl/HIFze5 for help");
                break;
            case "!history":
                HistoryMessage(user);
                break;
            case "!register":
                RegisterMessage(user);
                break;
            default:
                NewPendingMessage(user,
                        "Sorry, but i don't understand your message. Please see !help");
                break;
        }
    }

    private boolean InfoMessage(String user) {
        System.out.printf("!info request from @%s\n", user);
        try {
            BigDecimal amount = db.GetUserCoins(user);
            if (amount.equals(BigDecimal.ZERO.setScale(8,RoundingMode.HALF_DOWN)))
                amount = BigDecimal.ZERO;
            NewPendingMessage(user, "You have " + amount + " coins");
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            NewPendingMessage(user,
                    "Sorry. I can't do this. Are you registered?");
            return false;
        }
    }

    private void WithdrawMessage(String wallet, BigDecimal amount, String user) {
        System.out.printf("!withdraw request from @%s to %s wallet for %s coins\n",
                user, wallet, amount);
        amount = amount.add(new BigDecimal(FEE).setScale(8, RoundingMode.HALF_DOWN));
        try {
            if (db.GetUserCoins(user).compareTo(amount) < 0) {
                NewPendingMessage(user, "You don't have enough of coins.");
                return;
            }
            db.ReduceUserCoin(user, amount);
            if (!this.wallet.NewTransaction(wallet, amount)) {
                NewPendingMessage(user, "Unknown error. Please send a bug report");
                db.AddCoinsToUser(user, amount.subtract(new BigDecimal(FEE)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void HistoryMessage(String user) {
        System.out.printf("!history request from @%s\n", user);
    }

    private boolean RegisterMessage(String user) {
        System.out.printf("!register request from @%s\n", user);
        try {
            db.CreateNewUser(user, BigDecimal.ZERO.setScale(8, RoundingMode.HALF_DOWN));
            NewPendingMessage(user, "You are registered");
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            NewPendingMessage(user, "Can't register. Maybe you already here?");
            try {
                return (db.Exists(user));
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    private void SendMessage(String user, String message) throws TwitterException {
        try {
            twitter.sendDirectMessage(user, message);
        } catch (TwitterException e) {
            System.out.println(e.getErrorMessage());
            if (e.getErrorCode() != 150)
                pendingDirectMessages.add(new Pair<>(user, message));
            throw e;
        }
    }

    private void NewPendingMessage(String user, String message) {
        pendingDirectMessages.addLast(new Pair<>(user, message));
    }
}
