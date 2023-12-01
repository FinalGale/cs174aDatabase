import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class managerInterface {
    String currentManagerid;
    Connection connection;

    public void addInterest(double monthlyInterestRate) {

    }

    public String genMonthlyStatement(int curCustomer) {
        return "";
    }

    public String listActiveCustomers() {
        return "";
    }

    public String[] genGovDTER() {
        return null;
    }

    public int[][] genCustomerReport(int curCustomer) {
        return null;
    }

    public void delete_transactions() {

    }

    public void openMarket(String newDate) {
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

    public void closeMarket() {
        // don't allow trading if the market is closed
    }

    public void setStockPrice() {

    }

}
