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
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;

public class ReadPost implements Processor {

	/*
	 * The DB connection to IdEmpiere static String db_URI=
	 * "jdbc:postgresql://URI"
	 * ; static String db_User="adempiere"; static String
	 * db_PWD="";
	 */

	private static final transient Logger logger = Logger.getLogger(ReadPost.class.getName());
	private boolean verbose = true;
	private String prefix = "MyTransform";

	/**
	 * ebayInput is the list of XML field in the eBay order XML
	 * orderValues represents all DB entries for i_order in IdEmpiere 
	 * Transform reads the values of ebayInput from Body and put it into orderEntry with index orderValues
	 * the right order  of the list values is crucial!!!
	 */
	static String ebayInput= "OrderID,LastModifiedTime,Name,Street1,Street2,CityName,PostalCode,Country,Email,SalesTaxAmount,Total,ShippingService";
	static String orderValues = "i_order_id,created,bpartnervalue,address1,address2,city,postal,countrycode,email,taxamt,totalamount,m_shipper_id";
        static String ad_client_id = "1000000"; /* the client id in IdEmpiere */
        static String ad_org_id= "1000000"; /* the organization id */
        static String m_warehouse_id="1000001"; /* warehouse id */
        static String m_pricelist_id="1000000";
        static String c_doctype_id="1000032"; /*document type id according to c_doctype */
        static String doctypename="Standard Order"; /*kind of order for credit card pre pay order */
        static String c_currency_id="100"; /* currency code c_currency_id see c_currency table in idEmpiere */

   /** List of configured Shippers in Magento and IdEmpiere, for shipper_id, check table m_shipper_id in db **/
        static String shipperconfigName = "ShippingMethodStandard,UPS";
        static String shipperconfigID =     "1000000,1000001";

	/**
	 * The HashMap orderEntries[orderValues,OrderEntry], ie.
	 * orderEntries["i_order_id",100004]
	 */

	Map<String, String> orderEntries = new HashMap<String, String>();
	List<String> xmlEntries = new ArrayList<String>();
	List<String> orderKey = new ArrayList<String>();
	
	/** the arrays for products entries per order */
	List<String> sku = new ArrayList<String>();
	List<String> qtyordered = new ArrayList<String>();
	List<String> priceactual = new ArrayList<String>();
	
	/** Get Shipper ID from Shipper NAme **/

     String getShipperID(String shipperName) {

        List<String> shipperNameList = new ArrayList<String>();
        List<String> shipperIDList = new ArrayList<String>();
        Map<String, String> shipperMap = new HashMap<String, String>();
        String result;

        shipperNameList = Arrays.asList(shipperconfigName.split("\\s*,\\s*"));
        shipperIDList = Arrays.asList(shipperconfigID.split("\\s*,\\s*"));

        for (int i=0; i<shipperNameList.size(); i++) {
              shipperMap.put(shipperNameList.get(i), shipperIDList.get(i));    // is there a clearer way?
            }

        if (shipperMap.containsKey(shipperName)) {
        result = shipperMap.get(shipperName); }
        else { result = "0"; }
        return result;
    }


	/** The country map **/
	
	final Map<String, String> map = new TreeMap<String, String>   (String.CASE_INSENSITIVE_ORDER);
    
	/** get the country code for country name **/
	
	
	String getCode(String country){
		 String countryFound = map.get(country);
	     if(countryFound==null){
	             countryFound="GB";
	     }
	     return countryFound;
		
	}
	
	
	/* Init the country map 
	
		private void setCountryCodes() {
	
     map.put("Andorra, Principality Of", "AD");
     map.put("United Arab Emirates", "AE");
     map.put("Afghanistan, Islamic State Of", "AF");
     map.put("Antigua And Barbuda", "AG");
     map.put("Anguilla", "AI");
     map.put("Albania", "AL");
     map.put("Armenia", "AM");
     map.put("Netherlands Antilles", "AN");
     map.put("Angola", "AO");
     map.put("Antarctica", "AQ");
     map.put("Argentina", "AR");
     map.put("American Samoa", "AS");
     map.put("Austria", "AT");
     map.put("Australia", "AU");
     map.put("Aruba", "AW");
     map.put("Azerbaidjan", "AZ");
     map.put("Bosnia-Herzegovina", "BA");
     map.put("Barbados", "BB");
     map.put("Bangladesh", "BD");
     map.put("Belgium", "BE");
     map.put("Burkina Faso", "BF");
     map.put("Bulgaria", "BG");
     map.put("Bahrain", "BH");
     map.put("Burundi", "BI");
     map.put("Benin", "BJ");
     map.put("Bermuda", "BM");
     map.put("Brunei Darussalam", "BN");
     map.put("Bolivia", "BO");
     map.put("Brazil", "BR");
     map.put("Bahamas", "BS");
     map.put("Bhutan", "BT");
     map.put("Bouvet Island", "BV");
     map.put("Botswana", "BW");
     map.put("Belarus", "BY");
     map.put("Belize", "BZ");
     map.put("Canada", "CA");
     map.put("Cocos (Keeling) Islands", "CC");
     map.put("Central African Republic", "CF");
     map.put("Congo, The Democratic Republic Of The", "CD");
     map.put("Congo", "CG");
     map.put("Switzerland", "CH");
     map.put("Ivory Coast (Cote D'Ivoire)", "CI");
     map.put("Cook Islands", "CK");
     map.put("Chile", "CL");
     map.put("Cameroon", "CM");
     map.put("China", "CN");
     map.put("Colombia", "CO");
     map.put("Costa Rica", "CR");
     map.put("Former Czechoslovakia", "CS");
     map.put("Cuba", "CU");
     map.put("Cape Verde", "CV");
     map.put("Christmas Island", "CX");
     map.put("Cyprus", "CY");
     map.put("Czech Republic", "CZ");
     map.put("Germany", "DE");
     map.put("Djibouti", "DJ");
     map.put("Denmark", "DK");
     map.put("Dominica", "DM");
     map.put("Dominican Republic", "DO");
     map.put("Algeria", "DZ");
     map.put("Ecuador", "EC");
     map.put("Estonia", "EE");
     map.put("Egypt", "EG");
     map.put("Western Sahara", "EH");
     map.put("Eritrea", "ER");
     map.put("Spain", "ES");
     map.put("Ethiopia", "ET");
     map.put("Finland", "FI");
     map.put("Fiji", "FJ");
     map.put("Falkland Islands", "FK");
     map.put("Micronesia", "FM");
     map.put("Faroe Islands", "FO");
     map.put("France", "FR");
     map.put("France (European Territory)", "FX");
     map.put("Gabon", "GA");
     map.put("Great Britain", "UK");
     map.put("Grenada", "GD");
     map.put("Georgia", "GE");
     map.put("French Guyana", "GF");
     map.put("Ghana", "GH");
     map.put("Gibraltar", "GI");
     map.put("Greenland", "GL");
     map.put("Gambia", "GM");
     map.put("Guinea", "GN");
     map.put("Guadeloupe (French)", "GP");
     map.put("Equatorial Guinea", "GQ");
     map.put("Greece", "GR");
     map.put("S. Georgia & S. Sandwich Isls.", "GS");
     map.put("Guatemala", "GT");
     map.put("Guam (USA)", "GU");
     map.put("Guinea Bissau", "GW");
     map.put("Guyana", "GY");
     map.put("Hong Kong", "HK");
     map.put("Heard And McDonald Islands", "HM");
     map.put("Honduras", "HN");
     map.put("Croatia", "HR");
     map.put("Haiti", "HT");
     map.put("Hungary", "HU");
     map.put("Indonesia", "ID");
     map.put("Ireland", "IE");
     map.put("Israel", "IL");
     map.put("India", "IN");
     map.put("British Indian Ocean Territory", "IO");
     map.put("Iraq", "IQ");
     map.put("Iran", "IR");
     map.put("Iceland", "IS");
     map.put("Italy", "IT");
     map.put("Jamaica", "JM");
     map.put("Jordan", "JO");
     map.put("Japan", "JP");
     map.put("Kenya", "KE");
     map.put("Kyrgyz Republic (Kyrgyzstan)", "KG");
     map.put("Cambodia, Kingdom Of", "KH");
     map.put("Kiribati", "KI");
     map.put("Comoros", "KM");
     map.put("Saint Kitts & Nevis Anguilla", "KN");
     map.put("North Korea", "KP");
     map.put("South Korea", "KR");
     map.put("Kuwait", "KW");
     map.put("Cayman Islands", "KY");
     map.put("Kazakhstan", "KZ");
     map.put("Laos", "LA");
     map.put("Lebanon", "LB");
     map.put("Saint Lucia", "LC");
     map.put("Liechtenstein", "LI");
     map.put("Sri Lanka", "LK");
     map.put("Liberia", "LR");
     map.put("Lesotho", "LS");
     map.put("Lithuania", "LT");
     map.put("Luxembourg", "LU");
     map.put("Latvia", "LV");
     map.put("Libya", "LY");
     map.put("Morocco", "MA");
     map.put("Monaco", "MC");
     map.put("Moldavia", "MD");
     map.put("Madagascar", "MG");
     map.put("Marshall Islands", "MH");
     map.put("Macedonia", "MK");
     map.put("Mali", "ML");
     map.put("Myanmar", "MM");
     map.put("Mongolia", "MN");
     map.put("Macau", "MO");
     map.put("Northern Mariana Islands", "MP");
     map.put("Martinique (French)", "MQ");
     map.put("Mauritania", "MR");
     map.put("Montserrat", "MS");
     map.put("Malta", "MT");
     map.put("Mauritius", "MU");
     map.put("Maldives", "MV");
     map.put("Malawi", "MW");
     map.put("Mexico", "MX");
     map.put("Malaysia", "MY");
     map.put("Mozambique", "MZ");
     map.put("Namibia", "NA");
     map.put("New Caledonia (French)", "NC");
     map.put("Niger", "NE");
     map.put("Norfolk Island", "NF");
     map.put("Nigeria", "NG");
     map.put("Nicaragua", "NI");
     map.put("Netherlands", "NL");
     map.put("Norway", "NO");
     map.put("Nepal", "NP");
     map.put("Nauru", "NR");
     map.put("Neutral Zone", "NT");
     map.put("Niue", "NU");
     map.put("New Zealand", "NZ");
     map.put("Oman", "OM");
     map.put("Panama", "PA");
     map.put("Peru", "PE");
     map.put("Polynesia (French)", "PF");
     map.put("Papua New Guinea", "PG");
     map.put("Philippines", "PH");
     map.put("Pakistan", "PK");
     map.put("Poland", "PL");
     map.put("Saint Pierre And Miquelon", "PM");
     map.put("Pitcairn Island", "PN");
     map.put("Puerto Rico", "PR");
     map.put("Portugal", "PT");
     map.put("Palau", "PW");
     map.put("Paraguay", "PY");
     map.put("Qatar", "QA");
     map.put("Reunion (French)", "RE");
     map.put("Romania", "RO");
     map.put("Russian Federation", "RU");
     map.put("Rwanda", "RW");
     map.put("Saudi Arabia", "SA");
     map.put("Solomon Islands", "SB");
     map.put("Seychelles", "SC");
     map.put("Sudan", "SD");
     map.put("Sweden", "SE");
     map.put("Singapore", "SG");
     map.put("Saint Helena", "SH");
     map.put("Slovenia", "SI");
     map.put("Svalbard And Jan Mayen Islands", "SJ");
     map.put("Slovak Republic", "SK");
     map.put("Sierra Leone", "SL");
     map.put("San Marino", "SM");
     map.put("Senegal", "SN");
     map.put("Somalia", "SO");
     map.put("Suriname", "SR");
     map.put("Saint Tome (Sao Tome) And Principe", "ST");
     map.put("Former USSR", "SU");
     map.put("El Salvador", "SV");
     map.put("Syria", "SY");
     map.put("Swaziland", "SZ");
     map.put("Turks And Caicos Islands", "TC");
     map.put("Chad", "TD");
     map.put("French Southern Territories", "TF");
     map.put("Togo", "TG");
     map.put("Thailand", "TH");
     map.put("Tadjikistan", "TJ");
     map.put("Tokelau", "TK");
     map.put("Turkmenistan", "TM");
     map.put("Tunisia", "TN");
     map.put("Tonga", "TO");
     map.put("East Timor", "TP");
     map.put("Turkey", "TR");
     map.put("Trinidad And Tobago", "TT");
     map.put("Tuvalu", "TV");
     map.put("Taiwan", "TW");
     map.put("Tanzania", "TZ");
     map.put("Ukraine", "UA");
     map.put("Uganda", "UG");
     map.put("United Kingdom", "GB");
     map.put("USA Minor Outlying Islands", "UM");
     map.put("United States", "US");
     map.put("Uruguay", "UY");
     map.put("Uzbekistan", "UZ");
     map.put("Holy See (Vatican City State)", "VA");
     map.put("Saint Vincent & Grenadines", "VC");
     map.put("Venezuela", "VE");
     map.put("Virgin Islands (British)", "VG");
     map.put("Virgin Islands (USA)", "VI");
     map.put("Vietnam", "VN");
     map.put("Vanuatu", "VU");
     map.put("Wallis And Futuna Islands", "WF");
     map.put("Samoa", "WS");
     map.put("Yemen", "YE");
     map.put("Mayotte", "YT");
     map.put("Yugoslavia", "YU");
     map.put("South Africa", "ZA");
     map.put("Zambia", "ZM");
     map.put("Zaire", "ZR");
     map.put("Zimbabwe", "ZW");

    }
	
*/	
	
	/** create the sql statement, one line per product */

	private String createSQL(int orderline) {

		String sqlStatement;

		/** create the sql command */
       
		String sql = "INSERT INTO adempiere.i_order (i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,address1,address2,city,postal,countrycode,email,c_doctype_id,doctypename,taxamt,m_shipper_id,c_currency_id,sku,qtyordered,priceactual) VALUES(";
        String keyValues = "i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,address1,address2,city,postal,countrycode,email,c_doctype_id,doctypename,taxamt,m_shipper_id,c_currency_id";
        List<String> keyValuesList = new ArrayList<String>();
        keyValuesList = Arrays.asList(keyValues.split("\\s*,\\s*"));
        /** add the entries from the hashMap */
		for (String key : keyValuesList) {
			logger.info("the key: "+key);
			sql += "\'"+ orderEntries.get(key).toString() + "\',";
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
	
	private String transform(String inputXML) {

		String answer = "";
		Document input;

                /* Reset global Variables as beans are not always new objects in servicemix */
               
                sku.clear();
                qtyordered.clear();
                priceactual.clear();
              
		/* Transform the string orderValues into a list */

		xmlEntries = Arrays.asList(ebayInput.split("\\s*,\\s*"));
		orderKey = Arrays.asList(orderValues.split("\\s*,\\s*"));
		
		input = Jsoup.parse(inputXML);
		
		/*
		 * go through the list xmlEntries, identify the Nodes with name
		 * xmlEntry[i] and get it's value
		 */

		for (int i = 0; i < xmlEntries.size(); i++) {
			String key = xmlEntries.get(i);
            		Element xmlValue = input.select(key).get(0);
            		orderEntries.put(orderKey.get(i), xmlValue.text());
			logger.info("the key: "+key+" and the Value:" + xmlValue.text());
            }
		
		/** get the date format right for sql **/
		
		String createdTemp=orderEntries.get("created");
		logger.info("and the time: "+createdTemp);
		String createdCorrected=createdTemp.replaceAll("Z","");
		orderEntries.put("created",createdCorrected.replaceAll("T"," "));
	        
                 /** replace shpper name with shipper id **/
                String shipperID = getShipperID(orderEntries.get("m_shipper_id"));
                orderEntries.put("m_shipper_id",shipperID);

	
		/** add special information  **/ 
		
		orderEntries.put("ad_client_id", ad_client_id);
		orderEntries.put("ad_org_id", ad_org_id);
		orderEntries.put("m_warehouse_id", m_warehouse_id);
		orderEntries.put("m_pricelist_id", m_pricelist_id);
		orderEntries.put("c_doctype_id", c_doctype_id);
		orderEntries.put("doctypename", doctypename);
		orderEntries.put("c_currency_id", c_currency_id);
		
	
		/** get ordered products **/
		Elements productsFound = input.select("SKU");
		
		for(int i = 0;i<productsFound.size();i++) {
	            Element xmlValueSKU = productsFound.get(i);
		    sku.add(xmlValueSKU.text());
		    Element xmlValueQuantity = input.select("QuantityPurchased").get(i);
		    qtyordered.add(xmlValueQuantity.text());
		    Element xmlValuePrice = input.select("TransactionPrice").get(i);
		    priceactual.add(xmlValuePrice.text());
		}
		
		/**
		 * go through the list of products and create one entry in the DB per
		 * product
		 */

			
		
		for (int i = 0; i < sku.size(); i++) {
			/* create individual i_order_id */
			String tmp = orderEntries.get("i_order_id");
			String tmpshort = tmp.substring(tmp.length() - 6);
			String uniqueOrder = "1" + tmpshort + "0" + Integer.toString(i+1);
			orderEntries.put("i_order_id", uniqueOrder);

			/** and add to answer */

			answer += createSQL(i);
		}

		return answer;
	}

	public void process(Exchange exchange) throws Exception {
	        // Grab the booked header value
	        // String SOAPACTION = (String) exchange.getIn().getHeader("SOAPACTION");
	        String body = exchange.getIn().getBody(String.class);
	        logger.info("this is the log and the body type"+body);
	        String answer = transform(body);
		logger.info("and the transformed message: "+answer);
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
			
		    logger.severe(IOUtils.toString(error));
		    logger.info(IOUtils.toString(output));
		    
		    
		} catch (Exception e) {
			System.err.println("call failed: " + e.getMessage());
			logger.severe("Can not call DB script " + e.getMessage());
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

   
