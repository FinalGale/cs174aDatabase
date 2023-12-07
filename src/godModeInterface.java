import java.io.BufferedReader;
import java.io.InputStreamReader;
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

        java.sql.Date currentDate = new java.sql.Date(0);

        try {
            // get the current date
            String queryString = "SELECT currentDate FROM TimeInfo";
            PreparedStatement getCurrentDate = connection.prepareStatement(queryString);
            ResultSet resultSet = getCurrentDate.executeQuery();
            if (resultSet.next()) {
                currentDate = resultSet.getDate(1);
            }

            getCurrentDate.close();
            System.out.println("Closing market");
            // get the current stock price
            queryString = "SELECT stockSymbol, currentPrice FROM StarProfile";
            Statement getPrice = connection.createStatement();
            resultSet = getPrice.executeQuery(queryString);
            while (resultSet.next()) {
                String stockSymbol = resultSet.getString(1);
                double currentPrice = resultSet.getDouble(2);

                PreparedStatement setClosingPrice = connection
                        .prepareStatement("INSERT INTO ClosingPrice VALUES (?, ?, ?)");
                setClosingPrice.setString(1, stockSymbol);
                setClosingPrice.setDate(2, currentDate);
                setClosingPrice.setDouble(3, currentPrice);
                setClosingPrice.executeQuery();
                setClosingPrice.close();
            }
            getPrice.close();
        } catch (Exception e) {
            System.out.println("ERROR: Closing Market failed.");
            System.out.println(e);
        }
        System.out.println("Market has been closed");
    }

    public static void setStockPrice(Connection connection, String stockSymbol, double new_value)
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
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Welcome to the GOD Mode Interace!\nWhat would you like your world to be?");
            boolean quit = false;
            while (!quit) {
                System.out.println("Enter one of the following number options to execute a GOD command: ");
                System.out.println("(1) Open Market");
                System.out.println("(2) Close Market");
                System.out.println("(3) SetStockPrice");
                System.out.println("(4) Exit");
                int option = Integer.parseInt(input.readLine());

                switch (option) {
                    case 1:
                        System.out.print("Enter new date to open market on: ");
                        String newDate = input.readLine();
                        openMarket(connection, newDate);
                        break;
                    case 2:
                        closeMarket(connection);
                        break;
                    case 3:
                        System.out.print("Enter the symbol of the stock you want to change: ");
                        String stockSymbol = input.readLine();
                        System.out.print("Enter the new price value for the stock: ");
                        double newValue = Double.parseDouble(input.readLine());
                        setStockPrice(connection, stockSymbol, newValue);
                        break;
                    case 4:
                        quit = true;
                        connection.close();
                        System.out.println("Thank you GOD for creating the world! Amen 🙏");
                }
            }
            input.close();
        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
