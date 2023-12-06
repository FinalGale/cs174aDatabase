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

            PreparedStatement insertMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber+1);
            insertMT.setString(4, "deposit");
            insertMT.setDouble(4, currentBalance + depositAmount);
            insertMT.executeQuery();
            insertMT.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Deposit failed.");
            System.out.println(e);
        }
    }
    
    public void withdraw(Connection connection, double withdrawAmount) throws SQLException {
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

            PreparedStatement insertMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber+1);
            insertMT.setString(4, "withdraw");
            insertMT.setDouble(4, currentBalance - withdrawAmount);
            insertMT.executeQuery();
            insertMT.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Withdraw failed.");
            System.out.println(e);
        }
    }

    public void buy(Connection connection, double quantity, String stockSymbol) throws SQLException {
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

            PreparedStatement insertMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber+1);
            insertMT.setString(4, "buy");
            insertMT.setDouble(4, currentBalance - totalCost);
            insertMT.executeQuery();
            insertMT.close();

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

                PreparedStatement insertOA = connection.prepareStatement("INSERT INTO OwnsAccount VALUES (?, ?, ?, ?)");
                insertOA.setString(1, username);
                insertOA.setInt(2, marketAccountID);
                insertOA.setInt(3, stockAccountID+1);
                insertOA.setString(4, "buy");
                insertOA.executeQuery();
                insertOA.close();

                PreparedStatement insertSA = connection.prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                insertSA.setInt(1, stockAccountID+1);
                insertSA.setString(2, stockSymbol);
                insertSA.setDouble(3, currentPrice);
                insertSA.setDouble(4, quantity);
                insertSA.executeQuery();
                insertSA.close();
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
                PreparedStatement insertST = connection.prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                insertST.setInt(1, stockAccountID);
                insertST.setString(2, stockSymbol);
                insertST.setDouble(3, currentPrice);
                insertST.setDouble(8, quantity);
                insertST.executeQuery();
                insertST.close();
            } else {
                PreparedStatement updateST = connection.prepareStatement("UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ?");
                updateST.setDouble(1, quantity + currentShares);
                updateST.setInt(2, stockAccountID);
                updateST.setString(3, stockSymbol);
                updateST.executeQuery();
                updateST.close();
            }

            PreparedStatement insertST = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
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

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Buy failed.");
            System.out.println(e);
        }
    }

    public void sell(Connection connection, double[] quantitiesSold, String stockSymbol, double[] buyPrices) throws SQLException {
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

            PreparedStatement insertMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            insertMT.setInt(1, marketAccountID);
            insertMT.setDate(2, currentDate);
            insertMT.setInt(3, orderNumber + 1);
            insertMT.setString(4, "sell");
            insertMT.setDouble(5, currentBalance + revenue);
            insertMT.executeQuery();
            insertMT.close();

            for (int i = 0; i < n; i++) {
                PreparedStatement insertST = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                insertST.setInt(1, stockAccountID);
                insertST.setString(2, stockSymbol);
                insertST.setDouble(3, buyPrices[i]);
                insertST.setDate(4, currentDate);
                insertST.setInt(5, orderNumber + 1);
                insertST.setString(6, "sell");
                insertST.setDouble(7, currentPrice);
                insertST.setDouble(8, quantitiesSold[i]);
                insertST.executeQuery();
                insertST.close();
            }

            for (int i = 0; i < n; i++) {
                if (currentShares[i] == quantitiesSold[i]) {
                    //we are selling all of this stock, so we delete the row
                    PreparedStatement updateSA = connection.prepareStatement("DELETE FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                    updateSA.setInt(1, stockAccountID);
                    updateSA.setString(2, stockSymbol);
                    updateSA.setDouble(3, buyPrices[i]);
                    updateSA.executeQuery();
                    updateSA.close();
                } else {
                    //we are selling part of this stock, so we subtract from the quantity
                    PreparedStatement updateSA = connection.prepareStatement("UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                    updateSA.setDouble(1, currentShares[i] - quantitiesSold[i]);
                    updateSA.setInt(2, stockAccountID);
                    updateSA.setString(3, stockSymbol);
                    updateSA.setDouble(4, buyPrices[i]);
                    updateSA.executeQuery();
                    updateSA.close();
                }
            }

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Sell failed.");
            System.out.println(e);
        }
    }

    public void cancel(Connection connection) throws SQLException {
        System.out.println("Preparing to cancel...");
        java.sql.Date currentDate = new java.sql.Date(0);
        double currentBalance = 0;
        int n = 0;
        int cancelOrderNumber = 0;

        try {
            //get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }
            getCurrentDate.close();

            //get the trader's current balance
            queryString = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
            }
            getBalance.close();

            //get the most recent buy/sell MarketTransaction
            queryString = "SELECT * FROM MarketTransaction WHERE marketAccountID = ? AND date = ? AND (transactionType = 'buy' OR transactionType = 'sell') ORDER BY orderNumber DESC";
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

            //get number of different buy prices
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

            //delete the transactions
            queryString = "DELETE FROM MarketTransaction WHERE marketAccountID = ? AND orderNumber = ?";
            PreparedStatement deleteMT = connection.prepareStatement(queryString);
            deleteMT.setInt(1, marketAccountID);
            deleteMT.setInt(3, cancelOrderNumber);
            resultSet = deleteMT.executeQuery();

            queryString = "DELETE FROM StockTransaction WHERE stockAccountID = ? AND orderNumber = ?";
            PreparedStatement deleteST = connection.prepareStatement(queryString);
            deleteST.setInt(2, sid);
            deleteST.setInt(1, cancelOrderNumber);
            resultSet = deleteST.executeQuery();            

            if (cancelTransactionType == "buy") {
                //cancel the last buy transaction
                double payback = buyPrices[0] * quantities[0] - 20;
                if (currentBalance + payback < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                //delete the appropriate stocks from the account
                for (int i = 0; i < n; i++) {
                    queryString = "SELECT quantity FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?";
                    PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                    getCurrentShares.setInt(1, sid); 
                    getCurrentShares.setString(2, stockSymbol);
                    getCurrentShares.setDouble(3, buyPrices[i]);
                    resultSet = getCurrentShares.executeQuery();
                    double currentShares = resultSet.getDouble(1);
                    getCurrentShares.close();

                    if (currentShares == quantities[i]) {
                        PreparedStatement deleteSA = connection.prepareStatement("DELETE FROM StockAccount WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        deleteSA.setInt(1, sid);
                        deleteSA.setString(2, stockSymbol);
                        deleteSA.setDouble(3, buyPrices[i]);
                        deleteSA.executeQuery();
                        deleteSA.close();
                    } else {
                        PreparedStatement updateSA = connection.prepareStatement("UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        updateSA.setDouble(1, currentShares - quantities[i]);
                        updateSA.setInt(2, sid);
                        updateSA.setString(3, stockSymbol);
                        updateSA.setDouble(4, buyPrices[i]);
                        updateSA.executeQuery();
                        updateSA.close();  
                    }    
                }     

                //add the cancel transaction to the table
                PreparedStatement updateMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
                updateMT.setInt(1, marketAccountID);
                updateMT.setDate(2, currentDate);
                updateMT.setInt(3, cancelOrderNumber+1);
                updateMT.setString(4, "cancelBuy");
                updateMT.setDouble(5, currentBalance + payback);
                updateMT.executeQuery();
                updateMT.close();

            } else if (cancelTransactionType == "sell") {
                //cancel the last sell transaction
                double loss = -20;
                for (int i = 0; i < n; i++) {
                    loss -= (sellPrices[i] - buyPrices[i]);
                }

                if (currentBalance + loss < 0) {
                    System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                    return;
                }

                //add back the appropriate stocks to stockAccount
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
                        //we need to put back the stocks as insertions since the trader doesn't have any
                        PreparedStatement insertSA = connection.prepareStatement("INSERT INTO StockAccount VALUES (?, ?, ?, ?)");
                        insertSA.setInt(1, sid);
                        insertSA.setString(2, stockSymbol);
                        insertSA.setDouble(3, buyPrices[i]);
                        insertSA.setDouble(4, quantities[i]);
                        insertSA.executeQuery();
                        insertSA.close();
                    } else {
                        //the trader has some of the stocks already so we add on to the existing row
                        PreparedStatement updateSA = connection.prepareStatement("UPDATE StockAccount SET quantity = ? WHERE stockAccountID = ? AND stockSymbol = ? AND buyPrice = ?");
                        updateSA.setDouble(1, currentShares + quantities[i]);
                        updateSA.setInt(2, sid);
                        updateSA.setString(3, stockSymbol);
                        updateSA.setDouble(4, buyPrices[i]);
                        updateSA.executeQuery();
                        updateSA.close();  
                    }    
                }    

                //add the cancel transaction to the table
                PreparedStatement insertMT = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
                insertMT.setInt(1, marketAccountID);
                insertMT.setDate(2, currentDate);
                insertMT.setInt(3, cancelOrderNumber+1);
                insertMT.setString(4, "cancelSell");
                insertMT.setDouble(5, currentBalance + loss);
                insertMT.executeQuery();
                insertMT.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: Cancel failed.");
            System.out.println(e);
        }
    }

    public void showBalance(Connection connection) throws SQLException {
        try {
            //get the trader's current balance
            double currentBalance = 0;
            String queryString = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? ORDER BY date DESC, orderNumber DESC";
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

    public void showTransactionHistory(Connection connection, String stockSymbol) throws SQLException {
        try {
            //get all StockTransactions
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
                "; Buy Price: " + buyPrice + (transactionType == "sell" ? ("; Sell Price: " + sellPrice) : "") + "; Quantity: " + quantity);
            }
        } catch (Exception e) {
            System.out.println("ERROR: showTransactionHistory failed.");
            System.out.println(e);
        }
    }

    public void getCurStockPriceAndStarProfile(Connection connection, String stockSymbol) throws SQLException {
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

    public void getTopMovies(Connection connection, int startDate, int stopDate) throws SQLException {
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

    public void getReviews(Connection connection, String title, int productionYear) throws SQLException {
        try {
            String queryString = "SELECT review FROM Movie WHERE title = ? AND productionYear = ?";
            PreparedStatement getReviews = connection.prepareStatement(queryString);
            getReviews.setString(1, title);
            getReviews.setInt(2, productionYear);
            ResultSet resultSet = getReviews.executeQuery();

            System.out.println("Movie Reviews for " + title + " (" + productionYear + "): \n");
            while(resultSet.next()) {
                String writtenContent = resultSet.getString(1);
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