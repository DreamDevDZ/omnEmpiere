import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetProductCosts {

   
/* Global Variables to set  */
   
   static String ad_client_id = "1000000";
   static String m_pricelistversion_id = "1000007";
   static String dbConnectionString = "jdbc:postgresql://db.bravostore.it:5432/idempiere";
   static String dbUserName = "adempiere";
   static String dbPwd = "f0und1nCastion";

    public SetProductCosts() {
        
    }
 
   
   /* Cretae a HashTable with CategoryNames and AttributeSetInstanceIds */
 
  

   private void setProductCosts(Connection con) {
           
       
       String getProductsID;
       String getLimitPrice;
       String limitPrice;
       String addLimitPriceToCosts;
       
       try {
           /* get list of product ids */
           
           getProductsID = "SELECT m_product_id FROM adempiere.m_product WHERE ad_client_id="+ad_client_id;
           
           Statement stmt = con.createStatement();
           ResultSet rs = stmt.executeQuery(getProductsID);
           /*go through product list */
           while (rs.next()) {
               /* get the catgeory name of the product_id via InnerJoin */
               long productID = rs.getLong("m_product_id");
               
               getLimitPrice = "SELECT pricelimit FROM adempiere.m_productprice WHERE m_product_id = "+Long.toString(productID)+"AND m_pricelist_version_id = "+m_pricelistversion_id;
               Statement stmt1 = con.createStatement();
               ResultSet rs1 = stmt1.executeQuery(getLimitPrice);
               if(rs1.next()) {
                /*Update attributeset_id and attributesetinstance_id for the Product */
                limitPrice = rs1.getString("pricelimit");
                if (limitPrice!= null) {
                    addLimitPriceToCosts = "UPDATE adempiere.m_cost SET currentcostprice="+limitPrice+",iscostfrozen = 'Y' WHERE m_product_id = "+Long.toString(productID);
                    System.out.println("ProductID "+productID +" SQL command " +addLimitPriceToCosts);
                    Statement stmt2 = con.createStatement();
                    int rows = stmt2.executeUpdate(addLimitPriceToCosts);                
                    con.commit();
                    System.out.println(Integer.toString(rows) +"lines updated ");
                    }
               }
           }
           /*all done */
       } catch (SQLException ex) {
           Logger.getLogger(SetProductCosts.class.getName()).log(Level.SEVERE, null, ex);
       }
      }
   
   public static void main(String[] args) {
        
       Connection connect;
       SetProductCosts me = new SetProductCosts();
       
       try {
         connect = DriverManager.getConnection(dbConnectionString, dbUserName, dbPwd);     
         connect.setAutoCommit(false);
         
         System.out.println("Opened database successfully");  /* Debug */

         
         me.setProductCosts(connect);   

         } catch (SQLException e) {
              Logger.getLogger(SetProductCosts.class.getName()).log(Level.SEVERE, null, e);
              System.exit(0);
                    }
         }

}
