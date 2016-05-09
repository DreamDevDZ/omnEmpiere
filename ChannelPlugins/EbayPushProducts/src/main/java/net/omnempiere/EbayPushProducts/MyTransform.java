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


package net.omnempiere.EbayPushProducts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.io.StringReader;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.apache.commons.io.IOUtils;

public class MyTransform  {

    private static final transient Logger logger = Logger.getLogger(MyTransform.class.getName());
    private boolean verbose = true;
    private String prefix = "MyTransform";

    public String transform(String body) {

/** Initialize the variable */    	
    	
 	    String answer,price,quantity,productname,upc,picture_uri,category,description,sku;
        price="dummy";
        quantity="dummy";
        productname="dummy";
        upc="dummy";
        picture_uri="dummy";
        category="dummy";
        description="dummy";
        sku="dummy";

        final String ebaytauken="Your Ebay Application token"
        final String seller_email="Your Email";  
        final String country_code="GB";  /*Your country Code*/
        final String site_code="UK";   /* Website code */
        final String currency_code="GBP";  /* Currency */
        final String post_code="Your PostCode";
        final String paymentmethod="PayPal";
        final String shippingService="UK_OtherCourier,UK_CollectInPerson"; /** Set your Shipping service here. see Ebay Documentation for list **/
        final String shippingCosts="4.0,0.0"; /*Set Shipping costs */
        final String freeShipping="1";
        
/** build shipping services and costs list */       
        List<String> shippingServiceList = new ArrayList<String>();
        List<String> shippingCostsList = new ArrayList<String>();
        
        shippingServiceList = Arrays.asList(shippingService.split("\\s*,\\s*"));
        shippingCostsList = Arrays.asList(shippingCosts.split("\\s*,\\s*"));
        
/** extract information from XML message */

      try{
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource temp = new InputSource();
        body = body.trim();
        temp.setCharacterStream(new StringReader(body));

        Document input = db.parse(temp);
          
        NodeList nodes = input.getElementsByTagName("Name");
        Node line = nodes.item(0);
        productname=line.getTextContent();  
                        
        nodes = input.getElementsByTagName("PriceStd");
        line = nodes.item(0);
        price=line.getTextContent();
   
        nodes = input.getElementsByTagName("UPC");
        line = nodes.item(0);
        upc=line.getTextContent();
          
        nodes = input.getElementsByTagName("QtyOnHand");
        line = nodes.item(0);
        quantity=line.getTextContent();  

	    nodes = input.getElementsByTagName("ImageURL");
        line = nodes.item(0);
        picture_uri=line.getTextContent();

        nodes = input.getElementsByTagName("CategoryID_EBAY");
        line = nodes.item(0);
        category=line.getTextContent(); 
        
        nodes = input.getElementsByTagName("Description");
        line = nodes.item(0);
        description=line.getTextContent();
        
        nodes = input.getElementsByTagName("M_Product_ID");
        line = nodes.item(0);
        sku=line.getTextContent();
  
    } catch (Exception e) {
  e.printStackTrace();
  }

/* Bulid answer string */

        answer="<?xml version=\"1.0\" encoding=\"utf-8\"?><AddItemRequest xmlns=\"urn:ebay:apis:eBLBaseComponents\"><RequesterCredentials><eBayAuthToken>"+ ebaytauken +"</eBayAuthToken></RequesterCredentials>";
        answer+="<Item><Title>"+productname+"</Title><Description>"+description+"</Description><SKU>"+sku+"</SKU><PrimaryCategory><CategoryID>"+category+"</CategoryID></PrimaryCategory><StartPrice>"+price+"</StartPrice><ConditionID>1000</ConditionID><Country>"+country_code+"</Country><Currency>"+currency_code+"</Currency><DispatchTimeMax>3</DispatchTimeMax><ListingDuration>Days_7</ListingDuration><ListingType>FixedPriceItem</ListingType>";
        answer+="<PaymentMethods>"+paymentmethod+"</PaymentMethods><PayPalEmailAddress>"+seller_email+"</PayPalEmailAddress><PostalCode>"+post_code+"</PostalCode><ProductListingDetails><EAN>"+upc+"</EAN><IncludePrefilledItemInformation>true</IncludePrefilledItemInformation><IncludeStockPhotoURL>true</IncludeStockPhotoURL></ProductListingDetails><PictureDetails><PictureURL>"+picture_uri+"</PictureURL></PictureDetails>";
        answer+="<Quantity>"+quantity+"</Quantity><ReturnPolicy><ReturnsAcceptedOption>ReturnsAccepted</ReturnsAcceptedOption><RefundOption>MoneyBack</RefundOption><ReturnsWithinOption>Days_30</ReturnsWithinOption><Description>If not satisfied, return the item for refund.</Description></ReturnPolicy>";
        answer+="<ShippingDetails><ShippingServiceOptions><FreeShipping>"+freeShipping+"</FreeShipping><ShippingServicePriority>1</ShippingServicePriority>";
        
        for (int i = 0; i < shippingServiceList.size(); i++) {
        	answer+="<ShippingService>"+shippingServiceList.get(i)+"</ShippingService><ShippingServiceCost>"+shippingCostsList.get(i)+"</ShippingServiceCost><ShippingServiceAdditionalCost>0.00</ShippingServiceAdditionalCost>";        	
        }
        answer+="</ShippingServiceOptions></ShippingDetails><Site>"+site_code+"</Site></Item></AddItemRequest>";
        	
        
        logger.info(">>>> " + answer);
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

