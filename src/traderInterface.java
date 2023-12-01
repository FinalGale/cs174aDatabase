import java.sql.*;
import java.util.*;

public class traderInterface {
    String username;
    int marketAccountID;

    public void deposit(Connection connection, double depositAmount) throws SQLException {
        System.out.println("Preparing to Deposit...");
        int orderNumber = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        
        try {
            //get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            //get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, marketAccountID);
            updateMarketTransaction.setDate(2, currentDate);
            updateMarketTransaction.setInt(3, orderNumber+1);
            updateMarketTransaction.setString(4, "deposit");
            updateMarketTransaction.setDouble(4, currentBalance + depositAmount);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateMadeMT = connection.prepareStatement("INSERT INTO MadeMT VALUES (?, ?, ?)");
            updateMadeMT.setInt(1, marketAccountID);
            updateMadeMT.setDate(2, currentDate);
            updateMadeMT.setInt(3, orderNumber);
            updateMadeMT.executeQuery();
            updateMadeMT.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Deposit failed.");
            System.out.println(e);
        }
    }
    
    public void withdraw(Connection connection, double withdrawAmount) {
        System.out.println("Preparing to Withdraw...");
        int orderNumber = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;

        try {
            //get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            //get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
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

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, marketAccountID);
            updateMarketTransaction.setDate(2, currentDate);
            updateMarketTransaction.setInt(3, orderNumber+1);
            updateMarketTransaction.setString(4, "withdraw");
            updateMarketTransaction.setDouble(4, currentBalance - withdrawAmount);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateMadeMT = connection.prepareStatement("INSERT INTO MadeMT VALUES (?, ?, ?)");
            updateMadeMT.setInt(1, marketAccountID);
            updateMadeMT.setDate(2, currentDate);
            updateMadeMT.setInt(3, orderNumber);
            updateMadeMT.executeQuery();
            updateMadeMT.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Withdraw failed.");
            System.out.println(e);
        }
    }

    public void buy(Connection connection, double quantity, String stockSymbol) {
        System.out.println("Preparing to buy...");
        int orderNumber = 0;
        int stockAccountID = 0;
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        double currentPrice = 0;
        double currentShares = 0;

        try {
            //get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            //get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            //get the current stock price
            queryString = "SELECT currentPrice FROM StarProfile WHERE stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            if (resultSet.next()) {
                currentPrice = resultSet.getDouble(1);
            }
            getPrice.close();

            double totalCost = quantity * currentPrice + 20;
            if (currentBalance < totalCost) {
                System.out.println("ERROR: Buy failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, marketAccountID);
            updateMarketTransaction.setDate(2, currentDate);
            updateMarketTransaction.setInt(3, orderNumber+1);
            updateMarketTransaction.setString(4, "buy");
            updateMarketTransaction.setDouble(4, currentBalance - totalCost);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateMadeMT = connection.prepareStatement("INSERT INTO MadeMT VALUES (?, ?, ?)");
            updateMadeMT.setInt(1, marketAccountID);
            updateMadeMT.setDate(2, currentDate);
            updateMadeMT.setInt(3, orderNumber);
            updateMadeMT.executeQuery();
            updateMadeMT.close();

            //check if we need to create a new stock account
            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE username = ? AND stockSymbol = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            getStockAccountID.setString(1, username);
            getStockAccountID.setString(2, stockSymbol);
            resultSet = getStockAccountID.executeQuery();
            if (!resultSet.next()) {
                //generate new stockAccountID by getting the last ID
                queryString = "SELECT stockAccountID FROM OwnsAccount WHERE username = ? ORDER BY stockAccountID DESC";
                PreparedStatement getLast = connection.prepareStatement(queryString);
                getLast.setString(1, username);
                resultSet = getLast.executeQuery();
                if (resultSet.next()) {
                    stockAccountID = resultSet.getInt(1);
                }
                getLast.close();

                PreparedStatement updateOwnsAccount = connection.prepareStatement("INSERT INTO OwnsAccount VALUES (?, ?, ?, ?)");
                updateOwnsAccount.setString(1, username);
                updateOwnsAccount.setInt(2, marketAccountID);
                updateOwnsAccount.setInt(3, stockAccountID+1);
                updateOwnsAccount.setString(4, "buy");
                updateOwnsAccount.executeQuery();
                updateOwnsAccount.close();

                PreparedStatement updateStockAccount = connection.prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                updateStockAccount.setInt(1, stockAccountID+1);
                updateStockAccount.setString(2, stockSymbol);
                updateStockAccount.setDouble(3, currentPrice);
                updateStockAccount.setDouble(4, quantity);
                updateStockAccount.executeQuery();
                updateStockAccount.close();
            } else {
                stockAccountID = resultSet.getInt(1);
            }
            getStockAccountID.close();

            //get the current number of shares 
            queryString = "SELECT quantity FROM StockAccount WHERE StockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
            PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
            getCurrentShares.setInt(1, stockAccountID);
            getCurrentShares.setString(2, stockSymbol);
            getCurrentShares.setDouble(3, currentPrice);
            resultSet = getCurrentShares.executeQuery();
            if (resultSet.next()) {
                currentShares = resultSet.getInt(1);
            }
            getCurrentShares.close();

            if (currentShares == 0) {
                PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                updateStockTransaction.setInt(1, stockAccountID);
                updateStockTransaction.setString(2, stockSymbol);
                updateStockTransaction.setDouble(3, currentPrice);
                updateStockTransaction.setDouble(8, quantity);
                updateStockTransaction.executeQuery();
                updateStockTransaction.close();
            } else {
                PreparedStatement updateStockTransaction = connection.prepareStatement("UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ?");
                updateStockTransaction.setDouble(1, quantity + currentShares);
                updateStockTransaction.setInt(2, stockAccountID);
                updateStockTransaction.setString(3, stockSymbol);
                updateStockTransaction.executeQuery();
                updateStockTransaction.close();
            }

            PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            updateStockTransaction.setInt(1, stockAccountID);
            updateStockTransaction.setString(2, stockSymbol);
            updateStockTransaction.setDouble(3, currentPrice);
            updateStockTransaction.setDate(4, currentDate);
            updateStockTransaction.setInt(5, orderNumber + 1);
            updateStockTransaction.setString(6, "buy");
            updateStockTransaction.setDouble(7, currentPrice);
            updateStockTransaction.setDouble(8, quantity);
            updateStockTransaction.executeQuery();
            updateStockTransaction.close();

            PreparedStatement updateMadeST = connection.prepareStatement("INSERT INTO MadeST VALUES (?, ?, ?, ?)");
            updateMadeST.setInt(1, stockAccountID);
            updateMadeST.setString(2, stockSymbol);
            updateMadeST.setDate(3, currentDate);
            updateMadeST.setInt(4, orderNumber);
            updateMadeST.executeQuery();
            updateMadeST.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Buy failed.");
            System.out.println(e);
        }
    }

    public void sell(Connection connection, double[] quantitiesSold, String stockSymbol, double[] buyPrices) {
        System.out.println("Preparing to sell...");
        java.sql.Date currentDate = new java.sql.Date(0);
        int n = quantitiesSold.length;
        double currentBalance = 0;
        double currentPrice = 0;
        int orderNumber = 0;
        int stockAccountID = 0;
        double[] currentShares = new double[n];

        try {
            //get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            //get the trader's current balance and order number
            queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            //get the current stock price
            queryString = "SELECT currentPrice FROM StarProfile WHERE stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            if (resultSet.next()) {
                currentPrice = resultSet.getDouble(1);
            }
            getPrice.close();

            //get stockAccountID
            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE username = ? AND stockSymbol = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            getStockAccountID.setString(1, username);
            getStockAccountID.setString(2, stockSymbol);
            resultSet = getStockAccountID.executeQuery();
            if (resultSet.next()) {
                stockAccountID = resultSet.getInt(1);
            }

            //get the number of shares the customer currently owns
            for (int i = 0; i < n; i++) {
                queryString = "SELECT quantity FROM StockAccount WHERE StockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
                PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                getCurrentShares.setInt(1, stockAccountID);
                getCurrentShares.setString(2, stockSymbol);
                getCurrentShares.setDouble(3, buyPrices[i]);
                resultSet = getCurrentShares.executeQuery();
                if (resultSet.next()) {
                    currentShares[i] = resultSet.getDouble(1);
                }
                getCurrentShares.close();
            }

            for (int i = 0; i < n; i++) {
                if (currentShares[i] < quantitiesSold[i]) {
                    System.out.println("ERROR: Sell failed. You cannot sell more shares than you own.");
                    return;
                }
            }
            //ensure that the balance won't go negative after selling
            double revenue = -20;
            for (int i = 0; i < n; i++) {
                revenue += quantitiesSold[i] * currentPrice;
            }
            if (currentBalance + revenue < 0) {
                System.out.println("ERROR: Sell failed. Your account lacks sufficient funds.");
                return;
            } 

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, marketAccountID);
            updateMarketTransaction.setDate(2, currentDate);
            updateMarketTransaction.setInt(3, orderNumber + 1);
            updateMarketTransaction.setString(4, "sell");
            updateMarketTransaction.setDouble(5, currentBalance + revenue);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateMadeMT = connection.prepareStatement("INSERT INTO MadeMT VALUES (?, ?, ?)");
            updateMadeMT.setInt(1, marketAccountID);
            updateMadeMT.setDate(2, currentDate);
            updateMadeMT.setInt(3, orderNumber);
            updateMadeMT.executeQuery();
            updateMadeMT.close();

            for (int i = 0; i < n; i++) {
                PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                updateStockTransaction.setInt(1, stockAccountID);
                updateStockTransaction.setString(2, stockSymbol);
                updateStockTransaction.setDouble(3, buyPrices[i]);
                updateStockTransaction.setDate(4, currentDate);
                updateStockTransaction.setInt(5, orderNumber + 1);
                updateStockTransaction.setString(6, "sell");
                updateStockTransaction.setDouble(7, currentPrice);
                updateStockTransaction.setDouble(8, quantitiesSold[i]);
                updateStockTransaction.executeQuery();
                updateStockTransaction.close();
            }

            PreparedStatement updateMadeST = connection.prepareStatement("INSERT INTO MadeST VALUES (?, ?, ?, ?)");
            updateMadeST.setInt(1, stockAccountID);
            updateMadeST.setString(2, stockSymbol);
            updateMadeST.setDate(3, currentDate);
            updateMadeST.setInt(4, orderNumber);
            updateMadeST.executeQuery();
            updateMadeST.close();

            for (int i = 0; i < n; i++) {
                if (currentShares[i] == quantitiesSold[i]) {
                    //we are selling all of this stock, so we delete the row
                    PreparedStatement updateStonk = connection.prepareStatement("DELETE FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                    updateStonk.setInt(1, stockAccountID);
                    updateStonk.setString(2, stockSymbol);
                    updateStonk.setDouble(3, buyPrices[i]);
                    updateStonk.executeQuery();
                    updateStonk.close();
                } else {
                    //we are selling part of this stock, so we subtract from the quantity
                    PreparedStatement updateStonk = connection.prepareStatement("UPDATE StonckAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                    updateStonk.setDouble(1, currentShares[i] - quantitiesSold[i]);
                    updateStonk.setInt(2, stockAccountID);
                    updateStonk.setString(3, stockSymbol);
                    updateStonk.setDouble(4, buyPrices[i]);
                    updateStonk.executeQuery();
                    updateStonk.close();
                }
            }

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Sell failed.");
            System.out.println(e);
        }
    }

    public void cancel(Connection connection) {
        //We should only be able to cancel the most recent buy/sell transaction for the day
        System.out.println("Preparing to cancel...");
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        int orderNumber = 0;
        int n = 0;

        try {
            //get the trader's current balance and order number
            String queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            ResultSet resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
                orderNumber = resultSet.getInt(2);
            }
            getBalance.close();

            //get the most recent buy/sell MarketTransaction
            queryString = "SELECT * FROM MarketTransaction WHERE marketAccountID = ? AND date = ? AND (transactionType = 'B' OR transactionType = 'S') ORDER BY orderNumber DESC";
            PreparedStatement getMarketTransaction = connection.prepareStatement(queryString);
            getMarketTransaction.setInt(1, marketAccountID);
            getMarketTransaction.setDate(2, currentDate);
            resultSet = getMarketTransaction.executeQuery();
            if (!resultSet.next()) {
                System.out.println("ERROR: Cancel failed. There are no transactions to cancel.");
                return;
            }
            java.sql.Date cancelDate = resultSet.getDate(2);
            int cancelOrderNumber = resultSet.getInt(3);
            String cancelTransactionType = resultSet.getString(4);
            double cancelBalance = resultSet.getDouble(5);
            getMarketTransaction.close();

            //get number of different buy prices (for sell only)
            queryString = "SELECT COUNT(*) FROM StockTransaction WHERE orderNumber = ? AND stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getCount = connection.prepareStatement(queryString);
            getCount.setInt(1, cancelOrderNumber);
            getCount.setString(2, username);
            resultSet = getCount.executeQuery();
            if (resultSet.next()) {
                n = resultSet.getInt(1);
            }

            //get the most recent buy/sell StockTransaction
            queryString = "SELECT * FROM StockTransaction WHERE orderNumber = ? AND stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getStockTransaction = connection.prepareStatement(queryString);
            getStockTransaction.setInt(1, cancelOrderNumber);
            getStockTransaction.setString(2, username);
            resultSet = getStockTransaction.executeQuery();
            int j = 0;
            while (resultSet.next()) {
                int stockAccountID = resultSet.getInt(1);
                String stockSymbol = resultSet.getString(2);
                String transactionType = resultSet.getString(6);
                double buyPrice = resultSet.getDouble(3);
                double sellPrices = resultSet.getDouble(7);
                double quantities = resultSet.getDouble(8);
                j += 1;
            }
            getStockTransaction.close();

            if (cancelTransactionType == "B") {
                //cancel the last buy transaction
                double payback = buyPrices[0] + quantities[0] - 20;
                if (currentBalance + payback < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                //delete the appropriate stocks from the account
                for (int i = 0; i < n; i++) {
                    queryString = "SELECT quantity FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
                    PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                    getCurrentShares.setInt(1, stockAccountID);
                    getCurrentShares.setString(2, stockSymbol);
                    getCurrentShares.setDouble(3, cancelBuyPrices[i]);
                    resultSet = getCurrentShares.executeQuery();
                    double currentShares = resultSet.getDouble(1);
                    getCurrentShares.close();

                    if (currentShares == quantitiesD[i]) {
                        PreparedStatement deleteStonk = connection.prepareStatement("DELETE FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                        deleteStonk.setInt(1, accountId);
                        deleteStonk.setString(2, stockSymbol);
                        deleteStonk.setDouble(3, buyPricesD[i]);
                        deleteStonk.executeQuery();
                        deleteStonk.close();
                    } else {
                        PreparedStatement updateStonk = connection.prepareStatement("UPDATE Stonk SET quantity = ? WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                        updateStonk.setDouble(1, currentShares - quantitiesD[i]);
                        updateStonk.setInt(2, accountId);
                        updateStonk.setString(3, stockSymbol);
                        updateStonk.setDouble(4, buyPricesD[i]);
                        updateStonk.executeQuery();
                        updateStonk.close();  
                    }    
                }     

                //add the cancel transaction to the tables
                PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
                updateMarketTransaction.setInt(1, accountId);
                updateMarketTransaction.setTimestamp(2, currentTimestamp);
                updateMarketTransaction.setString(3, "CB");
                updateMarketTransaction.setDouble(4, amountTransferred);
                updateMarketTransaction.executeQuery();
                updateMarketTransaction.close();

                PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?)");
                updateStockTransaction.setInt(1, accountId);
                updateStockTransaction.setTimestamp(2, currentTimestamp);
                updateStockTransaction.setString(3, "CB");
                updateStockTransaction.setString(4, stockSymbol);
                updateStockTransaction.setString(5, buyPrices);
                updateStockTransaction.setDouble(6, sellPrice);
                updateStockTransaction.setString(7, quantities);
                updateStockTransaction.executeQuery();
                updateStockTransaction.close();
                    
            } else if (cancelTransactionType == "S") {
                //cancel the last sell transaction
                double loss = -amountTransferred - 20;
                if (currentBalance + loss < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
                updateBalance.setInt(1, accountId);
                updateBalance.setDouble(2, currentBalance + loss);
                updateBalance.setTimestamp(3, currentTimestamp);
                updateBalance.executeQuery();
                updateBalance.close();

                //add back the appropriate stocks to stonk
                for (int i = 0; i < n; i++) {
                    queryString = "SELECT S.quantity FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?";
                    PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                    getCurrentShares.setInt(1, accountId);
                    getCurrentShares.setString(2, stockSymbol);
                    getCurrentShares.setDouble(3, buyPricesD[i]);
                    resultSet = getCurrentShares.executeQuery();
                    double currentShares = 0;
                    if (resultSet.next()) {
                        currentShares = resultSet.getDouble(1);
                    }
                    getCurrentShares.close();

                    if (currentShares == 0) {
                        //we need to put back the stocks as insertions since the trader doesn't have any
                        PreparedStatement updateStonk = connection.prepareStatement("INSERT INTO Stonk VALUES (?, ?, ?, ?)");
                        updateStonk.setInt(1, accountId);
                        updateStonk.setString(2, stockSymbol);
                        updateStonk.setDouble(3, buyPricesD[i]);
                        updateStonk.setDouble(4, quantitiesD[i]);
                        updateStonk.executeQuery();
                        updateStonk.close();
                    } else {
                        //the trader has some of the stocks so we add on to the existing row
                        PreparedStatement updateStonk = connection.prepareStatement("UPDATE Stonk SET quantity = ? WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                        updateStonk.setDouble(1, currentShares + quantitiesD[i]);
                        updateStonk.setInt(2, accountId);
                        updateStonk.setString(3, stockSymbol);
                        updateStonk.setDouble(4, buyPricesD[i]);
                        updateStonk.executeQuery();
                        updateStonk.close();  
                    }    
                }    

                //add the cancel transaction to the tables
                PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
                updateMarketTransaction.setInt(1, accountId);
                updateMarketTransaction.setTimestamp(2, currentTimestamp);
                updateMarketTransaction.setString(3, "CS");
                updateMarketTransaction.setDouble(4, amountTransferred);
                updateMarketTransaction.executeQuery();
                updateMarketTransaction.close();

                PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?)");
                updateStockTransaction.setInt(1, accountId);
                updateStockTransaction.setTimestamp(2, currentTimestamp);
                updateStockTransaction.setString(3, "CS");
                updateStockTransaction.setString(4, stockSymbol);
                updateStockTransaction.setString(5, buyPrices);
                updateStockTransaction.setDouble(6, sellPrice);
                updateStockTransaction.setString(7, quantities);
                updateStockTransaction.executeQuery();
                updateStockTransaction.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: Cancel failed.");
            System.out.println(e);
        }
    }

    public void showBalance(Connection connection) {
        try {
            //get the trader's current balance
            String queryString = "SELECT B.amount FROM BALANCE AS B WHERE B.accountId = ? AND B.timestamp = " + 
                                    "(SELECT MAX(B2.timestamp) FROM BALANCE AS B2 WHERE B2.accountId = ?)";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, accountId);
            getBalance.setInt(2, accountId);
            ResultSet resultSet = getBalance.executeQuery();
            double currentBalance = resultSet.getDouble(1);
            getBalance.close();

            System.out.println("Your current account balance is: " + String.format("%.2f", currentBalance));
        } catch (Exception e) {
            System.out.println("ERROR: showBalance failed.");
            System.out.println(e);
        }
    }

    public void showTransactionHistory(Connection connection) {
        try {
            //get all StockTransactions
            String queryString = "SELECT * FROM StockTransaction AS S WHERE S.accountId = ? ORDER BY S.Timestamp";
            PreparedStatement getStockTransaction = connection.prepareStatement(queryString);
            getStockTransaction.setInt(1, accountId);
            ResultSet resultSet = getStockTransaction.executeQuery();

            System.out.println("Transactions this month: ");
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp(2);
                String transactionType = resultSet.getString(3);
                String stockSymbol = resultSet.getString(4);
                String buyPrices = resultSet.getString(5);
                double sellPrice = resultSet.getDouble(6);
                String quantities = resultSet.getString(7);
                getStockTransaction.close();

                System.out.println(
                    "Timestamp: " + timestamp.toLocalDateTime() + "; Transaction Type: " + transactionType + "; Stock Symbol: " +
                        stockSymbol + "; Buy Prices: " + buyPrices + "; Sell Price: " + sellPrice + "; Quantities: " + quantities);
            }
        } catch (Exception e) {
            System.out.println("ERROR: showTransactionHistory failed.");
            System.out.println(e);
        }
    }

    public void getCurStockPriceAndStarProfile(Connection connection, String stockSymbol) {
        try {
            String queryString = "SELECT * FROM StarProfile AS S WHERE S.stockSymbol = ?";
            PreparedStatement getStockInfo = connection.prepareStatement(queryString);
            getStockInfo.setString(1, stockSymbol);
            ResultSet resultSet = getStockInfo.executeQuery();
            String name = resultSet.getString(2);
            double currentPrice = resultSet.getDouble(4);
            Timestamp dateOfBirth = resultSet.getTimestamp(5);
            getStockInfo.close();

            System.out.println("Current Price for " + stockSymbol + ": " + currentPrice + " USD\n" +
                                "Actor Profile for " + name + ":\n" +
                                "Date of Birth: " + dateOfBirth);
            System.out.println("----------------------------------------");

            queryString = "SELECT * FROM SignedContract AS S WHERE S.stockSymbol = ?";
            PreparedStatement getContractInfo = connection.prepareStatement(queryString);
            getContractInfo.setString(1, stockSymbol);
            resultSet = getStockInfo.executeQuery();

            System.out.println("Movie Contracts signed by " + name + ": ");
            while(resultSet.next()) {
                String title = resultSet.getString(2);
                int productionYear = resultSet.getInt(3);
                String role = resultSet.getString(4);
                double totalValue = resultSet.getDouble(5);
                System.out.println("Movie title: " + title + "; Production Year: " + productionYear + "; Role: " + role + "; Total Payment Value: " + totalValue);
            }
            getContractInfo.close();
        } catch (Exception e) {
            System.out.println("ERROR: getCurStockPriceAndStarProfile failed.");
            System.out.println(e);
        }
    }

    public void getTopMovies(Connection connection, int startDate, int stopDate) {
        try {
            String queryString = "SELECT M.title, M.productionYear FROM Movie AS M WHERE M.productionYear >= ? AND M.productionYear <= ? AND M.rating = 10";
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

    public void getReviews(Connection connection, String title, int productionYear) {
        try {
            String queryString = "SELECT * FROM Review as R WHERE M.title = ? AND M.productionYear = ?";
            PreparedStatement getReviews = connection.prepareStatement(queryString);
            getReviews.setString(1, title);
            getReviews.setInt(2, productionYear);
            ResultSet resultSet = getReviews.executeQuery();

            System.out.println("Movie Reviews for " + title + " (" + productionYear + "): \n");
            while(resultSet.next()) {
                String username = resultSet.getString(1);
                String writtenContent = resultSet.getString(4);
                System.out.println("Review by " + username + ": ");
                System.out.println(writtenContent + "\n");
            }
            getReviews.close();
        } catch (Exception e) {
            System.out.println("ERROR: getReviews failed.");
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        Scanner input = new Scanner(System.in);
        System.out.println("Welcome to the Stars 'R' Us Trader Interace!\nEnter 1 to login, or 2 to create an account.");
        
    }
}