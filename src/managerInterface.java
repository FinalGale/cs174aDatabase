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
        System.out.println("Preparing to add interest...");
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

            ArrayList<Integer> mid = new ArrayList<Integer>();
            queryString = "SELECT * FROM MarketAccount";
            Statement getMID = connection.createStatement();
            resultSet = getMID.executeQuery(queryString);
            while (resultSet.next()) {
                mid.add(resultSet.getInt(1));
            }

            for (int m : mid) {
                ArrayList<Integer> days = new ArrayList<Integer>();
                ArrayList<Double> balances = new ArrayList<Double>();

                queryString = "SELECT M.transactDate, MAX(M.orderNumber) FROM MarketTransaction M WHERE M.marketAccountID = ?"
                        +
                        " GROUP BY M.transactDate ORDER BY M.transactDate";
                PreparedStatement getON = connection.prepareStatement(queryString);
                getON.setInt(1, m);
                resultSet = getON.executeQuery();
                while (resultSet.next()) {
                    java.sql.Date curDate = resultSet.getDate(1);
                    int curON = resultSet.getInt(2);

                    String q = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? AND transactDate = ? AND orderNumber = ?";
                    PreparedStatement getBalances = connection.prepareStatement(q);
                    getBalances.setInt(1, m);
                    getBalances.setDate(2, curDate);
                    getBalances.setInt(3, curON);
                    ResultSet r = getBalances.executeQuery();
                    if (r.next()) {
                        balances.add(r.getDouble(1));
                    }
                    days.add(curDate.toLocalDate().getDayOfMonth());
                }

                // get the trader's current balance and order number
                queryString = "SELECT balance, orderNumber FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
                PreparedStatement getBalance = connection.prepareStatement(queryString);
                getBalance.setInt(1, m);
                resultSet = getBalance.executeQuery();
                double currentBalance = 0;
                int orderNumber = 0;
                if (resultSet.next()) {
                    currentBalance = resultSet.getDouble(1);
                    orderNumber = resultSet.getInt(2);
                }
                getBalance.close();
                days.add(currentDate.toLocalDate().getDayOfMonth() + 1);
                balances.add(currentBalance);

                double sum = 0;
                for (int i = 0; i < days.size() - 1; i++) {
                    sum += balances.get(i) * (days.get(i + 1) - days.get(i));
                }

                sum /= (days.get(days.size() - 1) - days.get(0));
                double accrueAmount = sum * monthlyInterestRate;

                PreparedStatement insertMT = connection
                        .prepareStatement("INSERT INTO MarketTransaction VALUES (?, ?, ?, ?, ?)");
                insertMT.setInt(1, m);
                insertMT.setDate(2, currentDate);
                insertMT.setInt(3, orderNumber + 1);
                insertMT.setString(4, "accrue-interest");
                insertMT.setDouble(5, currentBalance + accrueAmount);
                insertMT.executeQuery();
                insertMT.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: Deposit failed.");
            System.out.println(e);
        }
    }

    public static void genMonthlyStatement(Connection connection, String customerUsername) throws SQLException {
        try {
            String queryString = "SELECT name, emailAddress FROM UserProfile WHERE username = ?";
            PreparedStatement getPersonal = connection.prepareStatement(queryString);
            getPersonal.setString(1, customerUsername);
            ResultSet resultSet = getPersonal.executeQuery();
            String name = "";
            String email = "";
            double totalCommisions = 0;
            if (resultSet.next()) {
                name = resultSet.getString(1);
                email = resultSet.getString(2);
            }
            getPersonal.close();

            // get all StockTransactions
            queryString = "SELECT * FROM MarketTransaction WHERE marketAccountID IN (SELECT marketAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getMT = connection.prepareStatement(queryString);
            getMT.setString(1, customerUsername);
            resultSet = getMT.executeQuery();

            System.out.println("Customer name: " + name);
            System.out.println("Customer email: " + email);
            System.out.println("Transactions this month: ");
            while (resultSet.next()) {
                java.sql.Date date = resultSet.getDate(2);
                int orderNumber = resultSet.getInt(3);
                String transactionType = resultSet.getString(4);
                double balance = resultSet.getDouble(5);
                if (transactionType.equals("buy") || transactionType.equals("sell")) {
                    totalCommisions += 20;
                } else if (transactionType.equals("cancel")) {
                    totalCommisions += 40;
                }

                System.out.println(
                        "Date: " + date + ";Order number: " + orderNumber + "; Transaction Type: " + transactionType +
                                "; Balance: " + balance);
            }
            getMT.close();

            // get total earnings/losses
            double totalEarnings = 0;
            queryString = "SELECT quantity, sellPrice, buyPrice FROM StockTransaction WHERE stockAccountID IN (SELECT stockAccountID FROM OwnsAccount WHERE username = ?)";
            PreparedStatement getStockSold = connection.prepareStatement(queryString);
            getStockSold.setString(1, customerUsername);
            resultSet = getStockSold.executeQuery();
            while (resultSet.next()) {
                totalEarnings += resultSet.getDouble("quantity") * resultSet.getDouble("sellPrice")
                        - resultSet.getDouble("quantity") * resultSet.getDouble("buyPrice");
            }
            getStockSold.close();

            System.out.println("Total earnings/losses this month: " + totalEarnings + " USD");
            System.out.println("Total commisions paid this month: " + totalCommisions + " USD");
        } catch (Exception e) {
            System.out.println("ERROR: genMonthlyStatement failed.");
            System.out.println(e);
        }
    }

    public static void listActiveCustomers(Connection connection) throws SQLException {
        try {
            String queryString = "SELECT U.username FROM UserProfile U GROUP BY U.username" +
                    " HAVING 1000 <= (SELECT SUM(quantity) FROM StockTransaction WHERE stockAccountID IN (SELECT O.stockAccountID FROM OwnsAccount O WHERE O.username = U.username))";
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

            queryString = "SELECT U.username, S.quantity, S.sellPrice, S.buyPrice" +
                    " FROM StockTransaction S, UserProfile U" +
                    " WHERE S.transactionType = 'sell' AND S.stockAccountID IN" +
                    " (SELECT O.stockAccountID FROM OwnsAccount O WHERE O.username = U.username) GROUP BY U.username, S.quantity, S.sellPrice, S.buyPrice";
            PreparedStatement getStockSold = connection.prepareStatement(queryString);
            resultSet = getStockSold.executeQuery();
            while (resultSet.next()) {
                Double currentValue = counter.get(resultSet.getString("username"));
                counter.put(resultSet.getString("username"),
                        currentValue + ((resultSet.getDouble("quantity") * resultSet.getDouble("sellPrice")))
                                - (resultSet.getDouble("quantity") * resultSet.getDouble("buyPrice")));
            }
            getStockSold.close();

            System.out.println("genGovDTER list:");
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
            int marketAccountID = 0;
            String queryString = "SELECT marketAccountID FROM OwnsAccount WHERE username = ?";
            PreparedStatement getAccountID = connection.prepareStatement(queryString);
            getAccountID.setString(1, customerUsername);
            ResultSet resultSet = getAccountID.executeQuery();
            if (resultSet.next()) {
                marketAccountID = resultSet.getInt(1);
            }
            getAccountID.close();

            // get the trader's current balance
            queryString = "SELECT balance FROM MarketTransaction WHERE marketAccountID = ? ORDER BY transactDate DESC, orderNumber DESC";
            PreparedStatement getBalance = connection.prepareStatement(queryString);
            getBalance.setInt(1, marketAccountID);
            resultSet = getBalance.executeQuery();
            double currentBalance = 0;
            if (resultSet.next()) {
                currentBalance = resultSet.getDouble(1);
            }
            getBalance.close();

            System.out
                    .println("Market Account ID: " + marketAccountID + "\nCurrent Balance: " + currentBalance + " USD");

            queryString = "SELECT stockAccountID FROM OwnsAccount WHERE username = ?";
            PreparedStatement getStockAccountID = connection.prepareStatement(queryString);
            getStockAccountID.setString(1, customerUsername);
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

            queryString = "UPDATE MarketTransaction M SET M.transactDate = (SELECT T.currentDate FROM TimeInfo T), M.transactionType = 'newMonth'";
            PreparedStatement updateDate = connection.prepareStatement(queryString);
            updateDate.executeQuery();
            updateDate.close();
            System.out.println("Test");

            System.out.println("deleted transactions");
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
                System.out.println("(5) Generate Customer Report");
                System.out.println("(6) Delete Transactions");
                System.out.println("(7) Exit");
                int option = Integer.parseInt(input.readLine());

                switch (option) {
                    case 1:
                        System.out.print("Enter the monthly interest rate: ");
                        double monthIR = Double.parseDouble(input.readLine());
                        addInterest(connection, monthIR);
                        break;
                    case 2:
                        System.out.print("Enter customer username: ");
                        String customerUsername = input.readLine();
                        genMonthlyStatement(connection, customerUsername);
                        break;
                    case 3:
                        listActiveCustomers(connection);
                        break;
                    case 4:
                        genGovDTER(connection);
                        break;
                    case 5:
                        System.out.print("Enter customer username: ");
                        String custUser = input.readLine();
                        genCustomerReport(connection, custUser);
                        break;
                    case 6:
                        deleteTransactions(connection);
                        break;
                    case 7:
                        quit = true;
                        connection.close();
                        System.out.println("Thank you for using the Stars 'R' Us Manager Interface. Goodbye!");
                }
            }
        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
