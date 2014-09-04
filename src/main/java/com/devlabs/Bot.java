package com.devlabs;


import twitter4j.TwitterException;

import java.io.IOException;
import java.sql.SQLException;

public class Bot {
    public static void main(String[] args) {
        try {
            TwitterWorker twitterWorker = new TwitterWorker();
            System.out.println("autorize");
            twitterWorker.Authorize();
            System.out.println("autorize done");
            Thread monitorThread = new Thread(twitterWorker);
            monitorThread.start();
            monitorThread.join();
        } catch (SQLException | ClassNotFoundException | TwitterException | IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
