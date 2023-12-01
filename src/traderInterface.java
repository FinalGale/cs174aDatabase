import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class traderInterface {
    String currentUsername;
    int accountId;

    public void deposit(Connection connection, double depositAmount) throws SQLException {
        System.out.println("Preparing to Deposit...");
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        double currentBalance;
        
        try {
            //get the trader's current balance
            String queryString = "SELECT B.amount FROM BALANCE AS B WHERE B.accountId = ? AND B.timestamp = " + 
                                    "(SELECT MAX(B2.timestamp) FROM BALANCE AS B2 WHERE B2.accountId = ?)";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, accountId);
            getBalance.setInt(2, accountId);
            ResultSet resultSet = getBalance.executeQuery();
            currentBalance = resultSet.getDouble(1);
            getBalance.close();

            PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
            updateBalance.setInt(1, accountId);
            updateBalance.setDouble(2, currentBalance + depositAmount);
            updateBalance.setTimestamp(3, currentTime);
            updateBalance.executeQuery();
            updateBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, accountId);
            updateMarketTransaction.setTimestamp(2, currentTime);
            updateMarketTransaction.setString(3, "D");
            updateMarketTransaction.setDouble(4, depositAmount);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Deposit failed.");
            System.out.println(e);
        }
    }
    
    public void withdraw(Connection connection, double withdrawAmount) {
        System.out.println("Preparing to Withdraw...");
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        double currentBalance;

        try {
            //get the trader's current balance
            String queryString = "SELECT B.amount FROM BALANCE AS B WHERE B.accountId = ? AND B.timestamp = " + 
                                    "(SELECT MAX(B2.timestamp) FROM BALANCE AS B2 WHERE B2.accountId = ?)";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, accountId);
            getBalance.setInt(2, accountId);
            ResultSet resultSet = getBalance.executeQuery();
            currentBalance = resultSet.getDouble(1);
            getBalance.close();

            if (currentBalance < withdrawAmount) {
                System.out.println("ERROR: Withdraw failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
            updateBalance.setInt(1, accountId);
            updateBalance.setDouble(2, currentBalance - withdrawAmount);
            updateBalance.setTimestamp(3, currentTime);
            updateBalance.executeQuery();
            updateBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, accountId);
            updateMarketTransaction.setTimestamp(2, currentTime);
            updateMarketTransaction.setString(3, "W");
            updateMarketTransaction.setDouble(4, withdrawAmount);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Withdraw failed.");
            System.out.println(e);
        }
    }

    public void buy(Connection connection, double quantity, String stockSymbol) {
        System.out.println("Preparing to buy...");
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        double currentBalance, currentPrice;

        try {
            //get the trader's current balance
            String queryString = "SELECT B.amount FROM BALANCE AS B WHERE B.accountId = ? AND B.timestamp = " + 
                                    "(SELECT MAX(B2.timestamp) FROM BALANCE AS B2 WHERE B2.accountId = ?)";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, accountId);
            getBalance.setInt(2, accountId);
            ResultSet resultSet = getBalance.executeQuery();
            currentBalance = resultSet.getDouble(1);
            getBalance.close();

            //get the current stock price
            queryString = "SELECT S.currentPrice FROM StarProfile AS S WHERE S.stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            currentPrice = resultSet.getDouble(1);
            getPrice.close();

            double totalCost = quantity * currentPrice + 20;
            if (currentBalance < totalCost) {
                System.out.println("ERROR: Buy failed. Your account lacks sufficient funds.");
                return;
            }

            PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
            updateBalance.setInt(1, accountId);
            updateBalance.setDouble(2, currentBalance - totalCost);
            updateBalance.setTimestamp(3, currentTime);
            updateBalance.executeQuery();
            updateBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, accountId);
            updateMarketTransaction.setTimestamp(2, currentTime);
            updateMarketTransaction.setString(3, "B");
            updateMarketTransaction.setDouble(4, totalCost);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?)");
            updateStockTransaction.setInt(1, accountId);
            updateStockTransaction.setTimestamp(2, currentTime);
            updateStockTransaction.setString(3, "B");
            updateStockTransaction.setString(4, stockSymbol);
            updateStockTransaction.setString(5, Double.toString(currentPrice));
            updateStockTransaction.setDouble(6, currentPrice);
            updateStockTransaction.setString(7, Double.toString(quantity));
            updateStockTransaction.executeQuery();
            updateStockTransaction.close();

            queryString = "SELECT S.quantity FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?";
            PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
            getCurrentShares.setInt(1, accountId);
            getCurrentShares.setString(2, stockSymbol);
            getCurrentShares.setDouble(3, currentPrice);
            resultSet = getCurrentShares.executeQuery();
            double currentShares = 0;
            if (resultSet.next()) {
                currentShares = resultSet.getDouble(1);
            }
            getCurrentShares.close();

            if (currentShares == 0) {
                //insert the stock as brand new since the trader doesn't have it yet
                PreparedStatement updateStonk = connection.prepareStatement("INSERT INTO Stonk VALUES (?, ?, ?, ?)");
                updateStonk.setInt(1, accountId);
                updateStonk.setString(2, stockSymbol);
                updateStonk.setDouble(3, currentPrice);
                updateStonk.setDouble(4, quantity);
                updateStonk.executeQuery();
                updateStonk.close();
            } else {
                //the trader has some of the stock already so we add on to the existing row
                PreparedStatement updateStonk = connection.prepareStatement("UPDATE Stonk SET quantity = ? WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                updateStonk.setDouble(1, currentShares + quantity);
                updateStonk.setInt(2, accountId);
                updateStonk.setString(3, stockSymbol);
                updateStonk.setDouble(4, currentPrice);
                updateStonk.executeQuery();
                updateStonk.close();  
            }  

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Buy failed.");
            System.out.println(e);
        }
    }

    public void sell(Connection connection, double[] quantitiesSold, String stockSymbol, double[] buyPrices) {
        System.out.println("Preparing to sell...");
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        int n = quantitiesSold.length;
        double currentBalance, currentPrice;
        double[] currentShares = new double[n];

        try {
            //get the trader's current balance
            String queryString = "SELECT B.amount FROM BALANCE AS B WHERE B.accountId = ? AND B.timestamp = " + 
                                    "(SELECT MAX(B2.timestamp) FROM BALANCE AS B2 WHERE B2.accountId = ?)";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, accountId);
            getBalance.setInt(2, accountId);
            ResultSet resultSet = getBalance.executeQuery();
            currentBalance = resultSet.getDouble(1);
            getBalance.close();

            //get the current stock price
            queryString = "SELECT S.currentPrice FROM StarProfile AS S WHERE S.stockSymbol = ?";
            PreparedStatement getPrice = connection.prepareStatement(queryString);
            getPrice.setString(1, stockSymbol);
            resultSet = getPrice.executeQuery();
            currentPrice = resultSet.getDouble(1);
            getPrice.close();

            //get the number of shares the customer currently owns
            for (int i = 0; i < n; i++) {
                queryString = "SELECT S.quantity FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?";
                PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                getCurrentShares.setInt(1, accountId);
                getCurrentShares.setString(2, stockSymbol);
                getCurrentShares.setDouble(3, buyPrices[i]);
                resultSet = getCurrentShares.executeQuery();
                currentShares[i] = resultSet.getDouble(1);
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

            String buyPricesCSV = "";
            String quantitiesCSV = "";
            for (int i = 0; i < n; i++) {
                buyPricesCSV += buyPrices[i] + ",";
                quantitiesCSV += quantitiesSold[i] + ",";
            }

            PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
            updateBalance.setInt(1, accountId);
            updateBalance.setDouble(2, currentBalance + revenue);
            updateBalance.setTimestamp(3, currentTime);
            updateBalance.executeQuery();
            updateBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?)");
            updateMarketTransaction.setInt(1, accountId);
            updateMarketTransaction.setTimestamp(2, currentTime);
            updateMarketTransaction.setString(3, "S");
            updateMarketTransaction.setDouble(4, revenue);
            updateMarketTransaction.executeQuery();
            updateMarketTransaction.close();

            PreparedStatement updateStockTransaction = connection.prepareStatement("INSERT INTO StockTransaction VALUES (?, ?, ?, ?, ?, ?, ?)");
            updateStockTransaction.setInt(1, accountId);
            updateStockTransaction.setTimestamp(2, currentTime);
            updateStockTransaction.setString(3, "S");
            updateStockTransaction.setString(4, stockSymbol);
            updateStockTransaction.setString(5, buyPricesCSV);
            updateStockTransaction.setDouble(6, currentPrice);
            updateStockTransaction.setString(7, quantitiesCSV);
            updateStockTransaction.executeQuery();
            updateStockTransaction.close();

            for (int i = 0; i < n; i++) {
                if (currentShares[i] == quantitiesSold[i]) {
                    //we are selling all of this stock, so we delete the row
                    PreparedStatement updateStonk = connection.prepareStatement("DELETE FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                    updateStonk.setInt(1, accountId);
                    updateStonk.setString(2, stockSymbol);
                    updateStonk.setDouble(3, buyPrices[i]);
                    updateStonk.executeQuery();
                    updateStonk.close();
                } else {
                    //we are selling part of this stock, so we subtract from the quantity
                    PreparedStatement updateStonk = connection.prepareStatement("UPDATE Stonk SET quantity = ? WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?");
                    updateStonk.setDouble(1, currentShares[i] - quantitiesSold[i]);
                    updateStonk.setInt(2, accountId);
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
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        LocalDateTime currentTime = currentTimestamp.toLocalDateTime();

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

            //get the most recent buy/sell MarketTransaction
            queryString = "SELECT * FROM MarketTransaction AS M WHERE M.accountId = ? AND (M.transactionType = 'B' OR M.transactionType = 'S') AND M.timestamp = " + 
                                    "(SELECT MAX(M2.timestamp) FROM MarketTransaction AS M2 WHERE (M2.transactionType = 'B' OR M2.transactionType = 'S') AND M2.accountId = ?)";
            PreparedStatement getMarketTransaction = connection.prepareStatement(queryString);
            getMarketTransaction.setInt(1, accountId);
            getMarketTransaction.setInt(2, accountId);
            resultSet = getMarketTransaction.executeQuery();
            if (!resultSet.next()) {
                System.out.println("ERROR: Cancel failed. There are no transactions to cancel.");
                return;
            }
            Timestamp timestamp = resultSet.getTimestamp(2);
            LocalDateTime date = timestamp.toLocalDateTime();
            String transactionType = resultSet.getString(3);
            double amountTransferred = resultSet.getDouble(4);
            getMarketTransaction.close();

            //get the most recent buy/sell StockTransaction
            queryString = "SELECT * FROM StockTransaction AS S WHERE S.accountId = ? AND S.timestamp = ? AND S.transactionType = ?";
            PreparedStatement getStockTransaction = connection.prepareStatement(queryString);
            getStockTransaction.setInt(1, accountId);
            getStockTransaction.setTimestamp(2, timestamp);
            getStockTransaction.setString(2, transactionType);
            String stockSymbol = resultSet.getString(4);
            String buyPrices = resultSet.getString(5);
            double sellPrice = resultSet.getDouble(6);
            String quantities = resultSet.getString(7);
            resultSet = getStockTransaction.executeQuery();
            getStockTransaction.close();

            String[] buyPricesArr = buyPrices.split(",");
            String[] quantitiesArr = quantities.split(",");
            int n = buyPricesArr.length;
            double[] buyPricesD = new double[n];
            double[] quantitiesD = new double[n];

            for (int i = 0; i < n; i++) {
                buyPricesD[i] = Double.parseDouble(buyPricesArr[i]);
                quantitiesD[i] = Double.parseDouble(quantitiesArr[i]);
            }

            //check if we are canceling a transaction from the same day
            if (currentTime.getYear() == date.getYear() && currentTime.getMonthValue() == date.getMonthValue() && currentTime.getDayOfMonth() == date.getDayOfMonth()) {
                if (transactionType == "B") {
                    //cancel the last buy transaction
                    double payback = amountTransferred - 20;
                    if (currentBalance + payback < 0) {
                        System.out.println("ERROR: Cancel failed. Your account lacks sufficient funds.");
                        return;
                    }

                    PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
                    updateBalance.setInt(1, accountId);
                    updateBalance.setDouble(2, currentBalance + payback);
                    updateBalance.setTimestamp(3, currentTimestamp);
                    updateBalance.executeQuery();
                    updateBalance.close();

                    //delete the appropriate stocks from stonk
                    for (int i = 0; i < n; i++) {
                        queryString = "SELECT S.quantity FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?";
                        PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
                        getCurrentShares.setInt(1, accountId);
                        getCurrentShares.setString(2, stockSymbol);
                        getCurrentShares.setDouble(3, buyPricesD[i]);
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
                    
                } else if (transactionType == "S") {
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
        System.out.println("Welcome to the Stars 'R' Us Trader Interace!\nEnter 1 to login, or 2 to create an account.");
    }
}