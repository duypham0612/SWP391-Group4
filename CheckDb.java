import java.sql.*;
public class CheckDb {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:sqlserver://localhost:1433;databaseName=CafeChain;encrypt=false;trustServerCertificate=true";
    String user = "sa";
    String pass = "YourPassword123";
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      System.out.println("CONNECTED");
      try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT DB_NAME() AS db, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.TABLES")) {
        while (rs.next()) { System.out.println(rs.getString(1) + " | " + rs.getInt(2)); }
      }
    }
  }
}
