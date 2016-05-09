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
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.compiere.model.I_M_Locator;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_StorageOnHand;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_M_StorageOnHand;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import cm.itkamer.omnempiere.activemq.ActiveMQClient;

/**
 * 
 * @author Ing. Nguimmo Tsopmo Boris
 * 
 *         This class exports products from iDempiere to ActiveMQ. It exports
 *         those data for multiple channels like Wanda POS, Prestashop, Magento
 *         and so on.
 * 
 *         For the others channels, we also synchronize data for multiple
 *         stations if the channel has multiple stations. But we add to the
 *         queue name in ActiveMQ the channel's name :
 * 
 *         (POS_Locator_Name + Products_Locator_Name + Channel_Name). So, each
 *         channel will only peek their queue(With the POS_Locator_Name
 *         prepended) and do mutiple synchronization if it only has one station
 *         (Like Magento and Prestashop)
 * 
 *         If the channel has one station, we will use (Customer_Queue_Name +
 *         Channel_Name) for customers queues and (Products_Locator_Name +
 *         Channel_Name) for products queues.
 * 
 *         It is more scalable like that. So we will avoid the uses of a lot of
 *         queue in ActiveMQ and the integration on the others sides(Wanda POS
 *         Magento, Prestashop and so) will be easy to do for example if the
 *         channel has one station.
 * 
 */
public class InventoryUpdate extends SvrProcess {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.process.InventoryUpdate";
	
	private String m_productsQueue = "InvUpdate"; // defaults can be overiden by
													// params

	private String m_brokerUrl = "tcp://localhost:61616";
	private String m_username = "smx";
	private String m_password = "smx";
	private int m_productsExportedCount = 0;

	private ActiveMQClient m_mqClient;
	private List<MProduct> m_products;
	private int m_warehouse_id;

	@SuppressWarnings("unused")
	private List<MWarehouse> m_warehouseList;
	private Timestamp m_date = new Timestamp(0);// '1950-01-01 00:00:00' ;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] parameters = getParameter();
		for (int i = 0; i < parameters.length; i++) {
			String name = parameters[i].getParameterName();
			if (parameters[i].getParameter() == null)
				;
            
			else if (name.equals("date_creation"))
				m_date = parameters[i].getParameterAsTimestamp();

			else if (name.equals(I_M_Warehouse.COLUMNNAME_M_Warehouse_ID))
				m_warehouse_id = parameters[i].getParameterAsInt();
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
		
		//String channel = "InventoryUpdate";
		// Load channels
		

		// Get all warehouses value (eg : HQ)
		m_warehouseList = new Query(Env.getCtx(), I_M_Warehouse.Table_Name,
				I_M_Warehouse.COLUMNNAME_IsActive + " = ? ", get_TrxName())
				.setParameters("Y").list();

		// Load products informations

		loadProductsAndCategory();

		/*
		 * Begin products synchronisation
		 */

		boolean ok = synchronizeProducts();

		if (!ok) {

			m_mqClient.close();
			return "Error while exporting products to queue";
		}

		addLog("Export Results");
		addLog(getProcessInfo().getAD_Process_ID(),
				new Timestamp(System.currentTimeMillis()),
				new BigDecimal(getProcessInfo().getAD_PInstance_ID()),
				"Exported Products : "
						+ String.valueOf(m_productsExportedCount));

		// Close ActiveMQ Client
		m_mqClient.close();
		return "Export Finished";
	}

	private void loadProductsAndCategory() {

		m_products = new ArrayList<MProduct>();

		

			// m_products = new Query(Env.getCtx(), MProduct.Table_Name,
			// MProduct.COLUMNNAME_IsSelfService +
			// " = ? ",get_TrxName()).setParameters(m_isSelfService).list();

			m_products = new Query(Env.getCtx(), I_M_Product.Table_Name,
					        I_M_Product.COLUMNNAME_Updated + " >= '"
							+ m_date.toString() + "' OR "
							+ I_M_Product.COLUMNNAME_Created + " >= '"
							+ m_date.toString() + "'", get_TrxName())
					        .list();

		
	}

	private boolean synchronizeProducts() {

		boolean ok = false;
		String xml = getSingleStationProductsXML();

		ok = m_mqClient.sendMessage(xml, m_productsQueue);

		return ok;
	}

	private String getSingleStationProductsXML() {
		try {
			log.log(Level.SEVERE, "executing the getting products function");
			StringWriter res = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(res);
			
			String name="";
			
		
            
            List<X_M_StorageOnHand> stores2 = new Query(Env.getCtx(),
					I_M_StorageOnHand.Table_Name,
					"",
					get_TrxName()).list();

			
			for (X_M_StorageOnHand store : stores2) {
				MLocator loc = new Query(Env.getCtx(),
						I_M_Locator.Table_Name,
						I_M_Locator.COLUMNNAME_M_Locator_ID + " = ? ",
						null).setParameters(store.getM_Locator_ID()).first();
				
				log.log(Level.SEVERE,
						"Warehouse name : " + loc.getWarehouseName()
								+ " --- selected wareHouse id:"
								+ m_warehouse_id
								+ "---current product warehouse: "
								+ loc.getM_Warehouse_ID());
				
					if (loc.getM_Warehouse_ID() == m_warehouse_id)
						name = loc.getWarehouseName();
				
			}
			
			writer.writeStartDocument();
			writer.writeStartElement("InvUpdate"+name);

			
			m_productsExportedCount = 0;

			log.log(Level.SEVERE, "entering the products details");

	        /* Get Locator ID for Warehouse */
			
            int MLocator_ID = 0;
            MLocator locID = new Query(Env.getCtx(),
            		I_M_Locator.Table_Name,I_M_Locator.COLUMNNAME_M_Warehouse_ID + " = ? ",
            		get_TrxName()).setParameters(m_warehouse_id).first();
            MLocator_ID = locID.getM_Locator_ID();
            
            /* Go through list of products and get quantity */

			for (MProduct product : m_products) {
				log.log(Level.SEVERE, product.getName());
				// Get the price for this product according to the price list
				// getting the attributes
                
				List<X_M_StorageOnHand> stores = new Query(Env.getCtx(),
						I_M_StorageOnHand.Table_Name,
						I_M_StorageOnHand.COLUMNNAME_M_Product_ID + " = ? " + "AND " + I_M_StorageOnHand.COLUMNNAME_M_Locator_ID + " =?",
						get_TrxName()).setParameters(product.getM_Product_ID(),MLocator_ID).list();
				

				int qty = 0;
				boolean islocated = false;
				
				for (X_M_StorageOnHand store : stores) {
					qty = qty + store.getQtyOnHand().intValue();
					MLocator loc = new Query(Env.getCtx(),
							I_M_Locator.Table_Name,
							I_M_Locator.COLUMNNAME_M_Locator_ID + " = ? ",
							get_TrxName()).setParameters(
							store.getM_Locator_ID()).first();
					log.log(Level.SEVERE,
							"Warehouse name : " + loc.getWarehouseName()
									+ " --- selected wareHouse id:"
									+ m_warehouse_id
									+ "---current product warehouse: "
									+ loc.getM_Warehouse_ID());
					if (qty != 0) {
						if (loc.getM_Warehouse_ID() == m_warehouse_id)
							islocated = true;
					}
				}
				if (islocated) {
					// get Warehouse Name from Store Locator to match to POS
					// Locator Name
                    m_productsExportedCount++;
					
                    log.log(Level.SEVERE, "the product is locqted");
                    
                    writer.writeStartElement("Product");

					writer.writeStartElement(I_M_Product.COLUMNNAME_SKU);
					writer.writeCharacters(product.getSKU());
					writer.writeEndElement();

					
					/*writer.writeStartElement(I_M_Product.COLUMNNAME_UPC);
					writer.writeCharacters(product.getUPC());
					writer.writeEndElement();
                    */
					
				
					writer.writeStartElement("QtyOnHand");
					writer.writeCharacters(String.valueOf(qty));
					writer.writeEndElement();

	                 
					
					writer.writeEndElement();// end product

				}

			}

			
			writer.writeEndElement();// </omnempiere Sync>
			writer.writeEndDocument();
			log.log(Level.SEVERE, "here is the result :" + res.toString());
			return res.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}

	}
}
