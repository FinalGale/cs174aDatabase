import java.sql.*;
import java.util.*;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.driver.parser.SqlEarley;

public class godModeInterface {
    final static String DB_URL = "jdbc:oracle:thin:@projDB_tp?TNS_ADMIN=/Users/daniellu/Downloads/Wallet_projDB";
    final static String DB_USER = "ADMIN";
    final static String DB_PASSWORD = "Cookie12345+";

    public static void openMarket(Connection connection, String newDate) throws SQLException {
        System.out.println("Opening market to date " + newDate);
        try {
            // change the date in date table
            PreparedStatement updateDate = connection.prepareStatement("UPDATE TimeInfo SET currentDate = ?");
            updateDate.setDate(1, java.sql.Date.valueOf(newDate));
            updateDate.executeQuery();
            updateDate.close();

        } catch (Exception e) {
            System.out.println("ERROR: Opening Market failed.");
            System.out.println(e);
        }
    }

    public static void closeMarket(Connection connection) throws SQLException {
        // select from Star Profile the StockSymbol and CurrentPrice
        // Update Closing Price table the stockSymbol, currDate, and currentPrice
        System.out.println("Closing market");
        try {
            PreparedStatement setClosingPrice = connection.prepareStatement("UPDATE ClosingPrice C SET C.dailyClosingPrice FROM StarProfile S JOIN TimeInfo T ON S.stockSymbol = C.stockSymbol WHERE S.stockSymbol = C.stockSymbol AND T.currentDate = C.priceDate");
            setClosingPrice.executeQuery();
            setClosingPrice.close();
        } catch (Exception e) {
            System.out.println("ERROR: Closing Market failed.");
            System.out.println(e);
        }
        System.out.println("Market has been closed");
    }

    public static void setStockPrice(Connection connection, String stockType, String stockSymbol, double new_value)
            throws SQLException {
        // search by stock symbol in star profile or signed contract table
        // then look at current price or total value and change it to new input value
        System.out.println("Setting new stock price");
        try {
            PreparedStatement updateStock = connection
                    .prepareStatement("UPDATE StarProfile SET currentPrice = ? WHERE stockSymbol = ?");
            updateStock.setDouble(1, new_value);
            updateStock.setString(2, stockSymbol);
            updateStock.executeQuery();
            updateStock.close();

            updateStock = connection.prepareStatement("UPDATE SignedContract SET totalValue = ? WHERE stockSymbol = ?");
            updateStock.setDouble(1, new_value);
            updateStock.setString(2, stockSymbol);
            updateStock.executeQuery();
            updateStock.close();
            
            System.out.println("Update to Stock Price completed");
        } catch (Exception e) {
            System.out.println("ERROR: setStockPrice failed.");
            System.out.println(e);
        }
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
                    "Welcome to the God Mode Interace!\nWhat would you like your world to be?");

        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
