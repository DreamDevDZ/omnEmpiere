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
import java.util.ArrayList;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.adempiere.util.Callback;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cm.itkamer.omnempiere.activemq.ActiveMQClient;

public class ImportLogs extends SvrProcess {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.process.ImportLogs";
	private static int m_ordersProcess = 0;
	private static int m_ordersCount = 0;
	private static String m_test = "N";
	private static String m_brokerUrl = "tcp://localhost:61616";
	private static String m_ordersQueue = "errors";
	private static String m_username = "smx";
	private static String m_password = "smx";
    private String m_message = "";
	
    
	private  void parseXMLString(String message) throws SAXException,
			ParserConfigurationException, IOException {
		// uncomment for testing, together with above
		// message =
		// "<?xml version=\"1.0\"?><entityDetail><type>I_Order</type><BPartnerValue>Joe Block</BPartnerValue><detail><DocTypeName>POS Order</DocTypeName><AD_Client_ID>11</AD_Client_ID><AD_Org_ID>11</AD_Org_ID><DocumentNo>40</DocumentNo><DateOrdered>2011-09-08 14:52:52.152</DateOrdered><ProductValue>Rake-Metal</ProductValue><QtyOrdered>1.0</QtyOrdered><PriceActual>12.0</PriceActual><TaxAmt>0.0</TaxAmt></detail></entityDetail>";
		// Adempiere.startupEnvironment(true);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(message.getBytes()));
		Element docEle = doc.getDocumentElement();
		NodeList records = docEle.getElementsByTagName("error");
		
		m_ordersCount += records.getLength();
		for (int i = 0; i < records.getLength(); i++) {
	         m_message = m_message+"\n"+records.item(i).getTextContent();
	         
		}	
		
	}

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			
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
			this.processUI.ask(m_message,
					new Callback<Boolean>() {
						@Override
						public void onCallback(Boolean result) {
							
								
						}
					});

					} else {
			return "There is no orders to import.";
		}
		return "";
	}
}
