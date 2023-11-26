import java.sql.*;
import java.time.LocalDateTime;

public class traderInterface {
    String currentUsername;
    int accountId;

    public void deposit(Connection connection, double depositAmount) throws SQLException {
        System.out.println("Preparing to Deposit...");
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
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
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
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
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
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
            updateStockTransaction.setDouble(5, currentPrice);
            updateStockTransaction.setDouble(6, currentPrice);
            updateStockTransaction.setDouble(7, quantity);
            updateStockTransaction.executeQuery();
            updateStockTransaction.close();

            PreparedStatement updateStonk = connection.prepareStatement("INSERT INTO Stonk VALUES (?, ?, ?, ?)");
            updateStonk.setInt(1, accountId);
            updateStonk.setString(2, stockSymbol);
            updateStonk.setDouble(4, currentPrice);
            updateStonk.setDouble(3, quantity);
            updateStonk.executeQuery();
            updateStonk.close();
            

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Buy failed.");
            System.out.println(e);
        }
    }

    public void sell(Connection connection, double quantity, String stockSymbol, double buyPrice) {
        System.out.println("Preparing to sell...");
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
        double currentBalance, currentPrice, currentShares;

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
            queryString = "SELECT S.quantity FROM Stonk AS S WHERE S.accountId = ? AND S.stockSymbol = ? AND S.buyPrice = ?";
            PreparedStatement getCurrentShares = connection.prepareStatement(queryString);
            getCurrentShares.setInt(1, accountId);
            getCurrentShares.setString(2, stockSymbol);
            getCurrentShares.setDouble(3, buyPrice);
            resultSet = getCurrentShares.executeQuery();
            currentShares = resultSet.getDouble(1);
            getCurrentShares.close();

            double revenue = quantity * currentPrice - 20;
            if (currentBalance + revenue < 0) {
                System.out.println("ERROR: Sell failed. Your account lacks sufficient funds.");
                return;
            } else if (currentShares < quantity) {
                System.out.println("ERROR: Sell failed. You cannot sell more shares than you own.");
                return;
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
            updateStockTransaction.setDouble(5, buyPrice);
            updateStockTransaction.setDouble(6, currentPrice);
            updateStockTransaction.setDouble(7, quantity);
            updateStockTransaction.executeQuery();
            updateStockTransaction.close();

            PreparedStatement updateStonk = connection.prepareStatement("INSERT INTO Stonk VALUES (?, ?, ?, ?)");
            updateStonk.setInt(1, accountId);
            updateStonk.setString(2, stockSymbol);
            updateStonk.setDouble(4, currentPrice);
            updateStonk.setDouble(3, quantity);
            updateStonk.executeQuery();
            updateStonk.close();
            

            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Sell failed.");
            System.out.println(e);
        }
    }

    public void cancel(Connection connection) {
        
    }

    public double showBalance() {
        return 0;
    }

    public String showTransactionHistory() {
        return "";
    }

    public String getCurStockPriceAndProfile(String stockSymbol) {
        return "";
    }

    public String listMovieInfo(String title, int productionYear) {
        return "";
    }

    public String[] getTopMovies(int startDate, int stopDate) {
        return null;
    }

    public String[] getReviews(String title, int productionYear) {
        return null;
    }

    public static void main(String args[]) {
        System.out.println("Welcome to the Stars 'R' Us Trader Interace!\nEnter 1 to login, or 2 to create an account.");
    }
}