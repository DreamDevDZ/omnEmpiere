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

package net.omnempiere.EbayGetOrders;

import java.util.logging.Logger;
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
import java.io.StringReader;
import java.util.Properties;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;


import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ReadPost implements Processor {
    
    Map<String, String> orderEntries = new HashMap<String, String>();
    
            /** the arrays for products entries per order */
    List<String> sku = new ArrayList<String>();
    List<String> qtyordered = new ArrayList<String>();
    List<String> priceactual = new ArrayList<String>();



    private static final transient Logger LOGGER = Logger.getLogger(ReadPost.class.getName());
    private boolean verbose = true;
    private String prefix = "ReadPost";

	/**
	 * ebayInput is the list of XML field in the eBay order XML
	 * orderValues represents all DB entries for i_order in IdEmpiere 
	 * Transform reads the values of ebayInput from Body and put it into orderEntry with index orderValues
	 * the right order  of the list values is crucial!!!
	 */    
	
    private String createSQLBP() {
            
            String sqlStatement;
            
            String sql = "INSERT INTO adempiere.i_bpartner (i_bpartner_id,ad_client_id,ad_org_id,created,value,name,address1,address2,city,postal,countrycode,phone,email,iscustomer) VALUES(";
            sql+="\'" + orderEntries.get("i_bpartner_id") + "\',\'" + orderEntries.get("ad_client_id") + "\',\'" + orderEntries.get("ad_org_id")+ "\',\'" + orderEntries.get("created") + "\',\'" 
                      + orderEntries.get("bpartnervalue") + "\',\'" + orderEntries.get("name") + "\',\'" + orderEntries.get("address1")
                      + "\',\'" + orderEntries.get("address2") + "\',\'" + orderEntries.get("city") + "\',\'" + orderEntries.get("postal") + "\',\'" +
                        orderEntries.get("country") + "\',\'" + orderEntries.get("phone") + "\',\'" + orderEntries.get("email") + "\',\'Y\')";
                    
            sqlStatement = "<sqlStatement>" + sql + "</sqlStatement>";

            return sqlStatement;
        }
    private String createSQL(int orderline) {

	String sqlStatement;

	/** create the SQL command */
       
	String sql = "INSERT INTO adempiere.i_order (i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,name,address1,address2,city,postal,countrycode,email,c_doctype_id,doctypename,c_currency_id,m_shipper_id,c_ordersourcevalue,description,sku,qtyordered,priceactual)  VALUES(";
        String keyValues = "i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,name,address1,address2,city,postal,country,email,c_doctype_id,doctypename,c_currency_id,m_shipper_id,c_ordersourcevalue,documentno";
        List<String> keyValuesList;
        keyValuesList = Arrays.asList(keyValues.split("\\s*,\\s*"));
        /** add the entries from the hashMap */
	for (String key : keyValuesList) {
            LOGGER.log(Level.INFO, "the key: {0}", key);
            sql += "\'"+ orderEntries.get(key) + "\',";
	}
	/** and finally the product for each ordered product */
	sql +=   "\'" + sku.get(orderline) + "\'," + "\'" + qtyordered.get(orderline) + "\'," + "\'" + priceactual.get(orderline)+ "\')";

		/*
		 * manual
		 * i_order_id+","+ad_client_id+","+d_org_id+","+created,m_warehouse_id
		 * +","+bpartnervalue+","+address1+","address2+","+postal+","+city+","+
		 * countrycode
		 * +","+email+","+c_doctype_id+","+doctypename+",
		 * "+sku[orderline]+","+taxamt[orderline]+","+qtyordered[orederline
		 * ]+","+priceactual[orederline]+","+freightamt+")";
		 */

		/** format as XML */

	sqlStatement = "<sqlStatement>" + sql + "</sqlStatement>";

	return sqlStatement;

	}
	
    private String transform(String body) {
        
        Document input; 
        InputStream propsInput;
        Properties prop = new Properties();
                    
        List<String> shipperNameList;
        List<String> shipperIDList;
        List<String> shipperCostList;
        Map<String, String> shipperMap = new HashMap<String, String>();
        Map<String, String> shipperCostMap = new HashMap<String, String>();
        
        String answer = "";        
        
        String OmnEmpiere_Home = System.getenv("OMNEMPIERE_HOME");
    
        String shippingID;
        String shippingCost;
        String shipperName;
  
        String ad_client_id = ""; /* the client id in IdEmpiere */
        String ad_org_id= ""; /* the organization id */
        String m_warehouse_id=""; /* warehouse id */
        String m_pricelist_id="";
        String c_doctype_id=""; /*document type id according to c_doctype */
        String doctypename=""; /*kind of order for credit card pre pay order */
        String c_currency_id=""; /* currency code c_currency_id see c_currency table in idEmpiere */
        String c_ordersourcevalue="";
            /** List of configured Shippers in MAGENTO and IdEmpiere, for shipper_id, check table m_shipper_id in db **/
        String shipperconfigName = "";
        String shipperconfigID = "";
        String shipperCost = "";
        String shipperFreeOrder = "";
        
        
        try {
            propsInput = new FileInputStream(OmnEmpiere_Home+"/etc/ebayGetOrders.properties");
            // load a properties file
            prop.load(propsInput);
            ad_client_id=prop.getProperty("ad_client_id");
            ad_org_id=prop.getProperty("ad_org_id");
            m_warehouse_id=prop.getProperty("m_warehouse_id");
            m_pricelist_id=prop.getProperty("m_pricelist_id");
            c_doctype_id=prop.getProperty("c_doctype_id");
            doctypename=prop.getProperty("doctypename");
            c_currency_id=prop.getProperty("c_currency_id");
            c_ordersourcevalue=prop.getProperty("c_ordersourcevalue");
            shipperconfigName=prop.getProperty("shipperconfigName");
            shipperconfigID=prop.getProperty("shipperconfigID");
            shipperCost=prop.getProperty("shipperCost");
            shipperFreeOrder=prop.getProperty("shipperFreeOrder");
        } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Read Properties failed: {0}", e.getMessage()); 
            }   

        /* Reset global Variables as beans are not always new objects in servicemix */
               
        sku.clear();
        qtyordered.clear();
        priceactual.clear();
             
        /*** create the shipping Config list to map Ebay name with Idempiere code ***/
        shipperNameList = Arrays.asList(shipperconfigName.split("\\s*,\\s*"));
        shipperIDList = Arrays.asList(shipperconfigID.split("\\s*,\\s*"));
        shipperCostList = Arrays.asList(shipperCost.split("\\s*,\\s*"));
        for (int i=0; i<shipperNameList.size(); i++) {
                shipperMap.put(shipperNameList.get(i), shipperIDList.get(i));
                shipperCostMap.put(shipperNameList.get(i), shipperCostList.get(i)); 
        }
        
        /*** add information from config file to order map ***/
        	
        orderEntries.put("ad_client_id", ad_client_id);
        orderEntries.put("ad_org_id", ad_org_id);
        orderEntries.put("m_warehouse_id", m_warehouse_id);
        orderEntries.put("m_pricelist_id", m_pricelist_id);
        orderEntries.put("c_doctype_id", c_doctype_id);
        orderEntries.put("doctypename", doctypename);
        orderEntries.put("c_currency_id", c_currency_id);
        orderEntries.put("c_ordersourcevalue",c_ordersourcevalue);
        
        /*** read XML file from EBAY and analyse information ***/
        
        try {	
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource temp = new InputSource();
            body = body.trim();
            temp.setCharacterStream(new StringReader(body));
            input = db.parse(temp);
            /*** get order time **/
            String timeStamp = input.getElementsByTagName("Timestamp").item(0).getTextContent();
            /** get the date format right for SQL **/ 
            LOGGER.log(Level.INFO, "and the time: {0}", timeStamp);
            String createdCorrected=timeStamp.replaceAll("Z","");
            orderEntries.put("created",createdCorrected.replaceAll("T"," "));
            
            /*** get Buyer subtree ***/
            NodeList buyers = input.getElementsByTagName("Buyer");
            Node buyer = buyers.item(0);
            /*** Get Buyer info from XML file and write it to orderEntries map ***/
            if(buyer instanceof Element) {
                Element buyerDetails = (Element)buyer;
                Node email = buyerDetails.getElementsByTagName("Email").item(0);
                orderEntries.put("bpartnervalue", email.getTextContent());
                orderEntries.put("email", email.getTextContent());
                Node name = buyerDetails.getElementsByTagName("Name").item(0);
                orderEntries.put("name", name.getTextContent().replaceAll("'", "''"));
                Node street1 = buyerDetails.getElementsByTagName("Street1").item(0);
                orderEntries.put("address1", street1.getTextContent().replaceAll("'", "''"));
                if(buyerDetails.getElementsByTagName("Street2").getLength()>0) {
                    Node street2 = buyerDetails.getElementsByTagName("Street2").item(0);
                    orderEntries.put("address2", street2.getTextContent().replaceAll("'", "''"));
                }
                else {
                    orderEntries.put("address2","");
                }
                Node cityName = buyerDetails.getElementsByTagName("CityName").item(0);
                orderEntries.put("city", cityName.getTextContent().replaceAll("'", "''"));
                Node postalCode= buyerDetails.getElementsByTagName("PostalCode").item(0);
                orderEntries.put("postal", postalCode.getTextContent().replaceAll("'", "''"));
                Node country = buyerDetails.getElementsByTagName("Country").item(0);
                orderEntries.put("country", country.getTextContent().replaceAll("'", "''"));
                Node phone = buyerDetails.getElementsByTagName("Phone").item(0);
                orderEntries.put("phone", phone.getTextContent());
            }
             /** replace shipper name with shipper id **/
            shipperName=input.getElementsByTagName("ShippingService").item(0).getTextContent();
            /** check if shipper from XML maps to any names in the list ***/
            if (shipperMap.containsKey(shipperName)) {
                shippingID = shipperMap.get(shipperName);
                shippingCost = shipperCostMap.get(shipperName);
            }
            else { 
                shippingID = "error";
                shippingCost = "0";   } 
            
            orderEntries.put("m_shipper_id",shippingID);
            
            /*** Use original order_id as document no, create individual order_id per product **/
            String ebayOrderID = input.getElementsByTagName("OrderID").item(0).getTextContent();
            orderEntries.put("documentno",ebayOrderID);
            /*** Use the last 6 digits of Ebay OrderID as Idempiere Input OrderID **/
            orderEntries.put("i_bpartner_id",ebayOrderID.substring(ebayOrderID.length() - 6));
            /*** go through list of items and create one sql file per item ***/
            NodeList orderItems = input.getElementsByTagName("Item");
            /** orderValue represents the overall ordervalue **/
            double ordervalue=0;
            for (int i = 0, len = orderItems.getLength(); i < len; i++) {
                Node orderItem = orderItems.item(i);
                if(orderItem instanceof Element) {
                    Element orderItemDetail = (Element)orderItem;
                    String xmlValueSKU = orderItemDetail.getElementsByTagName("SKU").item(0).getTextContent();
                    sku.add(xmlValueSKU);
                    String xmlValueQuantity = orderItemDetail.getElementsByTagName("QuantitySold").item(0).getTextContent();
                    qtyordered.add(xmlValueQuantity);
                    String xmlValuePrice = orderItemDetail.getElementsByTagName("CurrentPrice").item(0).getTextContent();
                    priceactual.add(xmlValuePrice);
                    ordervalue+=Double.parseDouble(xmlValueQuantity)*Double.parseDouble(xmlValuePrice);
                }    
            }
            
            /** check if order value is above shipperFreeOrder and set shippingCost 0 **/
            if(ordervalue>=Double.parseDouble(shipperFreeOrder))
                shippingCost="0";
             /** add delivery as product to get things right in Idempiere**/	
            sku.add("delivery");
            qtyordered.add("1");
            priceactual.add(shippingCost);
            
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Error during XML management {0}",e );
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error during XML management {0}", e);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error during XML management {0}", e);
        }
	
        /*** Create import_BusinessPartner SQL entry  ****/
        
        answer += createSQLBP();
        
	/**
	* go through the list of products and create one entry in the DB per
        * product
        */
                
        for (int i = 0; i < sku.size(); i++) {
			/* create individual i_order_id */	
                        
                        //might create error, since ebay splits orders
			//String uniqueOrder = "1" + orderEntries.get("i_bpartner_id") + "0" + Integer.toString(i+1); 
                        
			long millis = System.currentTimeMillis();
                        String uniqueOrder=Long.toString(millis);
                        orderEntries.put("i_order_id", uniqueOrder.substring(uniqueOrder.length()-6) + "0" + Integer.toString(i+1));

			/** and add to answer */

			answer += createSQL(i);
		}

	return answer;
	}

    public void process(Exchange exchange) throws Exception {
	        // Grab the booked header value
	        // String SOAPACTION = (String) exchange.getIn().getHeader("SOAPACTION");
	        String body = exchange.getIn().getBody(String.class);
	        LOGGER.log(Level.INFO,"this is the log and the body type {0}",body);
	        String answer = transform(body);
		LOGGER.log(Level.INFO,"and the transformed message: {0}",answer);
	        // send the sql as body back
		exchange.getOut().setBody(answer);
	        
    }
	 
	
    public String addToDB(Object body) {
		try {
			Process p = new ProcessBuilder(
					"/opt/omnEmpiere/scripts/addOrderToDB.sh").start();
			p.waitFor();
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			
		    LOGGER.log(Level.SEVERE,IOUtils.toString(error));
		    LOGGER.log(Level.INFO,IOUtils.toString(output));
		    
		    
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"Can not call DB script {0}",e.getMessage());
		} catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE,"Can not call DB script {0}",e.getMessage());
        }
		return(""); 
		
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