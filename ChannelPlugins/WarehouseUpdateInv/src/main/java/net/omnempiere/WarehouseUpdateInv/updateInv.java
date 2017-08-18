/******************************************************************************
 * Product: OmnEmpiere - sub-project of ADempiere                              *
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

package net.omnempiere.WarehouseUpdateInv;


import java.util.logging.Logger;
import java.io.StringReader;
import java.io.InputStream;
import java.io.*;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;





public class updateInv {

    
    private static final transient Logger LOGGER = Logger.getLogger(updateInv.class.getName());
    private boolean verbose = true;
    private String prefix = "updateInv";
    
    
/***********************************************/
/** Method to call the Magento Php scripts    **/
/**                   **/
/**
* @param body              *************************/
    
    public void callMagentoUpdateInv(String body) {
     
    	
    String updateInvURI="";
    
    Properties prop = new Properties();
    InputStream propsInput;
    String OmnEmpiere_Home = System.getenv("OMNEMPIERE_HOME");
    try {   propsInput = new FileInputStream(OmnEmpiere_Home+"/etc/updateInventory.properties");
		// load a properties file
		prop.load(propsInput);
                updateInvURI=prop.getProperty("updateInvURI");
        } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Read Properties failed: {0}", e.getMessage()); 
                    }
    
    try {       
        Process p = new ProcessBuilder("/usr/bin/curl","--fail", "--silent", "--show-error",updateInvURI).start();
	p.waitFor();
	BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		
	LOGGER.severe(IOUtils.toString(error));
	//LOGGER.info(IOUtils.toString(output));
	    } catch (IOException e) {
              LOGGER.log(Level.SEVERE, "call failed: {0}", e.getMessage()); 
            } catch (InterruptedException e) {       
              LOGGER.log(Level.SEVERE, "call failed: {0}", e.getMessage());
        }       
    }     
    
  /***********************************/  
  /** Transform body for Ebay      **/
  /** @param body message body     **/
  /**   * @return transformed body **/
  /**
     * @param body*
     * @return *******************************/
  

    public String ebayTransform(String body) {

    	/** Initialize the variable */    	
    	    	
    	Properties prop = new Properties();
        InputStream propsInput;
        String OmnEmpiere_Home = System.getenv("OMNEMPIERE_HOME");
        String ebayToken="";
        
         //LOGGER.log(Level.INFO,"Ebay HTML Body {0}",body);
        
        String answer;
        String minQuantity="10";
        String ebayQuantity="5";
        
        // Get properties
        try {   propsInput = new FileInputStream(OmnEmpiere_Home+"/etc/updateInventory.properties");
		// load a properties file
		prop.load(propsInput);
                ebayToken=prop.getProperty("ebayToken");
                minQuantity=prop.getProperty("minQuantity");
                ebayQuantity=prop.getProperty("ebayQuantity");
        } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Read Properties failed: {0}", e.getMessage()); 
                    }
        // Build Ebay update Inventory command
    	answer="<?xml version=\"1.0\" encoding=\"utf-8\"?><ReviseInventoryStatusRequest xmlns=\"urn:ebay:apis:eBLBaseComponents\">";
        answer+="<RequesterCredentials><eBayAuthToken>"+ ebayToken +"</eBayAuthToken></RequesterCredentials>";
    	        
    	/** extract information from XML message */

    	try{
    	    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    	    InputSource temp = new InputSource();
    	    body = body.trim();
    	    temp.setCharacterStream(new StringReader(body));

    	    Document input = db.parse(temp);  
    	    NodeList skus  = input.getElementsByTagName("SKU"); 
    	    NodeList quantities = input.getElementsByTagName("QtyOnHand");
            NodeList categories= input.getElementsByTagName("Ebay_ID");
              
            int i = 0;
            int itemsynced = 0;
            String quantity;
            int length = skus.getLength();
            while (i < length) {
                if(!("".equals(categories.item(i).getTextContent()))) {
                    
                    // Check for min qunatity and set qunatity
                    String quantityRaw=quantities.item(i).getTextContent(); 
                        if(Integer.parseInt(quantityRaw)<Integer.parseInt(minQuantity)) {
                            quantity="0"; }
                        else {
                            quantity=ebayQuantity;
                        }
                    // Build the string    
                    answer+="<InventoryStatus>"; 
                    answer+= "<Quantity>" + quantity +"</Quantity>";
                    answer+= "<ItemID>" + categories.item(i).getTextContent() +"</ItemID>";
                    answer+= "<SKU>" + skus.item(i).getTextContent() + "</SKU>";
                    answer+="</InventoryStatus>";
                    itemsynced++;
                }
                i++;
            }
            answer+="</ReviseInventoryStatusRequest>";  
            if(itemsynced==0) { 
                answer="";
                }    	
    	  
    	    } catch (ParserConfigurationException e) {
                LOGGER.log(Level.SEVERE,"Error reading activemq {0}",e);
    	    } catch (SAXException e) {
               LOGGER.log(Level.SEVERE,"Error reading activemq {0}",e);
            } catch (IOException e) {
               LOGGER.log(Level.SEVERE,"Error reading activemq {0}",e);
            }
  	        
    	    //LOGGER.log(Level.INFO, "The Ebay reviseItemRequest {0}", answer);
    	    return answer;
    }
    	  

    public String getServerResponse(InputStream responseStream) throws EbayException {
	String response="";
	try {
		response=IOUtils.toString(responseStream, "UTF-8");
	} catch(java.io.IOException e) {
		LOGGER.log(Level.SEVERE, "Exception while getting server response: {0}", e.getMessage());
        }
        if(response.contains("<SeverityCode>Error</SeverityCode>")) { 
            throw new EbayException(response,new Throwable(response));
        } 
        return response;
    }
    
    
    public String handleException(Exchange exchange) throws Exception {
        // the caused by exception is stored in a property on the exchange
        Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class); 
        return caused.getMessage();
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

   
