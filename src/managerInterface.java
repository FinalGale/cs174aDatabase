import java.io.BufferedReader;
import java.io.InputStreamReader;
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
        } catch (Exception e) {
            System.out.println("ERROR: listActiveCustomers failed.");
            System.out.println(e);
        }
    }

    public static void genGovDTER(Connection connection) throws SQLException {
        try {
            Map<String, Double> counter = new HashMap<>();

            String queryString = "SELECT username FROM UserProfile";
            PreparedStatement getUsername = connection.prepareStatement(queryString);
            ResultSet resultSet = getUsername.executeQuery();
            while (resultSet.next()) {
                counter.put(resultSet.getString(1), 0.0);
            }
            getUsername.close();

            queryString = "SELECT S.quantity, S.sellPrice, S.buyPrice FROM StockTransaction AS S, UserProfile AS U WHERE S.transactionType = 'sell' AND S.stockAccountID IN (SELECT O.stockAccountID FROM OwnsAccount AS O WHERE O.username = U.username) GROUP BY U.username, S.quantity, S.sellPrice, S.buyPrice";
            PreparedStatement getStockSold = connection.prepareStatement(queryString);
            resultSet = getStockSold.executeQuery();
            while (resultSet.next()) {
                Double currentValue = counter.get(resultSet.getString("username"));
                counter.put(resultSet.getString("username"),
                        currentValue + ((resultSet.getDouble("quantity") * resultSet.getDouble("sellPrice")))
                                - (resultSet.getDouble("quantity") * resultSet.getDouble("buyPrice")));
            }
            getStockSold.close();

            System.out.println("genGovDTER list: \n");
            for (Map.Entry<String, Double> entry : counter.entrySet()) {
                // got to add interest here later
                if (entry.getValue() > 10000) {
                    System.out.println(entry.getKey());
                }
            }
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
         try {
            String queryString = "DELETE FROM StockTransaction";
            PreparedStatement deleteST = connection.prepareStatement(queryString);
            deleteST.executeQuery();
            deleteST.close();

            queryString = "DELETE FROM MarketTransaction M WHERE M.orderNumber < (SELECT MAX (M2.ordernumber) FROM MarketTransaction M2 WHERE M2.marketAccountID = M.marketAccountID)";
            PreparedStatement deleteMT = connection.prepareStatement(queryString);
            deleteMT.executeQuery();
            deleteMT.close();
            connection.close();
            queryString = "UPDATE MarketPrice M SET M.transactDate = (SELECT T.currentDate FROM TimeInfo T)";
            PreparedStatement updateDate = connection.prepareStatement(queryString);
            updateDate.executeQuery();
            updateDate.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("ERROR: deleteTransactions failed.");
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
                    "Welcome to the Stars 'R' Us Manager Interace!\nEnter 1 to login, or 2 to create an account:");
            String username = "";
            String password = "";

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
                System.out.println("Account created successfully!");
            }

            boolean quit = false;
            while (!quit) {
                System.out.println("Enter one of the following number options to execute a command: ");
                System.out.println("(1) Add interest to all accounts");
                System.out.println("(2) Generate monthly statement for a customer");
                System.out.println("(3) List Active Customers");
                System.out.println("(4) Generate Government Drug & Tax Evasion Report");
                System.out.println("(5) ");
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
                        System.out.println("Thank you for using the Stars 'R' Us Trader Interface. Goodbye!");
                }
            }
            input.close();
        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
