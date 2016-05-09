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

import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.adempiere.util.Callback;
import org.compiere.model.I_AD_Image;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_BP_Group;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_Location;
import org.compiere.model.I_C_TaxCategory;
import org.compiere.model.I_M_Locator;
import org.compiere.model.I_M_PriceList_Version;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_ProductPrice;
import org.compiere.model.I_M_Product_Category;
import org.compiere.model.I_M_StorageOnHand;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.MBPGroup;
import org.compiere.model.MBPartner;
import org.compiere.model.MCountry;
import org.compiere.model.MImage;
import org.compiere.model.MLocation;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductPrice;
import org.compiere.model.MTaxCategory;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_M_StorageOnHand;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import cm.itkamer.omnempiere.activemq.ActiveMQClient;
import cm.itkamer.omnempiere.model.MOmnEmpiereChannel;

/**
 * 
 * @author Ing. Tatioti Mbogning Raoul
 * 
 *         This class exports customers and products from iDempiere to ActiveMQ.
 *         It exports those data for multiple channels like Wanda POS,
 *         Prestashop, Magento and so on.
 * 
 *         For the others channels, we also synchronize data for multiple
 *         stations if the channel has multiple stations. But we add to the
 *         queue name in ActiveMQ the channel's name : (POS_Locator_Name +
 *         Customer_Queue_Name + Channel_Name) and (POS_Locator_Name +
 *         Products_Locator_Name + Channel_Name). So, each channel will only
 *         peek their queue(With the POS_Locator_Name prepended) and do mutiple
 *         synchronization if it only has one station (Like Magento and
 *         Prestashop)
 * 
 *         If the channel has one station, we will use (Customer_Queue_Name +
 *         Channel_Name) for customers queues and (Products_Locator_Name +
 *         Channel_Name) for products queues.
 * 
 *         It is more scalable like that. So we will avoid the uses of a lot of
 *         queue in ActiveMQ and the integration on the others sides(Wanda POS
 *         Magento, Prestasho and so) will be easy to do for example if the
 *         channel has one station.
 * 
 */
public class ExportData2Queues extends SvrProcess {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.process.ExportData2Queues";
	private int m_productsCategoryID;
	private int m_bpartnersGroupID;
	private int m_priceListVersionID;
	private String m_isSelfService = "Y";
	private String m_productsQueue = "Products"; // defaults can be overiden by
													// params
	private String m_customersQueue = "Customers";
	private String m_brokerUrl = "";
	private String m_username = "admin";
	private String m_password = "";
	private String m_eraseQueue = "N";
	private int m_productsExportedCount = 0;
	private int m_customersExportedCount = 0;
	private ActiveMQClient m_mqClient;
	private boolean m_eraseSuccess = false;
	private MProductCategory m_productCategory;
	private List<MProduct> m_products;
	private List<MBPartner> m_bpartners;

	@SuppressWarnings("unused")
	private MBPGroup m_bpartnersGroup;
	private List<MWarehouse> m_warehouseList;
	private List<MOmnEmpiereChannel> m_channels;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] parameters = getParameter();
		for (int i = 0; i < parameters.length; i++) {
			String name = parameters[i].getParameterName();
			if (parameters[i].getParameter() == null)
				;

			else if (name.equals("productsQueue"))
				m_productsQueue = (String) parameters[i].getParameter();

			else if (name.equals("customersQueue"))
				m_customersQueue = (String) parameters[i].getParameter();

			else if (name.equals("host"))
				m_brokerUrl = "tcp://" + (String) parameters[i].getParameter();

			else if (name.equals("port"))
				m_brokerUrl += ":" + (String) parameters[i].getParameter();

			else if (name.equals("username"))
				m_username = (String) parameters[i].getParameter();

			else if (name.equals("password"))
				m_password = (String) parameters[i].getParameter();

			else if (name.equals("isSelfService"))
				m_isSelfService = (String) parameters[i].getParameter();

			else if (name
					.equals(I_M_Product_Category.COLUMNNAME_M_Product_Category_ID))
				m_productsCategoryID = parameters[i].getParameterAsInt();

			else if (name.equals(I_C_BP_Group.COLUMNNAME_C_BP_Group_ID))
				m_bpartnersGroupID = parameters[i].getParameterAsInt();

			else if (name
					.equals(I_M_PriceList_Version.COLUMNNAME_M_PriceList_Version_ID))
				m_priceListVersionID = parameters[i].getParameterAsInt();

			else if (name.equals("eraseQueue"))
				m_eraseQueue = (String) parameters[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		m_mqClient = new ActiveMQClient(m_brokerUrl, m_username, m_password);

		if (!m_mqClient.init())
			return "ActiveMQ Service is turned OFF";

		// Erase queue before synchronise
		if (m_eraseQueue.equals("Y")) {

			this.processUI.ask("Do you realy want to erase queue ? ",
					new Callback<Boolean>() {
						@Override
						public void onCallback(Boolean result) {
							if (result)
								m_eraseQueue = "YY";
						}
					});
		}

		// Load channels
		loadChannels();

		// Get all warehouses value (eg : HQ)
		m_warehouseList = new Query(Env.getCtx(), I_M_Warehouse.Table_Name,
				I_M_Warehouse.COLUMNNAME_IsActive + " = ? ", get_TrxName())
				.setParameters("Y").list();

		// Load customers and products informations
		loadBPartnerAndBPartnerGroup();
		loadProductsAndCategory();

		/*
		 * Begin products synchronisation
		 */

		boolean ok = synchronizeProducts(m_channels);

		if (!ok) {

			m_mqClient.close();
			return "Error while exporting products to queue";
		}

		/*
		 * Begin customers synchronisation
		 */
		ok = synchronizeCustomers(m_channels);

		if (!ok) {

			m_mqClient.close();
			return "Error while exporting customers to queue";
		}

		addLog("Export Results");

		if (m_eraseQueue.equals("YY") && m_eraseSuccess) {
			addLog(getProcessInfo().getAD_Process_ID(),
					new Timestamp(System.currentTimeMillis()), new BigDecimal(
							getProcessInfo().getAD_PInstance_ID()),
					"Erase Queue : Sucess !!!");
		} else if (m_eraseQueue.equals("YY") && !m_eraseSuccess) {
			addLog(getProcessInfo().getAD_Process_ID(),
					new Timestamp(System.currentTimeMillis()), new BigDecimal(
							getProcessInfo().getAD_PInstance_ID()),
					"Erase Queue : Error !!!");
		}

		addLog(getProcessInfo().getAD_Process_ID(),
				new Timestamp(System.currentTimeMillis()),
				new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
				"Exported Products : "
						+ String.valueOf(m_productsExportedCount));

		addLog(getProcessInfo().getAD_Process_ID(),
				new Timestamp(System.currentTimeMillis()),
				new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
				"Exported Customers : "
						+ String.valueOf(m_customersExportedCount));

		// Close ActiveMQ Client
		m_mqClient.close();
		return "Export Finished";
	}

	private void eraseProductsQueues(List<MOmnEmpiereChannel> channels,
			Set<String> keys) {

		for (MOmnEmpiereChannel channel : channels) {

			// If the channel has multiples stations, we prepend the key(POS
			// Locator Name(eg : HQ, Fertilizer ....)) to queue name
			if (channel.hasMultiplesStation()) {

				// Erase products queues
				for (String key : keys) {
					m_eraseSuccess = m_mqClient.eraseQueue(key
							+ m_productsQueue + channel.getName());
				}
			} else {

				// Erase products queues
				m_eraseSuccess = m_mqClient.eraseQueue(m_productsQueue
						+ channel.getName());
			}
		}
	}

	private void eraseCustomersQueues(List<MOmnEmpiereChannel> channels) {

		for (MOmnEmpiereChannel channel : channels) {

			// If the channel has multiples stations, we prepend the key(POS
			// Locator Name(eg : HQ, Fertilizer ....)) to queue name
			if (channel.hasMultiplesStation()) {

				// Erase customers queues
				for (MWarehouse warehouse : m_warehouseList) {
					m_eraseSuccess = m_mqClient.eraseQueue(warehouse.getValue()
							+ m_customersQueue + channel.getName());
				}
			} else {

				// Erase customers queues
				m_eraseSuccess = m_mqClient.eraseQueue(m_customersQueue
						+ channel.getName());
			}
		}
	}

	private void loadChannels() {
		m_channels = MOmnEmpiereChannel.getChannels(getCtx(), get_TrxName());
	}

	private void loadProductsAndCategory() {

		m_products = new ArrayList<MProduct>();
		m_productCategory = null;

		if (m_productsCategoryID > 0) {

			// get products within this category
			m_products = new Query(Env.getCtx(), I_M_Product.Table_Name,
					I_M_Product_Category.COLUMNNAME_M_Product_Category_ID
							+ " = ? AND "
							+ I_M_Product.COLUMNNAME_IsSelfService + " = ? ",
					get_TrxName()).setParameters(m_productsCategoryID,
					m_isSelfService).list();

			// get ProdCategory details i.e. Name
			m_productCategory = new Query(Env.getCtx(),
					I_M_Product_Category.Table_Name,
					I_M_Product_Category.COLUMNNAME_M_Product_Category_ID
							+ " = ? ", get_TrxName()).setParameters(
					m_productsCategoryID).first();
		} else {
			// get all products
			m_products = new Query(Env.getCtx(), I_M_Product.Table_Name,
					I_M_Product.COLUMNNAME_IsSelfService + " = ? ",
					get_TrxName()).setParameters(m_isSelfService).list();
		}
	}

	private void loadBPartnerAndBPartnerGroup() {

		m_bpartners = new ArrayList<MBPartner>();
		m_bpartnersGroup = null;

		if (m_bpartnersGroupID > 0) {

			// get Customers within this category
			m_bpartners = new Query(Env.getCtx(), I_C_BPartner.Table_Name,
					I_C_BPartner.COLUMNNAME_C_BP_Group_ID + " = ? AND "
							+ I_C_BPartner.COLUMNNAME_IsCustomer + " = ? ",
					get_TrxName()).setParameters(m_bpartnersGroupID, "Y")
					.list();

			// model to get related info
			m_bpartnersGroup = new Query(Env.getCtx(), I_C_BP_Group.Table_Name,
					I_C_BP_Group.COLUMNNAME_C_BP_Group_ID + " = ? ", null)
					.setParameters(m_bpartnersGroupID).first();
		} else {
			// Get all customers
			m_bpartners = new Query(Env.getCtx(), I_C_BPartner.Table_Name,
					I_C_BPartner.COLUMNNAME_IsCustomer + " = ? ", get_TrxName())
					.setParameters("Y").list();
		}
	}

	private boolean synchronizeProducts(List<MOmnEmpiereChannel> channels) {

		boolean ok = false;

		// Product XML document for channels which have only one station
		String singleStationChannelProductXML = getSingleStationProductsXML();

		Map<String, String> multiplesStationChannelProductsXML = getMultiplesStationsProductsXML();
		Set<String> posLocatorNameKeys = multiplesStationChannelProductsXML
				.keySet();

		// Erase products queues for each channels
		if (m_eraseQueue.equals("YY")) {
			eraseProductsQueues(m_channels, posLocatorNameKeys);
		}

		// Set channel name on the <type> element
		for (MOmnEmpiereChannel channel : channels) {

			if (channel.hasMultiplesStation()) {

				for (String key : posLocatorNameKeys) {
					String productsXML = multiplesStationChannelProductsXML
							.get(key);

					// Send each productsXML in differents queue for each
					// channel
					ok = m_mqClient.sendMessage(productsXML, key
							+ m_productsQueue + channel.getName());
				}
			} else {
				// Send each productsXML in differents queue for each channel
				ok = m_mqClient.sendMessage(singleStationChannelProductXML,
						m_productsQueue + channel.getName());
			}
		}
		return ok;
	}

	private boolean synchronizeCustomers(List<MOmnEmpiereChannel> channels) {

		m_customersExportedCount = m_bpartners.size();
		String customersXML = getCustomersXML();

		// Erase products queues for each channels
		if (m_eraseQueue.equals("YY")) {
			eraseCustomersQueues(m_channels);
		}

		boolean ok = false;

		// Send customers for each channels
		for (MOmnEmpiereChannel channel : channels) {

			if (channel.hasMultiplesStation()) {

				for (MWarehouse warehouse : m_warehouseList) {
					ok = m_mqClient.sendMessage(
							customersXML,
							warehouse.getValue() + m_customersQueue
									+ channel.getName());
				}
			} else {

				ok = m_mqClient.sendMessage(customersXML, m_customersQueue
						+ channel.getName());
			}
		}
		return ok;
	}

	private String getSingleStationProductsXML() {
		try {
			StringWriter res = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(res);

			writer.writeStartDocument();
			writer.writeStartElement("productsDetails");

			for (MProduct product : m_products) {

				// Get the price for this product according to the price list
				// version selected by the user
				MProductPrice price = new Query(Env.getCtx(),
						I_M_ProductPrice.Table_Name,
						I_M_ProductPrice.COLUMNNAME_M_PriceList_Version_ID
								+ " = ? AND "
								+ I_M_ProductPrice.COLUMNNAME_M_Product_ID
								+ " = ? ", get_TrxName()).setParameters(
						m_priceListVersionID, product.getM_Product_ID())
						.first();

				// If the product does not have this price list version as a
				// price
				// if (price == null)
				// continue;

				// Get the storage on hand we have for this product
				List<X_M_StorageOnHand> stores = new Query(Env.getCtx(),
						I_M_StorageOnHand.Table_Name,
						I_M_StorageOnHand.COLUMNNAME_M_Product_ID + " = ? ",
						null).setParameters(product.getM_Product_ID()).list();

				// No storage on hand for this product
				// if (stores.isEmpty())
				// continue;

				m_productsExportedCount += stores.size();

				for (X_M_StorageOnHand store : stores) {

					// get Warehouse Name from Store Locator to match to POS
					// Locator Name
					MLocator loc = new Query(Env.getCtx(),
							I_M_Locator.Table_Name,
							I_M_Locator.COLUMNNAME_M_Locator_ID + " = ? ",
							get_TrxName()).setParameters(
							store.getM_Locator_ID()).first();

					writer.writeStartElement("detail");
					writer.writeStartElement("DocType");
					writer.writeCharacters(I_M_Product.Table_Name);
					writer.writeEndElement(); // </DocType>

					// Warehouse Name as POS name
					writer.writeStartElement("POSLocatorName");
					writer.writeCharacters(loc.getWarehouseName());
					writer.writeEndElement();

					writer.writeStartElement("ProductName");
					writer.writeCharacters(product.getValue());
					writer.writeEndElement();

					// Storage Data - QtyOnHand
					writer.writeStartElement("QtyOnHand");
					writer.writeCharacters(store.getQtyOnHand().toString());
					writer.writeEndElement();

					// ProductCategory model
					writer.writeStartElement(I_M_Product_Category.COLUMNNAME_M_Product_Category_ID);
					writer.writeCharacters(Integer.toString(product
							.getC_TaxCategory_ID()));
					writer.writeEndElement();

					writer.writeStartElement("CategoryName");
					writer.writeCharacters((m_productCategory == null) ? ""
							: m_productCategory.getName());
					writer.writeEndElement();

					writer.writeStartElement(I_M_Product.COLUMNNAME_M_Product_ID);
					writer.writeCharacters(Integer.toString(product
							.getM_Product_ID()));
					writer.writeEndElement();

					// Tax model i.e. Name, Percentage? (Rate is complex in AD)
					MTaxCategory tax = new Query(Env.getCtx(),
							I_C_TaxCategory.Table_Name,
							I_C_TaxCategory.COLUMNNAME_C_TaxCategory_ID
									+ " = ? ", null).setParameters(
							product.getC_TaxCategory().getC_TaxCategory_ID())
							.first();

					writer.writeStartElement(I_C_TaxCategory.COLUMNNAME_C_TaxCategory_ID);
					writer.writeCharacters(Integer.toString(tax
							.getC_TaxCategory_ID()));
					writer.writeEndElement();

					writer.writeStartElement("TaxName");
					writer.writeCharacters(tax.getName());
					writer.writeEndElement();

					writer.writeStartElement(I_M_Product.COLUMNNAME_UPC);
					writer.writeCharacters(product.getUPC());
					writer.writeEndElement();

					// Pricing info
					writer.writeStartElement(I_M_ProductPrice.COLUMNNAME_PriceList);
					writer.writeCharacters((price != null) ? price
							.getPriceList().toString() : "");
					writer.writeEndElement();

					writer.writeStartElement(I_M_ProductPrice.COLUMNNAME_PriceLimit);
					writer.writeCharacters((price != null) ? price
							.getPriceLimit().toString() : "");
					writer.writeEndElement();

					writer.writeEndElement(); // </detail>
				}
				// Product
			}

			writer.writeEndElement(); // </productsDetails>
			writer.writeEndDocument();

			return res.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}

	private Map<String, String> getMultiplesStationsProductsXML() {
		try {
			Map<String, String> posProductsDetailMap = new HashMap<String, String>();
			for (MProduct product : m_products) {

				// Get the price for this product according to the price list
				// version selected by the user
				MProductPrice price = new Query(Env.getCtx(),
						I_M_ProductPrice.Table_Name,
						I_M_ProductPrice.COLUMNNAME_M_PriceList_Version_ID
								+ " = ? AND "
								+ I_M_ProductPrice.COLUMNNAME_M_Product_ID
								+ " = ? ", get_TrxName()).setParameters(
						m_priceListVersionID, product.getM_Product_ID())
						.first();

				// If the product does not have this price list version as a
				// price
				// if (price == null)
				// continue;

				// Get the storage on hand we have for this product
				List<X_M_StorageOnHand> stores = new Query(Env.getCtx(),
						I_M_StorageOnHand.Table_Name,
						I_M_StorageOnHand.COLUMNNAME_M_Product_ID + " = ? ",
						null).setParameters(product.getM_Product_ID()).list();

				// No storage on hand for this product
				// if (stores.isEmpty())
				// continue;

				// m_productsExportedCount += stores.size();

				for (X_M_StorageOnHand store : stores) {

					// Get Warehouse Name from Store Locator to match to POS
					// Locator Name
					MLocator loc = new Query(Env.getCtx(),
							I_M_Locator.Table_Name,
							I_M_Locator.COLUMNNAME_M_Locator_ID + " = ? ",
							get_TrxName()).setParameters(
							store.getM_Locator_ID()).first();

					StringWriter res = new StringWriter();
					XMLStreamWriter writer = XMLOutputFactory.newInstance()
							.createXMLStreamWriter(res);

					// Store this store in the pos HashMap using his
					// POSLocatorName
					String posProductsDetailMapValue = posProductsDetailMap
							.get(loc.getM_Warehouse().getValue());
					if (posProductsDetailMapValue == null) {
						writer.writeStartDocument();
						writer.writeStartElement("productsDetails");
					}

					writer.writeStartElement("detail");
					writer.writeStartElement("DocType");
					writer.writeCharacters(I_M_Product.Table_Name);
					writer.writeEndElement(); // </DocType>

					// Warehouse Name as POS name
					writer.writeStartElement("POSLocatorName");
					writer.writeCharacters(loc.getWarehouseName());
					writer.writeEndElement();

					writer.writeStartElement("ProductName");
					writer.writeCharacters(product.getValue());
					writer.writeEndElement();

					// Storage Data - QtyOnHand
					writer.writeStartElement("QtyOnHand");
					writer.writeCharacters(store.getQtyOnHand().toString());
					writer.writeEndElement();

					// ProductCategory model
					writer.writeStartElement(I_M_Product_Category.COLUMNNAME_M_Product_Category_ID);
					writer.writeCharacters(Integer.toString(product
							.getC_TaxCategory_ID()));
					writer.writeEndElement();

					writer.writeStartElement("CategoryName");
					writer.writeCharacters((m_productCategory == null) ? ""
							: m_productCategory.getName());
					writer.writeEndElement();

					writer.writeStartElement(I_M_Product.COLUMNNAME_M_Product_ID);
					writer.writeCharacters(Integer.toString(product
							.getM_Product_ID()));
					writer.writeEndElement();

					// Tax model i.e. Name, Percentage? (Rate is complex in AD)
					MTaxCategory tax = new Query(Env.getCtx(),
							I_C_TaxCategory.Table_Name,
							I_C_TaxCategory.COLUMNNAME_C_TaxCategory_ID
									+ " = ? ", null).setParameters(
							product.getC_TaxCategory().getC_TaxCategory_ID())
							.first();

					writer.writeStartElement(I_C_TaxCategory.COLUMNNAME_C_TaxCategory_ID);
					writer.writeCharacters(Integer.toString(tax
							.getC_TaxCategory_ID()));
					writer.writeEndElement();

					writer.writeStartElement("TaxName");
					writer.writeCharacters(tax.getName());
					writer.writeEndElement();

					writer.writeStartElement(I_M_Product.COLUMNNAME_UPC);
					writer.writeCharacters(product.getUPC());
					writer.writeEndElement();

					// Pricing info
					writer.writeStartElement(I_M_ProductPrice.COLUMNNAME_PriceList);
					writer.writeCharacters((price != null) ? price
							.getPriceList().toString() : "");
					writer.writeEndElement();

					writer.writeStartElement(I_M_ProductPrice.COLUMNNAME_PriceLimit);
					writer.writeCharacters((price != null) ? price
							.getPriceLimit().toString() : "");
					writer.writeEndElement();

					writer.writeEndElement();

					posProductsDetailMapValue += res.toString();
					posProductsDetailMap.put(loc.getM_Warehouse().getValue(),
							posProductsDetailMapValue);
				}
			}

			for (String key : posProductsDetailMap.keySet()) {

				// append </productDetails> at the end to have a good XML
				// document
				String productsXML = posProductsDetailMap.get(key);
				posProductsDetailMap.put(key, productsXML
						+ "</productsDetails>");
			}

			return posProductsDetailMap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return new HashMap<String, String>();
		}
	}

	// TODO : Integration of the Business Partners Group(Field m_bpartnersGroup
	// in this class)
	private String getCustomersXML() {

		try {
			StringWriter res = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(res);

			writer.writeStartDocument();
			writer.writeStartElement("customersDetails");

			for (MBPartner partner : m_bpartners) {

				writer.writeStartElement("detail");
				writer.writeStartElement("DocType");
				writer.writeCharacters(I_C_BPartner.Table_Name);
				writer.writeEndElement();

				writer.writeStartElement("CustomerName");
				writer.writeCharacters(partner.getName());
				writer.writeEndElement();

				writer.writeStartElement(I_C_BPartner.COLUMNNAME_Value);
				writer.writeCharacters(partner.getValue());
				writer.writeEndElement();

				writer.writeStartElement(I_C_BPartner.COLUMNNAME_Description);
				writer.writeCharacters(partner.getDescription());
				writer.writeEndElement();

				writer.writeStartElement(I_C_BPartner.COLUMNNAME_C_BPartner_ID);
				writer.writeCharacters(Integer.toString(partner
						.getC_BPartner_ID()));
				writer.writeEndElement();

				writer.writeStartElement(I_C_BPartner.COLUMNNAME_TaxID);
				writer.writeCharacters(partner.getTaxID());
				writer.writeEndElement();

				// Add by Ing Tatioti Mbogning Raoul(tatiotir :
				// tatiotir@itkamer.com)
				// Synchronise partner locations infos
				if (partner.getLocations(false).length != 0) {
					MLocation partnerLocation = partner.getLocations(false)[0]
							.getLocation(false);
					if (partnerLocation != null) {
						MCountry partnerCountry = partnerLocation.getCountry();

						writer.writeStartElement(I_C_Location.COLUMNNAME_Address1);
						writer.writeCharacters(partnerLocation.getAddress1());
						writer.writeEndElement();

						writer.writeStartElement(I_C_Location.COLUMNNAME_Address2);
						writer.writeCharacters(partnerLocation.getAddress2());
						writer.writeEndElement();

						writer.writeStartElement(I_C_Location.COLUMNNAME_City);
						writer.writeCharacters(partnerLocation.getCity());
						writer.writeEndElement();

						writer.writeStartElement(I_C_Location.COLUMNNAME_Postal);
						writer.writeCharacters(partnerLocation.getPostal());
						writer.writeEndElement();

						if (partnerLocation.getRegion() != null) {
							writer.writeStartElement("RegionName");
							writer.writeCharacters(partnerLocation.getRegion()
									.getDescription());
							writer.writeEndElement();
						}

						if (partnerCountry != null) {
							writer.writeStartElement("Country");
							writer.writeCharacters(partnerCountry.getName());
							writer.writeEndElement();
						}
					}
				}

				// Synchronise partner contact info
				if (partner.getContacts(false).length != 0) {
					MUser partnerContact = partner.getContacts(false)[0];

					if (partnerContact != null) {
						writer.writeStartElement(I_AD_User.COLUMNNAME_Name);
						writer.writeCharacters(partnerContact.getName());
						writer.writeEndElement();

						writer.writeStartElement(I_AD_User.COLUMNNAME_EMail);
						writer.writeCharacters(partnerContact.getEMail());
						writer.writeEndElement();

						writer.writeStartElement(I_AD_User.COLUMNNAME_Phone);
						writer.writeCharacters(partnerContact.getPhone());
						writer.writeEndElement();

						writer.writeStartElement(I_AD_User.COLUMNNAME_Phone2);
						writer.writeCharacters(partnerContact.getPhone2());
						writer.writeEndElement();

						writer.writeStartElement(I_AD_User.COLUMNNAME_Fax);
						writer.writeCharacters(partnerContact.getFax());
						writer.writeEndElement();
					}
				}

				// Get partner logo
				if (partner.getLogo_ID() > 0) {
					MImage logo = new Query(Env.getCtx(),
							I_AD_Image.Table_Name,
							I_AD_Image.COLUMNNAME_AD_Image_ID + " = ? ",
							get_TrxName()).setParameters(partner.getLogo_ID())
							.first();

					if (logo != null) {
						writer.writeStartElement("Image");
						writer.writeCharacters(String.valueOf(logo
								.getBinaryData()));
						writer.writeEndElement();
					}
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
			writer.writeEndDocument();
			return res.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}
}
