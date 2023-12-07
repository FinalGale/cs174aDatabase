import java.sql.*;
import java.util.*;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.driver.parser.SqlEarley;

public class managerInterface {
    final static String DB_URL = "jdbc:oracle:thin:@projDB_tp?TNS_ADMIN=/Users/daniellu/Downloads/Wallet_projDB";
    final static String DB_USER = "ADMIN";
    final static String DB_PASSWORD = "Cookie12345+";

    public static void addInterest(Connection connection, double monthlyInterestRate) throws SQLException {

    }

    public static void genMonthlyStatement(Connection connection, String customerUsername) throws SQLException {

    }

    public static void listActiveCustomers(Connection connection) throws SQLException {
        try {
            String queryString = "SELECT U.username FROM UserProfile AS U GROUP BY U.username HAVING 1000 <= SUM (shares) FROM (SELECT quantity AS shares FROM StockTransaction WHERE stockAccountID IN (SELECT O.stockAccountID FROM OwnsAccount AS O WHERE O.username = U.username)";
            PreparedStatement getUsername = connection.prepareStatement(queryString);
            ResultSet resultSet = getUsername.executeQuery();
            while (resultSet.next()) {
                System.out.println("Active Customer Username: " + resultSet.getString(1));
            }
            getUsername.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: listActiveCustomers failed.");
            System.out.println(e);
        }
    }

    public static void genGovDTER(Connection connection) throws SQLException {
        try {
            String queryString = "SELECT DISTINCT M.balance, M.marketAccountID FROM MarketTransaction AS M, OwnsAccount AS O WHERE O.username = ? ORDER BY M.orderNumber DESC";
            PreparedStatement getAccountID = connection.prepareStatement(queryString);
            ResultSet resultSet = getAccountID.executeQuery();
            if (resultSet.next()) {
                System.out.println("Market Account ID: " + resultSet.getDouble("marketAccountID")
                        + "\nCurrent Balance: " + resultSet.getInt("balance"));
            }
            getAccountID.close();

            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE O.username = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            resultSet = getStockAccountID.executeQuery();
            while (resultSet.next()) {
                System.out.println("Stock Account ID: " + resultSet.getInt(1));
            }
            getStockAccountID.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: genGovDTER failed.");
            System.out.println(e);
        }
    }

    public static void genCustomerReport(Connection connection, String customerUsername) throws SQLException {
        try {
            String queryString = "SELECT DISTINCT M.balance, M.marketAccountID FROM MarketTransaction AS M, OwnsAccount AS O WHERE O.username = ? ORDER BY M.orderNumber DESC";
            PreparedStatement getAccountID = connection.prepareStatement(queryString);
            ResultSet resultSet = getAccountID.executeQuery();
            if (resultSet.next()) {
                System.out.println("Market Account ID: " + resultSet.getDouble("marketAccountID")
                        + "\nCurrent Balance: " + resultSet.getInt("balance"));
            }
            getAccountID.close();

            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE O.username = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            resultSet = getStockAccountID.executeQuery();
            while (resultSet.next()) {
                System.out.println("Stock Account ID: " + resultSet.getInt(1));
            }
            getStockAccountID.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: genCustomerReport failed.");
            System.out.println(e);
        }
    }

    public static void deleteTransactions(Connection connection) throws SQLException {

    }

    public static void openMarket(Connection connection, String newDate) throws SQLException {
        System.out.println("Opening market to date " + newDate);
        try {
            // change the date in date table
            PreparedStatement updateDate = connection.prepareStatement("UPDATE CurrDate SET date = ?");
            updateDate.setDate(1, java.sql.Date.valueOf(newDate));
            updateDate.executeQuery();
            updateDate.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("ERROR: Opening Market failed.");
            System.out.println(e);
        }
    }

    public static void closeMarket(Connection connection) throws SQLException {
        // don't allow trading if the market is closed
    }

    public static void setStockPrice(Connection connection) throws SQLException {

    }

    public static void main(String[] args) throws SQLException {
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
            Scanner input = new Scanner(System.in);
            System.out.println(
                    "Welcome to the Stars 'R' Us Trader Interace!\nEnter 1 to login, or 2 to create an account:");
            String username = "";
            String password = "";
            int marketAccountID = 0;

        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
