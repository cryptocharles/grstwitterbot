package com.devlabs;

import java.math.BigDecimal;
import java.sql.*;

public class DBWorker {
    private Connection connection;
    private Statement statement;

    public void ConnectToDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection("jdbc:h2:zeitbotdb", "zeitbot", "147258369");
        statement = connection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS PUBLIC.CUSTOMERS\n" +
                "(\n" +
                "    ID IDENTITY  PRIMARY KEY NOT NULL,\n" +
                "    USERNAME VARCHAR(255) NOT NULL,\n" +
                "    AMOUNT DECIMAL(38, 8) DEFAULT 0 NOT NULL\n" +
                ");\n" +
                "ALTER TABLE PUBLIC.CUSTOMERS ADD CONSTRAINT IF NOT EXISTS unique_USERNAME UNIQUE (USERNAME);");
    }

    public boolean CreateNewUser(String username, BigDecimal amount) throws SQLException {
        PreparedStatement preparedStatement =
                connection.prepareStatement("INSERT INTO PUBLIC.CUSTOMERS(USERNAME, AMOUNT) " +
                        "VALUES (?, ?)");
        preparedStatement.setString(1, username);
        preparedStatement.setBigDecimal(2, amount);
        preparedStatement.executeUpdate();
        preparedStatement.close();
        return true;
    }

    public boolean AddCoinsToUser(String username, BigDecimal coins) throws SQLException {
        if (!Exists(username))
            return false;
        String query = "UPDATE PUBLIC.CUSTOMERS SET AMOUNT = AMOUNT + ? WHERE USERNAME = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setBigDecimal(1, coins);
        preparedStatement.setString(2, username);
        int updateResult = preparedStatement.executeUpdate();
        preparedStatement.close();
        return (updateResult != 0);
    }

    public BigDecimal GetUserCoins(String username) throws SQLException {
        if (!Exists(username))
            return BigDecimal.ZERO;
        String query = "SELECT AMOUNT FROM PUBLIC.CUSTOMERS WHERE USERNAME = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, username);
        ResultSet result = preparedStatement.executeQuery();
        if (!result.next())
            return BigDecimal.ZERO;
        BigDecimal coins = result.getBigDecimal("AMOUNT");
        preparedStatement.close();
        return coins;
    }

    public boolean ReduceUserCoin(String username, BigDecimal amount) throws SQLException {
        if (!Exists(username))
            return true;
        if (GetUserCoins(username).compareTo(amount) < 0)
            return false;
        String query = "UPDATE PUBLIC.CUSTOMERS SET AMOUNT = AMOUNT - ? WHERE USERNAME = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setBigDecimal(1, amount);
        preparedStatement.setString(2, username);
        int updateResult = preparedStatement.executeUpdate();
        preparedStatement.close();
        return (updateResult != 0);
    }

    public void CloseConnection() throws SQLException {
        statement.close();
        connection.close();
    }

    public boolean Exists(String username) throws SQLException {
        PreparedStatement preparedStatement =
                connection.prepareStatement("SELECT USERNAME FROM PUBLIC.CUSTOMERS WHERE USERNAME=?");
        preparedStatement.setString(1, username);
        ResultSet result = preparedStatement.executeQuery();
        boolean exists = result.next();
        preparedStatement.close();
        return exists;
    }
}
