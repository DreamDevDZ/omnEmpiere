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


package net.omnempiere.MagentoGetOrders;

import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
	
import org.apache.commons.io.IOUtils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MyTransform {

	/*
	 * The DB connection to IdEmpiere static String db_URI=
	 * "jdbc:postgresql://omnempiere.cvlniysmijeg.eu-west-1.rds.amazonaws.com:5432/idempiere"
	 * ; static String db_User="adempiere"; static String
	 * db_PWD="xxxxxxx";
	 */

	private static final transient Logger logger = Logger.getLogger(MyTransform.class.getName());
	private boolean verbose = true;
	private String prefix = "MyTransform";

	/**
	 * orderValues represents all DB entries for i_order in IdEmpiere without
	 * product list
	 */
	
	static String orderValues = "i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,address1,address2,city,postal,countrycode,email,c_doctype_id,doctypename,taxamt,c_currency_id,m_shipper_id";
    
	/** List of configured Shippers in Magento and IdEmpiere, for shipper_id, check table m_shipper_id in db **/
	static String shipperconfigName = "Flat Rate - Fixed,UPS";    // The configured delivery methods in Magento 
        static String shipperconfigID =	"1000000,1000001";            // The corresponding shipper IDs in Idempiere 
    
	/**
	 * The HashMap orderEntries[orderValues,OrderEntry], ie.
	 * orderEntries["i_order_id",100004]
	 */

	Map<String, String> orderEntries = new HashMap<String, String>();
	List<String> xmlEntries = new ArrayList<String>();
	
	/** the arrays for products entries per order */
	List<String> productvalue = new ArrayList<String>();
	List<String> sku = new ArrayList<String>();
	List<String> qtyordered = new ArrayList<String>();
	List<String> priceactual = new ArrayList<String>();
	String shippingCosts;

	/** The country map **/
	
	final Map<String, String> map = new TreeMap<String, String>   (String.CASE_INSENSITIVE_ORDER);
    
	/** get shipper ID from Shipper Name **/
	
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
    
    /** get the country code for country name **/
    
	String getCode(String country){
		 String countryFound = map.get(country);
	     if(countryFound==null){
	             countryFound="UK";
	     }
	     return countryFound;
		
	}
	
	
	/*** Init the country map **/
	
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
     map.put("UK","GB");
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
	
	/** create the SQL statement, one line per product */

	private String createSQL(int orderline) {

		String sqlStatement;

		/** create the sql command */

		String sql = "INSERT INTO adempiere.i_order (i_order_id,ad_client_id,ad_org_id,created,m_warehouse_id,m_pricelist_id,bpartnervalue,address1,address2,city,postal,countrycode,email,c_doctype_id,doctypename,taxamt,c_currency_id,m_shipper_id,freightamt,productvalue,sku,qtyordered,priceactual) VALUES(";

		/** add the entries from the hashMap */
		for (String key : xmlEntries) {
			sql += "\'"+ orderEntries.get(key) + "\',";
		}
		/** and finally the product for each ordered product */
		sql += "\'" + shippingCosts + "\'," + "\'" + productvalue.get(orderline) + "\',"
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

	/** retrieve values from the HTML product list of Magento */

	private void analyzeHTML(String html) {

		logger.log(Level.INFO, "This is the html source:{0}", html);

		Document doc = Jsoup.parse(html);

		Elements tableItems = doc.select("table.items");
		Elements tableElements = tableItems.select("tbody");

		for (int i = 0; i < tableElements.size(); i++) {

			Element products = tableElements.get(i);

			Element productName = products.select("p.product-name").get(0);

			logger.log(Level.INFO, "The product Name: {0}", productName.text());

			productvalue.add(productName.text());

			Element productSKU = products.select("p.sku").get(0);
			sku.add(productSKU.text().replace("SKU: ", ""));

			Element productQuantity = products.select(
					"td.cell-content.align-center").get(0);
			qtyordered.add(productQuantity.text());

			Element productPrice = products.select("span.price").get(0);
			priceactual.add(productPrice.text().replaceAll("[^\\.0123456789]",
					""));
		}

		Elements tableShipCost = doc.select("tr.shipping");

		Element orderShipCost = tableShipCost.get(0);
		shippingCosts = orderShipCost.text().replaceAll("[^\\.0123456789]", "");
                productvalue.add("Delivery");
                sku.add("delivery");
                qtyordered.add("1");
                priceactual.add(shippingCosts);
	}

	public String transform(Object body) {

		String answer = "";
		String str = body.toString();
		String inputXML = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
                
		/* Reset global Variables as beans are not always new objects in servicemix */
		productvalue.clear();
        sku.clear();
        qtyordered.clear();
        priceactual.clear();        

        
		/* Transform the string orderValues into a list */

		xmlEntries = Arrays.asList(orderValues.split("\\s*,\\s*"));

		Document input = Jsoup.parse(inputXML);

		/*
		 * go through the list xmlEntries, identify the Nodes with name
		 * xmlEntry[i] and get it's value
		 */

		for (int i = 0; i < xmlEntries.size(); i++) {
			String key = xmlEntries.get(i);
			System.out.println(key);

			Element xmlValue = input.select(key).get(0);
			String xmlString = xmlValue.text();
			orderEntries.put(key, xmlString.replaceAll("'","''"));
		}

		/** get the date format right for sql **/
		
		final String OLD_FORMAT = "dd/MM/yyyy hh:mm";
		final String NEW_FORMAT = "yyyy-MM-dd hh:mm";

	
		String newDateString;

		SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
		try{
				Date d = sdf.parse(orderEntries.get("created"));
				sdf.applyPattern(NEW_FORMAT);
				newDateString = sdf.format(d);
				orderEntries.put("created",newDateString);
			} catch(Exception e) {System.out.println("Date Format Failed"); }
		
		/** reformat address **/
		setCountryCodes();
		
		String addressLong = orderEntries.get("address1");
		List<String> addressEntries = Arrays.asList(addressLong.split("\\s*,\\s*"));
		orderEntries.put("countrycode", getCode(addressEntries.get(addressEntries.size() - 1)));
		orderEntries.put("postal", addressEntries.get(addressEntries.size() - 2));
		String cityString=addressEntries.get(addressEntries.size() - 3);
		orderEntries.put("city",cityString.replaceAll("'", "''"));
		String streetString=addressEntries.get(1);
		orderEntries.put("address1", streetString.replaceAll("'", "''")); /** Second entry, first is name **/
		if(addressEntries.size()>5) { String addressString2=addressEntries.get(2);
			orderEntries.put("address2", addressString2.replaceAll("'", "''")); }
		
		/** replace shipper name with shipper id **/
		String shipperID = getShipperID(orderEntries.get("m_shipper_id"));
		orderEntries.put("m_shipper_id",shipperID);
		
		
		/** analyze the HTML entry from Magento and write data to the arrays **/
		analyzeHTML(inputXML);

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

	public void addToDB(Object body) {
		try {
			Process p = new ProcessBuilder(
					"/opt/omnEmpiere/scripts/addOrderToDB.sh").start();    // change PATH if neccessary
			p.waitFor();
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			
		    logger.severe(IOUtils.toString(error));
		    logger.info(IOUtils.toString(output));
		    
		    
		} catch (Exception e) {
			System.err.println("call failed: " + e.getMessage());
			logger.severe("Can not call DB script " + e.getMessage());
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
