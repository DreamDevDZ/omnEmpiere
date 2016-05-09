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
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;





public class updateInv {

    
    private static final transient Logger logger = Logger.getLogger(updateInv.class.getName());
    private boolean verbose = true;
    private String prefix = "updateInv";
    private static final String updateInvURI="Your Magento URI/OmnEmpiereUpdateInv.php";
    private static final String callReindexURI="Your Magento URI/OmnEmpiereCallReIndex.php";
    private static final String ebaytauken="You EBAY token"; // Insert Your EBAY token here
    
/***********************************************/    
/** Method to call the Magento Php scripts    **/
/*** @param body message body                 **/ 
    
    
    public void callMagentoUpdateInv(String body) {
    	
    	
    	
	logger.log(Level.INFO, "Working Directory = {0}", System.getProperty("user.dir"));
	try {       
		Process p = new ProcessBuilder("/usr/bin/curl","--fail", "--silent", "--show-error",updateInvURI).start();
		p.waitFor();
		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		
	    logger.severe(IOUtils.toString(error));
	    logger.info(IOUtils.toString(output));
	    } catch (IOException e) {
              logger.log(Level.SEVERE, "call failed: {0}", e.getMessage()); 
            } catch (InterruptedException e) {       
              logger.log(Level.SEVERE, "call failed: {0}", e.getMessage());
        }       
	} 
    
    public void callMagentoReIndex(String body) {
	logger.info("This is reindexing");
        try {
        	Process p = new ProcessBuilder("/usr/bin/curl","--fail", "--silent", "--show-error",callReindexURI).start();
        	BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        	BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		
        	logger.severe(IOUtils.toString(error));
        	logger.info(IOUtils.toString(output));
        	} catch (IOException e) {
    logger.log(Level.SEVERE, "call failed: {0}", e.getMessage()); }
      }

    
    
  /***********************************/  
  /** Transform body for Ebay      **/
  /**
     * @param body message body   *   
     * @return transformed body
     *******************************/   
  
    @SuppressWarnings("CallToPrintStackTrace")
    public String ebayTransform(String body) {

    	/** Initialize the variable */    	
    	    	
    	 	    String answer;
     
    	        
    	        answer="<?xml version=\"1.0\" encoding=\"utf-8\"?><ReviseInventoryStatusRequest xmlns=\"urn:ebay:apis:eBLBaseComponents\">";
                answer+="<RequesterCredentials><eBayAuthToken>"+ ebaytauken +"</eBayAuthToken></RequesterCredentials>";
    	        
    	        /** extract information from XML message */

    	      try{
    	        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    	        InputSource temp = new InputSource();
    	        body = body.trim();
    	        temp.setCharacterStream(new StringReader(body));

    	        Document input = db.parse(temp);
    	          
    	      
    	        NodeList skus  = input.getElementsByTagName("SKU"); 
    	        NodeList quantities = input.getElementsByTagName("QtyOnHand");
    	        
    			int i = 0;
    			int length = skus.getLength();
    			while (i < length) {
    	        
                            answer+="<InventoryStatus>"; 
                            answer+= "<Quantity>" + quantities.item(i) +"</Quantity>";
                            answer+= "<SKU>" + skus.item(i) + "</SKU>";
                            answer+="</InventoryStatus>";
                            i++;
    			}
                 answer+="</ReviseInventoryStatusRequest>";       
    	  
    	    } catch (ParserConfigurationException e) {
                e.printStackTrace();
    	    } catch (SAXException e) {
               e.printStackTrace();
          } catch (IOException e) {
               e.printStackTrace();
        }
  	        
    	        logger.log(Level.INFO, ">>>> {0}", answer);
    	        return answer;
    	        }
    	  

    	 public String getServerResponse(InputStream responseStream) {
    		String response="";
    		try {
    			response=IOUtils.toString(responseStream, "UTF-8");
    		} catch(java.io.IOException e) {
    			e.printStackTrace(); }
    		return response;
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

   
