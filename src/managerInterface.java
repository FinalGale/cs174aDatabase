import java.sql.Connection;
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

    public void openMarket() {

    }

    public void closeMarket() {
        // don't allow trading if the market is closed
    }

    public void setStockPrice() {

    }

    public void setDate() {

    }
}
