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

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO Transaction VALUES (?, ?, ?, ?)");
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
                System.out.println("ERROR: Withdraw failed. Your account lacks sufficient funds to withdraw this amount of money.");
                return;
            }

            PreparedStatement updateBalance = connection.prepareStatement("INSERT INTO Balance VALUES (?, ?, ?)");
            updateBalance.setInt(1, accountId);
            updateBalance.setDouble(2, currentBalance - withdrawAmount);
            updateBalance.setTimestamp(3, currentTime);
            updateBalance.executeQuery();
            updateBalance.close();

            PreparedStatement updateMarketTransaction = connection.prepareStatement("INSERT INTO Transaction VALUES (?, ?, ?, ?)");
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

    public void accrue_interest(double monthlyInterestRate) {

    }

    public void buy(double amount) {

    }

    public void sell(double amount) {

    }

    public void cancel() {

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

    public String list_movie_info(String title, int productionYear) {
        return "";
    }

    public String[] get_top_movies(int startDate, int stopDate) {
        return null;
    }

    public String[] get_reviews(String title, int productionYear) {
        return null;
    }

    public static void main(String args[]) {
        System.out.println("Welcome to the Stars 'R' Us Trader Interace!");
    }
}