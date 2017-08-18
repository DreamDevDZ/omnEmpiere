/******************************************************************************
 * Product: OmnEmpiere - sub-project of ADempiere                             *
 * Copyright (C) ALL GPL FOSS PROJECTS where taken                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/


package net.omnempiere.Magento2GetOrders;

import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;
	
import org.apache.commons.io.IOUtils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MyTransform {

    private static final transient Logger LOGGER = Logger.getLogger(MyTransform.class.getName());
    private boolean verbose = true;
    private String prefix = "MyTransform";
        

    Map<String, String> orderEntries = new HashMap<String, String>();
    List<String> xmlEntries = new ArrayList<String>();
	
    /** the arrays for products entries per order, orderEntries + 
     * a list of these values define the complete Order */
    List<String> productvalue = new ArrayList<String>();
    List<String> sku = new ArrayList<String>();
    List<String> qtyordered = new ArrayList<String>();
    List<String> priceactual = new ArrayList<String>();
    String shippingCosts;
    String documentno;

    
    /** get shipper ID from Shipper Name **/
	
    private String getShipperID(String shipperName) {
    	
    	List<String> shipperNameList;
    	List<String> shipperIDList;
    	Map<String, String> shipperMap = new HashMap<String, String>();
    	String result;
    	String OmnEmpiere_Home = System.getenv("OMNEMPIERE_HOME");
        String shipperconfigName="";
        String shipperconfigID="";
        
        InputStream propsInput;
        Properties prop = new Properties();
        try {
            propsInput = new FileInputStream(OmnEmpiere_Home+"/etc/magento2GetOrders.properties");
            // load a properties file
            prop.load(propsInput);
            shipperconfigName=prop.getProperty("shipperconfigName");
            shipperconfigID=prop.getProperty("shipperconfigID");
            
        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Read Properties failed: {0}", e.getMessage()); 
                            }
        
    	shipperNameList = Arrays.asList(shipperconfigName.split("\\s*,\\s*"));
    	shipperIDList = Arrays.asList(shipperconfigID.split("\\s*,\\s*"));
        
    	for (int i=0; i<shipperNameList.size(); i++) {
    	      shipperMap.put(shipperNameList.get(i), shipperIDList.get(i));    // is there a clearer way?
    	    }
    	
    	if (shipperMap.containsKey(shipperName)) {
    	result = shipperMap.get(shipperName); }
    	else { result = shipperIDList.get(0); }   // Use the default value in case no other selected
    	return result;
    }
    
        
    private String createSQLBP() {
            
            String sqlStatement;
            
            String sql = "INSERT INTO adempiere.i_bpartner (i_bpartner_id,ad_client_id,ad_org_id,created,value,name,address1,address2,city,postal,countrycode,phone,email,iscustomer) VALUES(";
            sql+="\'" + orderEntries.get("i_order_id") + "\',\'" + orderEntries.get("ad_client_id") + "\',\'" + orderEntries.get("ad_org_id")+ "\',\'" + orderEntries.get("created") + "\',\'" 
                      + orderEntries.get("bpartnervalue") + "\',\'" + orderEntries.get("name") + "\',\'" + orderEntries.get("address1")
                      + "\',\'" + orderEntries.get("address2") + "\',\'" + orderEntries.get("city") + "\',\'" + orderEntries.get("postal") + "\',\'" +
                        orderEntries.get("countrycode") + "\',\'" + orderEntries.get("phone") + "\',\'" + orderEntries.get("email") + "\',\'Y\')";
                    
            sqlStatement = "<sqlStatement>" + sql + "</sqlStatement>";

            return sqlStatement;
        }
        
    
    private String createSQL(int orderline) {
        /** create the SQL statement, one line per product oderline is the iterator for product list */
		String sqlStatement;

		/** create the sql command */

		String sql = "INSERT INTO adempiere.i_order (i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,name,address1,address2,city,postal,countrycode,phone,email,c_doctype_id,doctypename,taxamt,c_currency_id,m_shipper_id,c_ordersourcevalue,description,freightamt,productvalue,sku,qtyordered,priceactual) VALUES(";

		/** add the entries from the hashMap */
		for (String key : xmlEntries) {
			sql += "\'"+ orderEntries.get(key) + "\',";
		}
		/** and finally the product for each ordered product */
                sql += "\'" + documentno + "\',";
		sql += "\'" + shippingCosts + "\'," + "\'" + sku.get(orderline) + "\',"
				+ "\'" + sku.get(orderline) + "\',"  
				+ "\'" + qtyordered.get(orderline) + "\'," + "\'" + priceactual.get(orderline)
				+ "\')";

		/*
		 * manual
		 * i_order_id+","+ad_client_id+","+d_org_id+","+created,m_warehouse_id
		 * +","+bpartnervalue+","+address1+","address2+","+postal+","+city+","+
		 * countrycode
		 * +","+email+","+c_doctype_id+","+doctypename+","+productvalue
		 * [orderline
		 * ]+","+sku[orderline]+","+taxamt[orderline]+","+qtyordered[orederline
		 * ]+","+priceactual[orederline]+","+freightamt+")";
		 */

		/** format as XML */

		sqlStatement = "<sqlStatement>" + sql + "</sqlStatement>";

		return sqlStatement;

	}

    
    private void analyzeHTML(String html) {
        /** retrieve values from the HTML product list of Magento */
		LOGGER.log(Level.INFO, "This is the html source:{0}", html);

		Document doc = Jsoup.parse(html);

		Elements tableItems = doc.select("table[class=email-items]");
		Elements tableElements = tableItems.select("tbody");

            for (Element products : tableElements) {
                Element productName = products.select("p.product-name").get(0);
                
                LOGGER.log(Level.INFO, "The product Name: {0}", productName.text());
                
                productvalue.add(productName.text());
                
                Element productSKU = products.select("p.sku").get(0);
                sku.add(productSKU.text().replace("SKU: ", ""));
                
                Element productQuantity = products.select(
                        "td.item-qty").get(0);
                qtyordered.add(productQuantity.text());
                
                Element productPriceList = products.select("td.item-price").get(0);
                Elements productPrices = productPriceList.select("span.price");
                Element productPrice = productPrices.first();
                String productPrice_tmp = productPrice.text().replaceAll("[^\\.,0123456789]","");
                /** For international number formats with , instead of . **/
                Double price=Double.parseDouble(productPrice_tmp.replaceAll(",","."))/Double.parseDouble(productQuantity.text());
                /** Magento give price * quantity, therefore price per product is price/quantity **/
                priceactual.add(Double.toString(price));
            }

		Elements tableShipCost = doc.select("tr.shipping");
		Element orderShipCost = tableShipCost.select("span.price").get(0);
                String orderShipCost_tmp = orderShipCost.text().replaceAll("[^\\.,0123456789]", "");
                /** For international number formats with , instead of . **/
		shippingCosts = orderShipCost_tmp.replaceAll(",", ".");
                productvalue.add("Delivery");
                sku.add("delivery");
                qtyordered.add("1");
                priceactual.add(shippingCosts);
	}

    @SuppressWarnings("empty-statement")
    public String transform(Object body) {
            
        List<String> emailInputList;
        String orderValues = "i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,name,address1,address2,city,postal,countrycode,phone,email,c_doctype_id,doctypename,taxamt,c_currency_id,m_shipper_id,c_ordersourcevalue";    
        String emailInputValues = "i_order_id,created,bpartnervalue,name,address1,address2,city,postal,countrycode,phone,email,taxamt,m_shipper_id";
        String OMNE_DATE_FORMAT = "yyyy-MM-dd hh:mm";
        String answer = "";
        String OmnEmpiere_Home = System.getenv("OMNEMPIERE_HOME");
        String str = body.toString();
                
        /** LOGGER.log(Level.INFO,"The complete mail: {0}",body); **/
                
        String inputXML_raw = str.substring(str.indexOf("<!--[") + 5, str.indexOf("]-->"));
            /*** the following line re-formates the address string from Magento2 */
        String inputXML = inputXML_raw.replaceAll("<br/>",",").replaceAll("<br />",",");
        LOGGER.log(Level.INFO,"The input file: {0}",inputXML);
                
            /* Reset global Variables as beans are not always new objects in servicemix */
        productvalue.clear();
        sku.clear();
        qtyordered.clear();
        priceactual.clear();        

        
         /* Transform the string orderValues into a list */
         emailInputList = Arrays.asList(emailInputValues.split("\\s*,\\s*"));
         xmlEntries = Arrays.asList(orderValues.split("\\s*,\\s*"));
                
         Document input = Jsoup.parse(inputXML);
         InputStream propsInput;
         Properties prop = new Properties();
            /*
             * go through the list xmlEntries, identify the Nodes with name
             * xmlEntry[i] and get it's value
             */
        for (String key : xmlEntries) {
                
                /** if it is in the email list, get the property from email **/
                if(emailInputList.contains(key))  {
                    Element xmlValue = input.select(key).get(0);
                    String xmlString = xmlValue.text();
                    orderEntries.put(key, xmlString.replaceAll("'","''"));
                }
                else {
                    /** if not, try to find it in the properties file **/
                    try {
                        propsInput = new FileInputStream(OmnEmpiere_Home+"/etc/ebayGetOrders.properties");
                        // load a properties file
                        prop.load(propsInput);
                        orderEntries.put(key, prop.getProperty(key));               
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Read Properties failed: {0}", e.getMessage()); 
                            }
                }    
            }

		/** get the date format right for sql **/
		
		
            /* For Magento2: Use current date instead of Magento Order Date */
            
            SimpleDateFormat sdf = new SimpleDateFormat(OMNE_DATE_FORMAT);
            Date dateNow = new Date();
	    orderEntries.put("created",sdf.format(dateNow));                
                
            /*create Java list and remove empty entries */
            List<String> addressEntries = Arrays.asList(orderEntries.get("address1").split("\\s*,\\s*"));
            while(addressEntries.remove(null));
            
            /** Set correct customer details **/
            String streetString=addressEntries.get(1);
            orderEntries.put("address1", streetString.replaceAll("'", "''")); /** Second entry, first is name **/
            if(!(addressEntries.get(2).equals(orderEntries.get("city")))) {
                String addressString2=addressEntries.get(2);
		orderEntries.put("address2", addressString2.replaceAll("'", "''"));
            }
            /** remove leading T: from phone string in Magento **/
            orderEntries.put("phone",orderEntries.get("phone").replaceAll("T: ",""));
            
            /** replace shipper name with shipper id **/
            String shipperID = getShipperID(orderEntries.get("m_shipper_id"));
            orderEntries.put("m_shipper_id",shipperID);
		
		
            /** analyze the HTML entry from Magento and write data to the arrays **/
            analyzeHTML(inputXML);
                
            /* create Bp import entry */
            answer += createSQLBP();
                
            /**
            * go through the list of products and create one entry in the DB per
            * product
		 */  
            
            LOGGER.log(Level.INFO, "The DocumentNumber: {0}",orderEntries.get("i_order_id"));
            
            documentno=orderEntries.get("i_order_id");
		for (int i = 0; i < sku.size(); i++) {
                    /* create individual i_order_id */
                    String tmp = documentno;
                    String tmpshort = tmp.substring(tmp.length() - 6);
                    String uniqueOrderID = "1" + tmpshort + "0" + Integer.toString(i+1);
                    orderEntries.put("i_order_id", uniqueOrderID);
                    /** and add to answer */
                    answer += createSQL(i);
		}

		return answer;
	}

    public void addToDB(Object body) {
        /** add the complete SQL statement to the db, because of an 
        * issues with the OSGI PostgreSQL driver and ServiceMix (resource conflict), use an external script
        ***/
	try {
		Process p = new ProcessBuilder("/opt/omnEmpiere/scripts/addOrderToDB.sh").start();
		p.waitFor();
		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		if(error.ready()) {	
                         LOGGER.log(Level.SEVERE,"Error during DB script {0}",IOUtils.toString(error));
                }
		LOGGER.log(Level.INFO,"the result of the SQL statement {0}",IOUtils.toString(output));
		    
		    
            } catch (IOException e) {
		LOGGER.log(Level.SEVERE, "Can not call DB script {0}", e.getMessage());
            } catch (InterruptedException e) { 
                LOGGER.log(Level.SEVERE, "Can not call DB script {0}", e.getMessage());
            } 
		
	}

    public boolean isVerbose() {
		return verbose;
	}

    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

    public String getPrefix() {
		return prefix;
	}

    public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

}
