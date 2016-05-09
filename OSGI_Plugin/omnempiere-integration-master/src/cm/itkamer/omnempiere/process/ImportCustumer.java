/******************************************************************************
 * Product: iDempiere - sub-project of ADempiere 				              *
 * Copyright (C) ALL GPL FOSS PROJECTS where taken				              *
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

package cm.itkamer.omnempiere.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.compiere.model.I_C_BP_Group;
import org.compiere.model.I_C_Country;
import org.compiere.model.I_I_BPartner;
import org.compiere.model.MLocation;
import org.compiere.model.Query;
import org.compiere.model.X_C_BPartner;
import org.compiere.model.X_C_BPartner_Location;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cm.itkamer.omnempiere.activemq.ActiveMQClient;

public class ImportCustumer extends SvrProcess {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.process.ImportCustumer";
	private static int m_customerProcess = 0;
	private static int m_customerCount = 0;
	private static String m_test = "N";
	private static String m_brokerUrl = "tcp://localhost:61616";
	private static String m_customerQueue = "MCustomer";
	private static String m_username = "admin";
	private static String m_password = "";

	/**
	 * Called from Process in POS Synchronisation menu.
	 * 
	 * @author Based on ActiveMQ and XML foss resource. Redhuan D. Oon
	 *         http://www.red1.org/adempiere
	 * @param set
	 *            from Process params in prepare()
	 * @throws Exception
	 *             for ActiveMQ Stomp connection Importing of custumer from POS
	 *             into ERP.
	 * 
	 *             Contrizbutor Ing. Tatioti Mbogning Raoul Contributor Ing.
	 *             Taho Tamokeu Jolivais Steve Finish the synchronization of all
	 *             the informations about a Business Partner
	 */

	private static void parseXMLString(String message) throws SAXException,
			ParserConfigurationException, IOException {
		// uncomment for testing, together with above
		// message =
		// "<?xml version=\"1.0\"?><entityDetail><type>I_Order</type><BPartnerValue>Joe Block</BPartnerValue><detail><DocTypeName>POS Order</DocTypeName><AD_Client_ID>11</AD_Client_ID><AD_Org_ID>11</AD_Org_ID><DocumentNo>40</DocumentNo><DateOrdered>2011-09-08 14:52:52.152</DateOrdered><ProductValue>Rake-Metal</ProductValue><QtyOrdered>1.0</QtyOrdered><PriceActual>12.0</PriceActual><TaxAmt>0.0</TaxAmt></detail></entityDetail>";
		// Adempiere.startupEnvironment(true);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(message.getBytes()));
		Element docEle = doc.getDocumentElement();
		NodeList records = docEle.getElementsByTagName("customer");

		m_customerCount += records.getLength();
		String middlename = "", firstname = "", lastname = "";
		for (int i = 0; i < records.getLength(); i++) {
			String name = "";
			int i_bPartner_id = 0;
			// check for detail = POS Order only. Other data will be handle in
			// later brackets
			// if
			// (!records.item(i).getFirstChild().getTextContent().equals("POS Order"))
			// continue;
			X_C_BPartner customers = new X_C_BPartner(Env.getCtx(), 0, null);
			X_C_BPartner_Location billing_adress = new X_C_BPartner_Location(
					Env.getCtx(), 0, null);
			X_C_BPartner_Location shipping_adress = new X_C_BPartner_Location(
					Env.getCtx(), 0, null);
			MLocation x_c_location = new MLocation(Env.getCtx(), 0, null);
			MLocation x_c_location1 = new MLocation(Env.getCtx(), 0, null);

			NodeList customer = records.item(i).getChildNodes();

			for (int j = 0; j < customer.getLength(); j++) {
				Node n = customer.item(j);
				String column = n.getNodeName();

				// get Customer Name
				// Add by Ing Tatioti Mbogning Raoul
				// Finish the synchronisation of all informations about
				// Customers
				if (column.equals("firstname")) {
					firstname = n.getTextContent();
				}

				else if (column.equals("lastname"))
					lastname = n.getTextContent();
				else if (column.equals("middlename"))
					middlename = n.getTextContent();
				else if (column.equals(I_I_BPartner.COLUMNNAME_IsActive)) {
					Boolean bol = false;
					String active = n.getTextContent();
					if (active.equals("1"))
						bol = true;
					customers.setIsActive(bol);
				}
				// else if (column.equals("email"))
				// customers.setEMail(n.getTextContent());
				else if (column.equals("billingAddress")) {
					//
					NodeList records1 = docEle
							.getElementsByTagName("billingAddress");
					NodeList billingAddress = records1.item(0).getChildNodes();
					for (int h = 0; h < billingAddress.getLength(); h++) {

						Node m = billingAddress.item(h);
						String column1 = m.getNodeName();

						if (column1.equals("street"))
							billing_adress.setName(m.getTextContent());
						else if (column1.equals("postcode"))
							x_c_location.setPostal(m.getTextContent());

						else if (column1.equals("telephone"))
							billing_adress.setPhone(m.getTextContent());

						else if (column1.equals("country_id")) {
							int C_Country_ID = 0;
							C_Country_ID = new Query(Env.getCtx(),
									I_C_Country.Table_Name,
									I_C_Country.COLUMNNAME_CountryCode + "=?",
									null).setParameters(m.getTextContent())
									.firstId();
							x_c_location.setC_Country_ID(C_Country_ID);
						} else if (column1.equals("city"))
							x_c_location.setCity(m.getTextContent());

					}

					billing_adress.setIsBillTo(true);
					billing_adress.setIsActive(true);

				} else if (column.equals("shippingAddress")) {
					//
					NodeList records2 = docEle
							.getElementsByTagName("shippingAddress");
					NodeList shippingAddress = records2.item(0).getChildNodes();
					x_c_location1 = new MLocation(Env.getCtx(), 0, null);

					for (int k = 0; k < shippingAddress.getLength(); k++) {
						Node l = shippingAddress.item(k);
						String column2 = l.getNodeName();

						if (column2.equals("street"))
							shipping_adress.setName(l.getTextContent());
						else if (column2.equals("postcode"))
							x_c_location1.setPostal(l.getTextContent());

						else if (column2.equals("telephone"))
							shipping_adress.setPhone(l.getTextContent());

						else if (column2.equals("country_id")) {
							int C_Country_ID = 0;
							C_Country_ID = new Query(Env.getCtx(),
									I_C_Country.Table_Name,
									I_C_Country.COLUMNNAME_CountryCode + "=?",
									null).setParameters(l.getTextContent())
									.firstId();
							x_c_location1.setC_Country_ID(C_Country_ID);
						} else if (column2.equals("city"))
							x_c_location1.setCity(l.getTextContent());

					}
					shipping_adress.setIsShipTo(true);
					shipping_adress.setIsActive(true);
				}

				String text = n.getTextContent();
				System.out.println("Node =  " + column + " Text = " + text);
			}
			// Modified by Ing Tatioti Mbogning Raoul
			name = firstname + " " + lastname + " " + middlename;

			// group code of customers
			int customer_group = new Query(Env.getCtx(),
					I_C_BP_Group.Table_Name, I_C_BP_Group.COLUMNNAME_Name
							+ "=?", null).setParameters("Standard Customers")
					.firstId();
			if (!customers.equals(null)) {
				customers.setName(name);
				customers.setIsCustomer(true);
				customers.setIsProspect(false);
				customers.setC_BP_Group_ID(customer_group);
				customers.save();
				m_customerProcess++;
				i_bPartner_id = customers.get_ID();
				// we verifie if billing adress was informed
				if (!x_c_location.equals(null)) {

					x_c_location.save();

					int id_location = x_c_location.get_ID();

					if (!billing_adress.equals(null)) {
						billing_adress.setC_Location_ID(id_location);
						billing_adress.setC_BPartner_ID(i_bPartner_id);
						billing_adress.save();
					}

				}

				if (!x_c_location.equals(null)) {
					x_c_location1.save();
					int id_location1 = x_c_location1.get_ID();

					if (shipping_adress.equals(null)) {
						shipping_adress.setC_Location_ID(id_location1);
						shipping_adress.setC_BPartner_ID(i_bPartner_id);
						shipping_adress.save();
					}

				}
			}

		}
		{
			// to handle import of other data such as payments or returns
		}
	}

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("Test"))
				m_test = (String) para[i].getParameter();
			else if (name.equals("CustomerQueue"))
				m_customerQueue = (String) para[i].getParameter();
			else if (name.equals("Host"))
				m_brokerUrl = "tcp://" + (String) para[i].getParameter();
			else if (name.equals("port"))
				m_brokerUrl += ":" + (String) para[i].getParameter();
			else if (name.equals("username"))
				m_username = (String) para[i].getParameter();
			else if (name.equals("Password"))
				m_password = (String) para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		ActiveMQClient mqClient = new ActiveMQClient(m_brokerUrl, m_username,
				m_password);

		if (!mqClient.init())
			return "ActiveMQ Service Error";

		// Add by Ing. Tatioti Mbogning Raoul
		ArrayList<Message> messageList = mqClient
				.consumeAllMessages(m_customerQueue);

		if (!messageList.isEmpty()) {
			for (int i = 0; i < messageList.size(); ++i) {
				// Save all customers or not if it is a test run case
				parseXMLString(((TextMessage) messageList.get(i)).getText());
			}

			// send back all the previous customers in the customers queue
			if (m_test.equals("Y")) {
				for (int i = 0; i < messageList.size(); ++i) {
					mqClient.sendMessage(
							((TextMessage) messageList.get(i)).getText(),
							m_customerQueue);
				}
			}

			addLog("Import Result");

			if (m_test.equals("Y")) {
				addLog(getProcessInfo().getAD_Process_ID(), new Timestamp(
						System.currentTimeMillis()), new BigDecimal(
						getProcessInfo().getAD_PInstance_ID()),
						"Test run : Success !!!");
			}

			if (m_customerCount == m_customerProcess) {
				addLog(getProcessInfo().getAD_Process_ID(),
						new Timestamp(System.currentTimeMillis()),
						new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
						"Imported Customers : "
								+ String.valueOf(m_customerProcess));

				return "SUCCESS !!! All customers has been imported";
			} else {
				addLog(getProcessInfo().getAD_Process_ID(),
						new Timestamp(System.currentTimeMillis()),
						new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
						"Imported Customers : "
								+ String.valueOf(m_customerProcess));

				addLog(getProcessInfo().getAD_Process_ID(),
						new Timestamp(System.currentTimeMillis()),
						new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
						"Losted Customers : "
								+ String.valueOf(m_customerCount
										- m_customerProcess));
				return "All customers has not been imported correctly. !!!";
			}
		} else {
			return "There is no customers to import.";
		}
	}
}
