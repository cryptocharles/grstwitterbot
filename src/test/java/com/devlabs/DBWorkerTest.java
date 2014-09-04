package com.devlabs;

import junit.framework.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DBWorkerTest {
//
//    @Test
//    public void testCreateNewUser() throws Exception {
//        DBWorker db = new DBWorker();
//        db.ConnectToDB();
//
//        Assert.assertEquals(db.CreateNewUser("devnikor", 200), !db.Exists("devnikor"));
//        db.CloseConnection();
//    }
//
////    @Test
////    public void testExist() throws Exception {
////        DBWorker db = new DBWorker();
////        db.ConnectToDB();
////        Assert.assertEquals(db.Exists("devnikor"), false);
//////        db.CloseConnection();
//////    }
//
//    @Test
//    public void testAddCoinsToUser() throws Exception {
//        System.out.println("_______________________________");
//        DBWorker db = new DBWorker();
//        db.ConnectToDB();
//        if (!db.Exists("devnikor"))
//            db.CreateNewUser("devnikor", new BigDecimal(144.86).setScale(8, RoundingMode.HALF_DOWN));
//        BigDecimal currentAmount = db.GetUserCoins("devnikor");
//        System.out.println("Current amount: " + currentAmount);
//        BigDecimal happy = new BigDecimal(153.295643).setScale(8, RoundingMode.HALF_DOWN);
//        System.out.println("Add coins: " + happy);
//        Assert.assertEquals(db.AddCoinsToUser("devnikor", happy), true);
//        BigDecimal newAmount = db.GetUserCoins("devnikor");
//        Assert.assertEquals(newAmount, currentAmount.add(happy));
//        System.out.println("New amount: " + newAmount);
//        db.CloseConnection();
//        System.out.println("_______________________________");
//    }
//
//    @Test
//    public void testReduceUserCoin() throws Exception {
//        System.out.println("_______________________________");
//        DBWorker db = new DBWorker();
//        db.ConnectToDB();
//        if (!db.Exists("devnikor"))
//            db.CreateNewUser("devnikor", new BigDecimal(144.86).setScale(8, RoundingMode.HALF_DOWN));
//        BigDecimal currentAmount = db.GetUserCoins("devnikor");
//        System.out.println("Current amount: " + currentAmount);
//        BigDecimal reduceAmount = new BigDecimal(26.74).setScale(8, RoundingMode.HALF_DOWN);
//        System.out.println("Reduce amount: " + reduceAmount);
//        Assert.assertEquals(db.ReduceUserCoin("devnikor", reduceAmount), true);
//        BigDecimal newAmount = db.GetUserCoins("devnikor");
//        Assert.assertEquals(newAmount, currentAmount.subtract(reduceAmount));
//        System.out.println("New amount: " + newAmount);
//        db.CloseConnection();
//        System.out.println("_______________________________");
//    }
}
