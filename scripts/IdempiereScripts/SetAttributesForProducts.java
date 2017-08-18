import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetAttributesForProducts {

   private final HashMap<String, Long>  attributeSetInstanceTable; 
/* Global Variables to set  */
   static String m_attribute_id_CategoryName="1000000";
   static String m_attributeset_id = "1000000";
   static String ad_client_id = "1000000";
   static String dbConnectionString = "jdbc:postgresql://localhost:5432/idempiere";
   static String dbUserName = "adempiere";
   static String dbPwd = "f0und1nCastion";

    public SetAttributesForProducts() {
        this.attributeSetInstanceTable = new HashMap<>();
    }
 
   
   /* Cretae a HashTable with CategoryNames and AttributeSetInstanceIds */
 
   private void createAttributeSetInstanceTable(Connection con) {
     
       try {
           /* get a list of AttributeSetInstances */
           
           String getAttributeInstanceIDs = "SELECT m_attributesetinstance_id FROM adempiere.m_attributesetinstance WHERE ad_client_id="+ad_client_id;
           ResultSet rs;
           Statement stmt = con.createStatement();
           rs = stmt.executeQuery(getAttributeInstanceIDs);
           
           
           /* Iterate the list */
           
           while (rs.next()) {
               long attributeSetInstanceId = rs.getLong("m_attributesetinstance_id");
               /*find the Category name for this attributeSetInstance */
               String getCategoryName = "SELECT value FROM adempiere.m_attributeinstance WHERE m_attributesetinstance_id="+Long.toString(attributeSetInstanceId)+"AND m_attribute_id ="+m_attribute_id_CategoryName;
               Statement stmt1 = con.createStatement();
               ResultSet rs1 = stmt1.executeQuery(getCategoryName);
               rs1.next();
               /*and write both values in the HashTable */
               attributeSetInstanceTable.put(rs1.getString("value"),attributeSetInstanceId);
           }
       } catch (SQLException ex) {
           Logger.getLogger(SetAttributesForProducts.class.getName()).log(Level.SEVERE, null, ex);
       }
        }

   private void setProductAttributes(Connection con) {
           
       
       String getProductsID;
       String getCategoryName;
       String categoryName;
       String addAttributesProducts;
       
       try {
           /* get list of product ids */
           
           
           getProductsID = "SELECT m_product_id FROM adempiere.m_product WHERE ad_client_id="+ad_client_id;
           
           Statement stmt = con.createStatement();
           ResultSet rs = stmt.executeQuery(getProductsID);
           /*go through product list */
           while (rs.next()) {
               /* get the catgeory name of the product_id via InnerJoin */
               long productID = rs.getLong("m_product_id");
               
               getCategoryName = "SELECT C.name FROM adempiere.m_product P INNER JOIN adempiere.m_product_category C ON P.m_product_category_id = C.m_product_category_id WHERE m_product_id = "+Long.toString(productID);
               Statement stmt1 = con.createStatement();
               ResultSet rs1 = stmt1.executeQuery(getCategoryName);
               rs1.next();
               /*Update attributeset_id and attributesetinstance_id for the Product */
               categoryName = rs1.getString("name");
               if(attributeSetInstanceTable.get(categoryName)!= null) {
               addAttributesProducts = "UPDATE adempiere.m_product SET m_attributeset_id="+m_attributeset_id+",m_attributesetinstance_id="+Long.toString(attributeSetInstanceTable.get(categoryName))+" WHERE m_product_id = "+Long.toString(productID);
               Statement stmt2 = con.createStatement();
               int rows = stmt2.executeUpdate(addAttributesProducts);
               con.commit();
                }
           }
           /*all done */
       } catch (SQLException ex) {
           Logger.getLogger(SetAttributesForProducts.class.getName()).log(Level.SEVERE, null, ex);
       }
      }
   
   public static void main(String[] args) {
        
       Connection connect;
       SetAttributesForProducts me = new SetAttributesForProducts();
       
       try {
         connect = DriverManager.getConnection(dbConnectionString, dbUserName, dbPwd);     
         connect.setAutoCommit(false);
         
         System.out.println("Opened database successfully");  /* Debug */

         me.createAttributeSetInstanceTable(connect);

         System.out.println(me.attributeSetInstanceTable);  /* Debug: dump the table  */
         
         me.setProductAttributes(connect);   

         } catch (Exception e) {
              Logger.getLogger(SetAttributesForProducts.class.getName()).log(Level.SEVERE, null, e);
              System.exit(0);
                    }
         }

}
