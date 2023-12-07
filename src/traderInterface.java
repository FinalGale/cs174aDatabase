import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

public class traderInterface {
    final static String DB_URL = "jdbc:oracle:thin:@projDB_tp?TNS_ADMIN=/Users/daniellu/Downloads/Wallet_projDB";
    final static String DB_USER = "ADMIN";
    final static String DB_PASSWORD = "Cookie12345+";

    public static void deposit(Connection connection, double depositAmount, int marketAccountID) throws SQLException {
        System.out.println("Preparing to Deposit...");
        int orderNumber = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            // get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            PreparedStatement insertMT = connection
                    .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber + 1);
            insertMT.setString(4, "deposit");
            insertMT.setDouble(5, currentBalance + depositAmount);
            insertMT.executeQuery();
            insertMT.close();

            System.out.println("Deposit Successful.");
        } catch (Exception e) {
            System.out.println("ERROR: Deposit failed.");
            System.out.println(e);
        }
    }

    public static void withdraw(Connection connection, double withdrawAmount, int marketAccountID) throws SQLException {
        System.out.println("Preparing to Withdraw...");
        int orderNumber = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            // get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            if (currentBalance < withdrawAmount) {
                System.out.println("ERROR: Withdraw failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement insertMT = connection
                    .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber + 1);
            insertMT.setString(4, "withdraw");
            insertMT.setDouble(5, currentBalance - withdrawAmount);
            insertMT.executeQuery();
            insertMT.close();

            System.out.println("Withdraw Successful.");
        } catch (Exception e) {
            System.out.println("ERROR: Withdraw failed.");
            System.out.println(e);
        }
    }

    public static void buy(Connection connection, double quantity, String stockSymbol, String username,
            int marketAccountID) throws SQLException {
        System.out.println("Preparing to buy...");
        int orderNumber = 0;
        int stockAccountID = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        double currentPrice = 0;
        double currentShares = 0;

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            // get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            // get the current stock price
            queryString = "SELECT currentPrice FROM StarProfile WHERE stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            if (resultSet.next()) {
                currentPrice = resultSet.getDouble(1);
            } else {
                System.out.println("Buy failed. This stock doesn't exist.");
                return;
            }
            getPrice.close();

            double totalCost = quantity * currentPrice + 20;
            if (currentBalance < totalCost) {
                System.out.println("ERROR: Buy failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement insertMT = connection
                    .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber + 1);
            insertMT.setString(4, "buy");
            insertMT.setDouble(5, currentBalance - totalCost);
            insertMT.executeQuery();
            insertMT.close();

            // check if we need to create a new stock account
            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE username = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            getStockAccountID.setString(1, username);
            resultSet = getStockAccountID.executeQuery();
            boolean foundStock = false;
            while (resultSet.next()) {
                int curSID = resultSet.getInt(1);
                queryString = "SELECT stockSymbol FROM StockAccount WHERE stockAccountID = ?";
                PreparedStatement getStockSymbol = connection.prepareStatement(queryString);
                getStockSymbol.setInt(1, curSID);
                ResultSet resultSet2 = getStockSymbol.executeQuery();
                String curStockSymbol = "";
                if (resultSet2.next()) {
                    curStockSymbol = resultSet2.getString(1);
                }
                if (curStockSymbol.equals(stockSymbol)) {
                    foundStock = true;
                    stockAccountID = curSID;
                    break;
                }
                getStockSymbol.close();
            }
            getStockAccountID.close();

            if (!foundStock) {
                // generate new stockAccountID by getting the last ID
                queryString = "SELECT stockAccountID FROM StockAccount ORDER BY stockAccountID DESC";
                Statement getLast = connection.createStatement();
                resultSet = getLast.executeQuery(queryString);
                if (resultSet.next()) {
                    stockAccountID = resultSet.getInt(1);
                }
                stockAccountID++;
                getLast.close();
            }

            // get the current number of shares
            queryString = "SELECT quantity FROM StockAccount WHERE StockAccountID = ?";
            PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
            getCurrentShares.setInt(1, stockAccountID);
            resultSet = getCurrentShares.executeQuery();
            if (resultSet.next()) {
                currentShares = resultSet.getInt(1);
            }
            getCurrentShares.close();

            if (currentShares == 0) {
                PreparedStatement insertST = connection
                        .prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                insertST.setInt(1, stockAccountID);
                insertST.setString(2, stockSymbol);
                insertST.setDouble(3, currentPrice);
                insertST.setDouble(4, quantity);
                insertST.executeQuery();
                insertST.close();

                PreparedStatement insertOA = connection.prepareStatement("INSERT INTO OwnsAccount VALUES (?, ?, ?)");
                insertOA.setString(1, username);
                insertOA.setInt(2, marketAccountID);
                insertOA.setInt(3, stockAccountID);
                insertOA.executeQuery();
                insertOA.close();
            } else {
                PreparedStatement updateST = connection.prepareStatement(
                        "UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ?");
                updateST.setDouble(1, quantity + currentShares);
                updateST.setInt(2, stockAccountID);
                updateST.executeQuery();
                updateST.close();
            }

            PreparedStatement insertST = connection
                    .prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            insertST.setInt(1, stockAccountID);
            insertST.setString(2, stockSymbol);
            insertST.setDouble(3, currentPrice);
            insertST.setDate(4, currentDate);
            insertST.setInt(5, orderNumber + 1);
            insertST.setString(6, "buy");
            insertST.setDouble(7, currentPrice);
            insertST.setDouble(8, quantity);
            insertST.executeQuery();
            insertST.close();

            System.out.println("Buy Transaction Successful.");
        } catch (Exception e) {
            System.out.println("ERROR: Buy failed.");
            System.out.println(e);
        }
    }

    public static void sell(Connection connection, ArrayList<Double> quantitiesSold, String stockSymbol,
            ArrayList<Double> buyPrices, String username, int marketAccountID) throws SQLException {
        System.out.println("Preparing to sell...");
        java.sql.Date currentDate = new java.sql.Date(0);
        int n = quantitiesSold.size();
        double currentBalance = 0;
        double currentPrice = 0;
        int orderNumber = 0;
        double[] currentShares = new double[n];
        int sid[] = new int[n];

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            // get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            // get the current stock price
            queryString = "SELECT currentPrice FROM StarProfile WHERE stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            if (resultSet.next()) {
                currentPrice = resultSet.getDouble(1);
            } else {
                System.out.println("Buy failed. This stock doesn't exist.");
                return;
            }
            getPrice.close();

            // get the number of shares the customer currently owns
            for (int i = 0; i < n; i++) {
                queryString = "SELECT stockAccountID, quantity FROM StockAccount WHERE stockSymbol = ? AND buyPrice = ? AND StockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
                PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                getCurrentShares.setString(1, stockSymbol);
                getCurrentShares.setDouble(2, buyPrices.get(i));
                getCurrentShares.setString(3, username);
                resultSet = getCurrentShares.executeQuery();
                if (resultSet.next()) {
                    sid[i] = resultSet.getInt(1);
                    currentShares[i] = resultSet.getDouble(2);
                }
                getCurrentShares.close();
            }

            for (int i = 0; i < n; i++) {
                if (currentShares[i] < quantitiesSold.get(i)) {
                    System.out.println("ERROR: Sell failed. You cannot sell more shares than you own.");
                    return;
                }
            }
            // ensure that the balance won't go negative after selling
            double revenue = -20;
            for (int i = 0; i < n; i++) {
                revenue += quantitiesSold.get(i) * currentPrice;
            }
            if (currentBalance + revenue < 0) {
                System.out.println("ERROR: Sell failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement insertMT = connection
                    .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber + 1);
            insertMT.setString(4, "sell");
            insertMT.setDouble(5, currentBalance + revenue);
            insertMT.executeQuery();
            insertMT.close();

            for (int i = 0; i < n; i++) {
                PreparedStatement insertST = connection
                        .prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                insertST.setInt(1, sid[i]);
                insertST.setString(2, stockSymbol);
                insertST.setDouble(3, buyPrices.get(i));
                insertST.setDate(4, currentDate);
                insertST.setInt(5, orderNumber + 1);
                insertST.setString(6, "sell");
                insertST.setDouble(7, currentPrice);
                insertST.setDouble(8, quantitiesSold.get(i));
                insertST.executeQuery();
                insertST.close();
            }

            for (int i = 0; i < n; i++) {
                if (currentShares[i] == quantitiesSold.get(i)) {
                    // we are selling all of this stock, so we delete the row
                    PreparedStatement deleteOA = connection.prepareStatement(
                            "DELETE FROM OwnsAccount WHERE stockAccountID = ?");
                    deleteOA.setInt(1, sid[i]);
                    deleteOA.executeQuery();
                    deleteOA.close();

                    PreparedStatement deleteSA = connection.prepareStatement(
                            "DELETE FROM StockAccount WHERE stockAccountID = ?");
                    deleteSA.setInt(1, sid[i]);
                    deleteSA.executeQuery();
                    deleteSA.close();
                } else {
                    // we are selling part of this stock, so we subtract from the quantity
                    PreparedStatement updateSA = connection.prepareStatement(
                            "UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ?");
                    updateSA.setDouble(1, currentShares[i] - quantitiesSold.get(i));
                    updateSA.setInt(2, sid[i]);
                    updateSA.executeQuery();
                    updateSA.close();
                }
            }

            System.out.println("Sell Transaction Successful.");
        } catch (Exception e) {
            System.out.println("ERROR: Sell failed.");
            System.out.println(e);
        }
    }

    public static void cancel(Connection connection, String username, int marketAccountID) throws SQLException {
        System.out.println("Preparing to cancel...");
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        int n = 0;
        int cancelOrderNumber = 0;

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            // get the trader's current balance
            queryString = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
            }
            getBalance.close();

            // get the most recent buy/sell MarketTransaction
            queryString = "SELECT * FROM MarketTransaction WHERE marketAccountID = ? AND transactDate = ? AND (transactionType = 'buy' OR transactionType = 'sell') ORDER BY orderNumber DESC";
            PreparedStatement getMT = connection.prepareStatement(queryString);
            getMT.setInt(1, marketAccountID);
            getMT.setDate(2, currentDate);
            resultSet = getMT.executeQuery();
            if (!resultSet.next()) {
                System.out.println("ERROR: Cancel failed. There are no transactions to cancel.");
                return;
            }
            cancelOrderNumber = resultSet.getInt(3);
            String cancelTransactionType = resultSet.getString(4);
            getMT.close();

            // get number of different buy prices
            queryString = "SELECT COUNT(*) FROM StockTransaction WHERE orderNumber = ? AND stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getCount = connection.prepareStatement(queryString);
            getCount.setInt(1, cancelOrderNumber);
            getCount.setString(2, username);
            resultSet = getCount.executeQuery();
            if (resultSet.next()) {
                n = resultSet.getInt(1);
            }

            // get the most recent buy/sell StockTransaction
            queryString = "SELECT * FROM StockTransaction WHERE orderNumber = ? AND stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getST = connection.prepareStatement(queryString);
            getST.setInt(1, cancelOrderNumber);
            getST.setString(2, username);
            resultSet = getST.executeQuery();
            int j = 0;
            int sid = 0;
            String stockSymbol = "";
            double[] buyPrices = new double[n];
            double[] sellPrices = new double[n];
            double[] quantities = new double[n];
            while (resultSet.next()) {
                sid = resultSet.getInt(1);
                stockSymbol = resultSet.getString(2);
                buyPrices[j] = resultSet.getDouble(3);
                sellPrices[j] = resultSet.getDouble(7);
                quantities[j] = resultSet.getDouble(8);
                j++;
            }
            getST.close();

            // delete the transactions
            queryString = "DELETE FROM MarketTransaction WHERE marketAccountID = ? AND orderNumber = ?";
            PreparedStatement deleteMT = connection.prepareStatement(queryString);
            deleteMT.setInt(1, marketAccountID);
            deleteMT.setInt(2, cancelOrderNumber);
            resultSet = deleteMT.executeQuery();

            queryString = "DELETE FROM StockTransaction WHERE stockAccountID = ? AND orderNumber = ?";
            PreparedStatement deleteST = connection.prepareStatement(queryString);
            deleteST.setInt(1, sid);
            deleteST.setInt(2, cancelOrderNumber);
            resultSet = deleteST.executeQuery();

            if (cancelTransactionType.equals("buy")) {
                // cancel the last buy transaction
                double payback = buyPrices[0] * quantities[0] - 20;
                if (currentBalance + payback < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                // delete the appropriate stocks from the account
                for (int i = 0; i < n; i++) {
                    queryString = "SELECT quantity FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
                    PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                    getCurrentShares.setInt(1, sid);
                    getCurrentShares.setString(2, stockSymbol);
                    getCurrentShares.setDouble(3, buyPrices[i]);
                    resultSet = getCurrentShares.executeQuery();
                    double currentShares = 0;
                    if (resultSet.next()) {
                        currentShares = resultSet.getDouble(1);
                    }
                    getCurrentShares.close();

                    if (currentShares == quantities[i]) {
                        PreparedStatement deleteSA = connection.prepareStatement(
                                "DELETE FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        deleteSA.setInt(1, sid);
                        deleteSA.setString(2, stockSymbol);
                        deleteSA.setDouble(3, buyPrices[i]);
                        deleteSA.executeQuery();
                        deleteSA.close();
                    } else {
                        PreparedStatement updateSA = connection.prepareStatement(
                                "UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        updateSA.setDouble(1, currentShares - quantities[i]);
                        updateSA.setInt(2, sid);
                        updateSA.setString(3, stockSymbol);
                        updateSA.setDouble(4, buyPrices[i]);
                        updateSA.executeQuery();
                        updateSA.close();
                    }
                }

                // add the cancel transaction to the table
                PreparedStatement updateMT = connection
                        .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
                updateMT.setInt(1, marketAccountID);
                updateMT.setDate(2, currentDate);
                updateMT.setInt(3, cancelOrderNumber + 1);
                updateMT.setString(4, "cancelBuy");
                updateMT.setDouble(5, currentBalance + payback);
                updateMT.executeQuery();
                updateMT.close();

            } else if (cancelTransactionType.equals("sell")) {
                // cancel the last sell transaction
                double loss = -20;
                for (int i = 0; i < n; i++) {
                    loss -= (sellPrices[i] - buyPrices[i]);
                }

                if (currentBalance + loss < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                // add back the appropriate stocks to stockAccount
                for (int i = 0; i < n; i++) {
                    queryString = "SELECT quantity FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
                    PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                    getCurrentShares.setInt(1, sid);
                    getCurrentShares.setString(2, stockSymbol);
                    getCurrentShares.setDouble(3, buyPrices[i]);
                    resultSet = getCurrentShares.executeQuery();
                    double currentShares = 0;
                    if (resultSet.next()) {
                        currentShares = resultSet.getDouble(1);
                    }
                    getCurrentShares.close();

                    if (currentShares == 0) {
                        // we need to put back the stocks as insertions since the trader doesn't have
                        // any
                        PreparedStatement insertSA = connection
                                .prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                        insertSA.setInt(1, sid);
                        insertSA.setString(2, stockSymbol);
                        insertSA.setDouble(3, buyPrices[i]);
                        insertSA.setDouble(4, quantities[i]);
                        insertSA.executeQuery();
                        insertSA.close();
                    } else {
                        // the trader has some of the stocks already so we add on to the existing row
                        PreparedStatement updateSA = connection.prepareStatement(
                                "UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        updateSA.setDouble(1, currentShares + quantities[i]);
                        updateSA.setInt(2, sid);
                        updateSA.setString(3, stockSymbol);
                        updateSA.setDouble(4, buyPrices[i]);
                        updateSA.executeQuery();
                        updateSA.close();
                    }
                }
                // add the cancel transaction to the table
                PreparedStatement insertMT = connection
                        .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
                insertMT.setInt(1, marketAccountID);
                insertMT.setDate(2, currentDate);
                insertMT.setInt(3, cancelOrderNumber + 1);
                insertMT.setString(4, "cancelSell");
                insertMT.setDouble(5, currentBalance + loss);
                insertMT.executeQuery();
                insertMT.close();
            }
            System.out.println("Cancel Successful.");
        } catch (Exception e) {
            System.out.println("ERROR: Cancel failed.");
            System.out.println(e);
        }
    }

    public static void showBalance(Connection connection, int marketAccountID) throws SQLException {
        try {
            // get the trader's current balance
            double currentBalance = 0;
            String queryString = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            ResultSet resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
            }
            getBalance.close();
            System.out.println("Your current account balance is: " + String.format("%.2f", currentBalance));
        } catch (Exception e) {
            System.out.println("ERROR: showBalance failed.");
            System.out.println(e);
        }
    }

    public static void showTransactionHistory(Connection connection, String username, String stockSymbol)
            throws SQLException {
        try {
            // get all StockTransactions
            String queryString = "SELECT * FROM StockTransaction WHERE stocksymbol = ? AND stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getStockTransaction = connection.prepareStatement(queryString);
            getStockTransaction.setString(1, stockSymbol);
            getStockTransaction.setString(2, username);
            ResultSet resultSet = getStockTransaction.executeQuery();
            getStockTransaction.close();

            System.out.println("Your transactions this month for " + stockSymbol + ": ");
            while (resultSet.next()) {
                double buyPrice = resultSet.getDouble(3);
                java.sql.Date date = resultSet.getDate(4);
                String transactionType = resultSet.getString(6);
                double sellPrice = resultSet.getDouble(7);
                double quantity = resultSet.getDouble(8);
                System.out.println("Date: " + date + "; Transaction Type: " + transactionType +
                        "; Buy Price: " + buyPrice + (transactionType == "sell" ? ("; Sell Price: " + sellPrice) : "")
                        + "; Quantity: " + quantity);
            }
        } catch (Exception e) {
            System.out.println("ERROR: showTransactionHistory failed.");
            System.out.println(e);
        }
    }

    public static void showStocks(Connection connection, String username) throws SQLException {
        try {
            // get all stock accounts
            String queryString = "SELECT * FROM StockAccount WHERE stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getSA = connection.prepareStatement(queryString);
            getSA.setString(1, username);
            ResultSet resultSet = getSA.executeQuery();
            getSA.close();

            System.out.println("Stocks you currently own: ");
            while (resultSet.next()) {
                String stockSymbol = resultSet.getString(2);
                double buyPrice = resultSet.getDouble(3);
                double quantity = resultSet.getDouble(4);
                System.out.println(
                        "Stock Symbol: " + stockSymbol + "; Buy Price: " + buyPrice + "; Quantity: " + quantity);
            }
        } catch (Exception e) {
            System.out.println("ERROR: showStocks failed.");
            System.out.println(e);
        }
    }

    public static void getCurStockPriceAndStarProfile(Connection connection, String stockSymbol) throws SQLException {
        try {
            String queryString = "SELECT * FROM StarProfile WHERE stockSymbol = ?";
            PreparedStatement getStockInfo = connection.prepareStatement(queryString);
            getStockInfo.setString(1, stockSymbol);
            ResultSet resultSet = getStockInfo.executeQuery();
            String name = "";
            double currentPrice = 0;
            java.sql.Date dateOfBirth = new java.sql.Date(0);
            if (resultSet.next()) {
                name = resultSet.getString(2);
                currentPrice = resultSet.getDouble(3);
                dateOfBirth = resultSet.getDate(4);
            }
            getStockInfo.close();

            System.out.println("Current Price for " + stockSymbol + ": " + currentPrice + " USD\n" +
                    "Actor Profile for " + name + ":\n" +
                    "Date of Birth: " + dateOfBirth);
            System.out.println("----------------------------------------");

            queryString = "SELECT * FROM SignedContract WHERE stockSymbol = ?";
            PreparedStatement getContractInfo = connection.prepareStatement(queryString);
            getContractInfo.setString(1, stockSymbol);
            resultSet = getStockInfo.executeQuery();

            System.out.println("Movie Contracts signed by " + name + ": ");
            while (resultSet.next()) {
                String title = resultSet.getString(2);
                int productionYear = resultSet.getInt(3);
                String role = resultSet.getString(4);
                double totalValue = resultSet.getDouble(5);
                System.out.println("Movie title: " + title + "; Production Year: " + productionYear + "; Role: " + role
                        + "; Total Payment Value: " + totalValue);
            }
            getContractInfo.close();
        } catch (Exception e) {
            System.out.println("ERROR: getCurStockPriceAndStarProfile failed.");
            System.out.println(e);
        }
    }

    public static void getTopMovies(Connection connection, int startDate, int stopDate) throws SQLException {
        try {
            String queryString = "SELECT title, productionYear FROM Movie WHERE productionYear >= ? AND productionYear <= ? AND rating = 10";
            PreparedStatement getTopMovies = connection.prepareStatement(queryString);
            getTopMovies.setInt(1, startDate);
            getTopMovies.setInt(2, stopDate);
            ResultSet resultSet = getTopMovies.executeQuery();

            System.out.println("Top rated movies (" + startDate + "-" + stopDate + "): ");
            while (resultSet.next()) {
                String title = resultSet.getString(1);
                int productionYear = resultSet.getInt(2);
                System.out.println(title + " (" + productionYear + ")");
            }
        } catch (Exception e) {
            System.out.println("ERROR: getTopMovies failed.");
            System.out.println(e);
        }
    }

    public static void getReviews(Connection connection, String title, int productionYear) throws SQLException {
        try {
            String queryString = "SELECT review FROM Review WHERE title = ? AND productionYear = ?";
            PreparedStatement getReviews = connection.prepareStatement(queryString);
            getReviews.setString(1, title);
            getReviews.setInt(2, productionYear);
            ResultSet resultSet = getReviews.executeQuery();

            System.out.println("Movie Reviews for " + title + " (" + productionYear + "): \n");
            while (resultSet.next()) {
                String writtenContent = resultSet.getString(1);
                System.out.println(writtenContent + "\n");
            }
            getReviews.close();
        } catch (Exception e) {
            System.out.println("ERROR: getReviews failed.");
            System.out.println(e);
        }
    }

    public static void main(String args[]) throws SQLException {
        Properties info = new Properties();

        System.out.println("Initializing connection properties...");
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
        info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");

        System.out.println("Creating OracleDataSource...");
        OracleDataSource ods = new OracleDataSource();

        System.out.println("Setting connection properties...");
        ods.setURL(DB_URL);
        ods.setConnectionProperties(info);

        // With AutoCloseable, the connection is closed automatically
        try (OracleConnection connection = (OracleConnection) ods.getConnection()) {
            System.out.println("Connection established!\n");
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.out.println(
                    "Welcome to the Stars 'R' Us Trader Interace!\nEnter 1 to login, or 2 to create an account:");
            String username = "";
            String password = "";
            int marketAccountID = 0;

            if (Integer.parseInt(input.readLine()) == 1) {
                boolean userFound = false;
                while (!userFound) {
                    System.out.print("Enter your username: ");
                    username = input.readLine();
                    System.out.print("Enter your password: ");
                    password = input.readLine();
                    String queryString = "SELECT * FROM UserProfile WHERE username = ? AND password = ?";
                    PreparedStatement checkUser = connection.prepareStatement(queryString);
                    checkUser.setString(1, username);
                    checkUser.setString(2, password);
                    ResultSet resultSet = checkUser.executeQuery();
                    if (resultSet.next()) {
                        userFound = true;
                        System.out.println("Login successful!");
                    } else {
                        System.out.println("Username or password is incorrect. Try again.");
                    }
                    checkUser.close();
                }

                String queryString = "SELECT marketAccountID FROM OwnsAccount WHERE username = ?";
                PreparedStatement getMarketAccountID = connection.prepareStatement(queryString);
                getMarketAccountID.setString(1, username);
                ResultSet resultSet = getMarketAccountID.executeQuery();
                if (resultSet.next()) {
                    marketAccountID = resultSet.getInt(1);
                }
            } else {
                System.out.print("Create a username: ");
                input.readLine();
                username = input.readLine();
                System.out.print("Create a password: ");
                password = input.readLine();
                System.out.print("Enter your Tax ID: ");
                String taxID = input.readLine();
                System.out.print("Enter your full name: ");
                String name = input.readLine();
                System.out.print("Enter the State that you live in: ");
                String state = input.readLine();
                System.out.print("Enter your phone number: ");
                String phoneNumber = input.readLine();
                System.out.print("Enter your email address: ");
                String email = input.readLine();

                // insert new user profile
                String queryString = "INSERT INTO UserProfile VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement insertUser = connection.prepareStatement(queryString);
                insertUser.setString(1, username);
                insertUser.setString(2, password);
                insertUser.setString(3, taxID);
                insertUser.setString(4, name);
                insertUser.setString(5, state);
                insertUser.setString(6, phoneNumber);
                insertUser.setString(7, email);
                insertUser.executeQuery();

                // generate new marketAccountID
                queryString = "SELECT MAX(marketAccountID) FROM MarketAccount";
                Statement getMarketAccountID = connection.createStatement();
                ResultSet resultSet = getMarketAccountID.executeQuery(queryString);
                if (resultSet.next()) {
                    marketAccountID = resultSet.getInt(1);
                }
                marketAccountID++;
                getMarketAccountID.close();

                // create new market account
                queryString = "INSERT INTO MarketAccount VALUES (?)";
                PreparedStatement insertMA = connection.prepareStatement(queryString);
                insertMA.setInt(1, marketAccountID);
                insertMA.executeQuery();

                queryString = "INSERT INTO OwnsAccount VALUES (?, ?, 0)";
                PreparedStatement insertOA = connection.prepareStatement(queryString);
                insertOA.setString(1, username);
                insertOA.setInt(2, marketAccountID);
                insertOA.executeQuery();

                // create initial deposit of $1000
                deposit(connection, 1000, marketAccountID);

                System.out.println("Account created successfully!");
            }

            boolean quit = false;
            while (!quit) {
                System.out.println("Enter one of the following number options to execute a command: ");
                System.out.println("(1) Deposit funds");
                System.out.println("(2) Withdraw funds");
                System.out.println("(3) Buy stocks");
                System.out.println("(4) Sell stocks");
                System.out.println("(5) Cancel a transaction");
                System.out.println("(6) Show your current Balance");
                System.out.println("(7) Show your stock transaction history");
                System.out.println("(8) Show your current stocks");
                System.out.println("(9) List stock price and Star profile info");
                System.out.println("(10) Get top movies in a range of years");
                System.out.println("(11) Get movie reviews for a movie");
                System.out.println("(12) Exit the Trader Interface");
                int option = Integer.parseInt(input.readLine());

                switch (option) {
                    case 1:
                        System.out.print("Enter the amount to deposit: ");
                        double depositAmount = Double.parseDouble(input.readLine());
                        deposit(connection, depositAmount, marketAccountID);
                        break;
                    case 2:
                        System.out.print("Enter the amount to withdraw: ");
                        double withdrawAmount = Double.parseDouble(input.readLine());
                        withdraw(connection, withdrawAmount, marketAccountID);
                        break;
                    case 3:
                        System.out.print("Enter the symbol of the stock you are buying: ");
                        String stockSymbolBuy = input.readLine();
                        System.out.print("Enter the quantity you are buying: ");
                        double quantityBuy = Double.parseDouble(input.readLine());
                        buy(connection, quantityBuy, stockSymbolBuy, username, marketAccountID);
                        break;
                    case 4:
                        System.out.print("Enter the symbol of the stock you are selling: ");
                        String stockSymbolSell = input.readLine();
                        boolean stopSell = false;
                        ArrayList<Double> buyPrices = new ArrayList<Double>();
                        ArrayList<Double> quantitiesSold = new ArrayList<Double>();
                        while (!stopSell) {
                            System.out.print(
                                    "Enter a 'buy price' of the stock you are selling, or enter -1 if you are done: ");
                            double bp = Double.parseDouble(input.readLine());
                            if (bp == -1) {
                                break;
                            }
                            buyPrices.add(bp);
                            System.out.print("Enter the quantity you are selling: ");
                            quantitiesSold.add(Double.parseDouble(input.readLine()));
                        }
                        sell(connection, quantitiesSold, stockSymbolSell, buyPrices, username, marketAccountID);
                        break;
                    case 5:
                        cancel(connection, username, marketAccountID);
                        break;
                    case 6:
                        showBalance(connection, marketAccountID);
                        break;
                    case 7:
                        System.out.println("Enter the stock symbol of the account you want to view: ");
                        showTransactionHistory(connection, username, input.readLine());
                        break;
                    case 8:
                        showStocks(connection, username);
                        break;
                    case 9:
                        System.out.println("Enter the stock symbol of the Star profile you want to view: ");
                        getCurStockPriceAndStarProfile(connection, input.readLine());
                        break;
                    case 10:
                        System.out.println("Enter the starting year for the search range: ");
                        int startDate = Integer.parseInt(input.readLine());
                        System.out.println("Enter the ending year for the search range: ");
                        int stopDate = Integer.parseInt(input.readLine());
                        getTopMovies(connection, startDate, stopDate);
                        break;
                    case 11:
                        System.out.println("Enter the title of the movie: ");
                        String title = input.readLine();
                        System.out.println("Enter the production year of the movie: ");
                        int productionYear = Integer.parseInt(input.readLine());
                        getReviews(connection, title, productionYear);
                        break;
                    case 12:
                        quit = true;
                        connection.close();
                        input.close();
                        System.out.println("Thank you for using the Stars 'R' Us Trader Interface. Goodbye!");
                }
            }
        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}