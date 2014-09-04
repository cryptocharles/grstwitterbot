package com.devlabs;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;

public class Wallet {
    private URL server = null;
    private HttpURLConnection connection = null;

    public Wallet() {
        try {
            server = new URL("http://127.0.0.1:1441");
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    // First - rpcuser. Second - rpcpassword
                    return new PasswordAuthentication("saffroncoinrpc", "HJKd12SQDjk89qsdsq671289hjhjsSS987".toCharArray());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigDecimal GetBalance() {
        try {
            SendRequest("getbalance", null);
            BigDecimal balance = new BigDecimal((double)GetParam("result"));
            balance = balance.setScale(8, RoundingMode.HALF_DOWN);
            return balance;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        } finally {
            connection.disconnect();
        }
    }

    public boolean NewTransaction(String wallet, BigDecimal amount) {
        JSONArray params = new JSONArray();
        params.add(wallet);
        params.add(amount.doubleValue());
        try {
            SendRequest("sendtoaddress", params);
            return (GetParam("result") == null);
        } catch (IOException | ParseException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            connection.disconnect();
        }
    }

    private void SendRequest(String method, JSONArray params) throws IOException {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("id", "0");
        request.put("method", method);
        if (method.equals("sendtoaddress")) {
            request.put("params", params);
        }
        String requestString = request.toString();
        connection = (HttpURLConnection) server.openConnection(Proxy.NO_PROXY);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Length",
                Integer.toString(requestString.getBytes().length));
        connection.setUseCaches(true);
        connection.setDoInput(true);
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestString.getBytes());
        System.out.println(request.toString());
    }

    private Object GetParam(String param) throws IOException, ParseException {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        JSONParser parser = new JSONParser();
        JSONObject response = (JSONObject) parser.parse(reader);
        if (response.get("errors") != null)
            return null;
        return response.get(param);
    }
}
