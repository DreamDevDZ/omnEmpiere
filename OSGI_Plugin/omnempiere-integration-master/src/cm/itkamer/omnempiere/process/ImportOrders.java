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

import org.compiere.model.I_I_Order;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.Query;
import org.compiere.model.X_I_Order;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cm.itkamer.omnempiere.activemq.ActiveMQClient;

public class ImportOrders extends SvrProcess {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.process.ImportOrders";
	private static int m_ordersProcess = 0;
	private static int m_ordersCount = 0;
	private static String m_test = "N";
	private static String m_brokerUrl = "localhost:61616";
	private static String m_ordersQueue = "Orders";
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
	 *             for ActiveMQ Stomp connection Importing of Orders from POS
	 *             into I_Orders. Then separately from same menu process the
	 *             I_Orders into Sales Orders
	 * 
	 *             Contributor Ing. Tatioti Mbogning Raoul Finish the
	 *             synchronization of all the informations about a Business
	 *             Partner
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
		NodeList records = docEle.getElementsByTagName("detail");

		m_ordersCount += records.getLength();
		for (int i = 0; i < records.getLength(); i++) {
			// check for detail = POS Order only. Other data will be handle in
			// later brackets
			if (!records.item(i).getFirstChild().getTextContent()
					.equals("POS Order"))
				continue;
			X_I_Order order = new X_I_Order(Env.getCtx(), 0, null);

			NodeList details = records.item(i).getChildNodes();
			for (int j = 0; j < details.getLength(); j++) {
				Node n = details.item(j);
				String column = n.getNodeName();

				// get Customer Name
				// Add by Ing Tatioti Mbogning Raoul
				// Finish the synchronisation of all informations about Partner
				if (column.equals(I_I_Order.COLUMNNAME_BPartnerValue))
					order.setBPartnerValue(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_Postal))
					order.setPostal(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_Address1))
					order.setAddress1(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_Address2))
					order.setAddress2(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_Phone))
					order.setPhone(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_City))
					order.setCity(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_EMail))
					order.setEMail(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_RegionName))
					order.setRegionName(n.getTextContent());

				else if (column.equals(I_I_Order.COLUMNNAME_DateOrdered))
					order.setDateOrdered(Timestamp.valueOf(n.getTextContent()));

				else if (column.equals(I_I_Order.COLUMNNAME_AD_Client_ID))
					order.set_ValueOfColumn(I_I_Order.COLUMNNAME_AD_Client_ID,
							Integer.parseInt(n.getTextContent()));

				else if (column.equals("PosLocatorName")) {
					int warehouseID = new Query(Env.getCtx(),
							I_M_Warehouse.Table_Name,
							I_M_Warehouse.COLUMNNAME_Value + "=?", null)
							.setParameters(n.getTextContent()).firstId();
					order.set_CustomColumn(I_I_Order.COLUMNNAME_M_Warehouse_ID,
							warehouseID);
				}

				else if (column.equals(I_I_Order.COLUMNNAME_DocTypeName))
					order.setDocTypeName((n.getTextContent()));

				else if (column.equals(I_I_Order.COLUMNNAME_DocumentNo))
					order.setDocumentNo((n.getTextContent()));

				else if (column.equals(I_I_Order.COLUMNNAME_PriceActual))
					order.setPriceActual(BigDecimal.valueOf(Double
							.parseDouble(n.getTextContent())));

				else if (column.equals(I_I_Order.COLUMNNAME_ProductValue))
					order.setProductValue((n.getTextContent()));

				else if (column.equals(I_I_Order.COLUMNNAME_QtyOrdered))
					order.setQtyOrdered(BigDecimal.valueOf(Double.parseDouble(n
							.getTextContent())));

				// TODO red1 ImportOrder does not do anything with TaxAmt, maybe
				// up to preset Tax_ID
				else if (column.equals(I_I_Order.COLUMNNAME_TaxAmt))
					order.setTaxAmt(BigDecimal.valueOf(Double.parseDouble(n
							.getTextContent())));

				// TODO red1 lookup SalesRep for ID not implemented in
				// ImportOrder, thus..
				else if (column.equals(I_I_Order.COLUMNNAME_SalesRep_ID))
					order.setSalesRep_ID(Integer.parseInt(n.getTextContent()));
				// red1 - ID should match exactly in ERP Server instance.

				String text = n.getTextContent();
				System.out.println("Node =  " + column + " Text = " + text);
			}
			// saves each I_Order line for each Detail XML and increment
			// m_ordersProcess
			// Modified by Ing Tatioti Mbogning Raoul
			if (order.save() && m_test.equals("N"))
				m_ordersProcess++;
			else if (m_test.equals("Y"))
				m_ordersProcess++;
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
			if (name.equals("test"))
				m_test = (String) para[i].getParameter();
			else if (name.equals("ordersQueue"))
				m_ordersQueue = (String) para[i].getParameter();
			else if (name.equals("host"))
				m_brokerUrl = "tcp://" + (String) para[i].getParameter();
			else if (name.equals("port"))
				m_brokerUrl += ":" + (String) para[i].getParameter();
			else if (name.equals("username"))
				m_username = (String) para[i].getParameter();
			else if (name.equals("password"))
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
				.consumeAllMessages(m_ordersQueue);
		if (!messageList.isEmpty()) {
			for (int i = 0; i < messageList.size(); ++i) {
				// Save all Orders or not if it is a test run case
				parseXMLString(((TextMessage) messageList.get(i)).getText());
			}

			// send back all the previous orders in the orders queue
			if (m_test.equals("Y")) {
				for (int i = 0; i < messageList.size(); ++i) {
					mqClient.sendMessage(
							((TextMessage) messageList.get(i)).getText(),
							m_ordersQueue);
				}
			}

			addLog("Import Result");

			if (m_test.equals("Y")) {
				addLog(getProcessInfo().getAD_Process_ID(), new Timestamp(
						System.currentTimeMillis()), new BigDecimal(
						getProcessInfo().getAD_PInstance_ID()),
						"Test run : Success !!!");
			}

			if (m_ordersCount == m_ordersProcess) {
				addLog(getProcessInfo().getAD_Process_ID(), new Timestamp(
						System.currentTimeMillis()), new BigDecimal(
						getProcessInfo().getAD_PInstance_ID()),
						"Imported Orders : " + String.valueOf(m_ordersProcess));

				return "SUCCESS !!! All orders has been imported";
			} else {
				addLog(getProcessInfo().getAD_Process_ID(), new Timestamp(
						System.currentTimeMillis()), new BigDecimal(
						getProcessInfo().getAD_PInstance_ID()),
						"Imported Orders : " + String.valueOf(m_ordersProcess));

				addLog(getProcessInfo().getAD_Process_ID(),
						new Timestamp(System.currentTimeMillis()),
						new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
						"Losted Orders : "
								+ String.valueOf(m_ordersCount
										- m_ordersProcess));
				return "All orders has not been imported correctly. !!!";
			}
		} else {
			return "There is no orders to import.";
		}
	}
}
