/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author oschmitt
 */

public class CreateAttributeValues {

   private final ArrayList<String> categoryList; 
   private final ArrayList<String> attributeIDList;
    
  
   static String m_attributeset_id = "1000000";
   static String ad_client_id = "1000000";
   static String eShopChannelID= "1000000";
   static String dbConnectionString = "jdbc:postgresql://localhost:5432/idempiere";
   static String dbUserName = "adempiere";
   static String dbPwd = "f0und1nCastion";
   static int AttributeSetInstanceID_Offset=131;  /* the first available AttributeSetInstance_ID without leading 1xx */
    
    public CreateAttributeValues() {
        this.categoryList = new ArrayList();
        this.attributeIDList = new ArrayList();
    }
    
    private void createCategoryList(Connection con) {
     
     ResultSet rs;   
     try {   
            String getAttributeInstanceIDs = "SELECT name FROM adempiere.m_product_category WHERE ad_client_id="+ad_client_id;
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(getAttributeInstanceIDs);
            if(!rs.next()) {
                return;
            }
            while (rs.next()) {
                categoryList.add(rs.getString("name").replaceAll("'", "''"));
               }  
            stmt.close(); 
          } catch (SQLException ex) {
           Logger.getLogger(CreateAttributeValues.class.getName()).log(Level.SEVERE,"Error in createCatgeoryList",ex);
                }    
            
    }
    
    private void createAttributeIDList(Connection con) {
        
           
     ResultSet rs; 
     
     try {   
            String getAttributeInstanceIDs = "SELECT m_attribute_id FROM adempiere.m_attributeuse WHERE m_attributeset_id="+m_attributeset_id;
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(getAttributeInstanceIDs);
            while (rs.next()) {
                attributeIDList.add(rs.getString("m_attribute_id"));  
               }   
            stmt.close();           
          } catch (SQLException ex) {
           Logger.getLogger(CreateAttributeValues.class.getName()).log(Level.SEVERE,"Error in CreateAttributeList",ex);
                }
        
    }
    
    
    private void createAttributes(Connection con) {
        
        
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String today = df.format(Calendar.getInstance().getTime());  
        String attributeValue;
        int uniqueSetInstanceID=AttributeSetInstanceID_Offset;
        
        
        /* Iterate through CategoryList */
        for (String categoryName : categoryList) {
            
            /** create one entry in  m_attributesetinstance
             * (m_attributesetinstance_id,ad_client_id(1000000),ad_org_id(0),isactive ("Y"),created (time), 
             * createdby(100), m_attributeset_id(1000000), description ("Category_Name") )
             **/
            String uniqueSetInstanceID_Formatted = "1" + String.format("%06d", uniqueSetInstanceID);
            try {  
                
                String addAttributeSetInstance = "INSERT INTO adempiere.m_attributesetinstance (m_attributesetinstance_id,ad_client_id,ad_org_id,isactive,created,createdby,updated,updatedby,m_attributeset_id,description) "
                        + "VALUES('"+uniqueSetInstanceID_Formatted+"','"+ad_client_id+"','0','Y','"+today+"','100','"+today+"','100','"+m_attributeset_id+"','"+categoryName+"')";
                /* Debug */
                System.out.println(addAttributeSetInstance);
                Statement stmt = con.createStatement();
                int rows = stmt.executeUpdate(addAttributeSetInstance);
                stmt.close();
                con.commit();        
            } catch (SQLException ex) {
            Logger.getLogger(CreateAttributeValues.class.getName()).log(Level.SEVERE,"Error in Insert to setinstance",ex);
                }
            /** create one entries in m_attributeInstance per attribute in attributeSet**/
                 
            for(String attributeID : attributeIDList) {
                
                /* Set the attribute value only for the eShopChannel Attribute, leave the others empty string */
                
                if(eShopChannelID.equals(attributeID)) {
                           attributeValue=categoryName; }
                        else {
                                attributeValue=""; }
                
                try {  
                        String addAttributeInstance = "INSERT INTO adempiere.m_attributeinstance (m_attributesetinstance_id,m_attribute_id,ad_client_id,ad_org_id,isactive,created,createdby,updated,updatedby,value) "
                        + "VALUES('"+uniqueSetInstanceID_Formatted+"','"+attributeID+"','"+ad_client_id+"','0','Y','"+today+"','100','"+today+"','100','"+attributeValue+"')";
                        /* Debug */
                        System.out.println(addAttributeInstance);
                        Statement stmt1 = con.createStatement();
                        int rows = stmt1.executeUpdate(addAttributeInstance);
                        stmt1.close();
                        con.commit();        
                    } catch (SQLException ex) {
                        Logger.getLogger(CreateAttributeValues.class.getName()).log(Level.SEVERE,"Error in Insert to instance",ex);
                        }
                }
            uniqueSetInstanceID++;    
        }
    }    
        
    public static void main(String[] args) {
        
       Connection connect;
       CreateAttributeValues me = new CreateAttributeValues();
       
       try {
         connect = DriverManager.getConnection(dbConnectionString, dbUserName, dbPwd);     
         connect.setAutoCommit(false);
         
         System.out.println("Opened database successfully");  /* Debug */
         
         /*create list of categories */
         
         me.createCategoryList(connect);

         System.out.println(me.categoryList);  /* Debug: dump the table  */
         
         /*create list of Attributes in AttributeSet */
         
         me.createAttributeIDList(connect);
         
         /*finally write the new Attributes to the DB */
         
         me.createAttributes(connect); 
         connect.commit();  

         } catch (Exception e) {
              Logger.getLogger(CreateAttributeValues.class.getName()).log(Level.SEVERE, null, e);
              System.exit(0);
                    }
         }

}

