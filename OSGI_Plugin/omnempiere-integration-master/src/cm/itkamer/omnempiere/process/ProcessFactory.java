package cm.itkamer.omnempiere.process;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

/**
 * 
 * @author Ing. Tatioti Mbogning Raoul
 * 
 */
public class ProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		if (className.equals(ExportData2Queues.class.getName()))
			return new ExportData2Queues();
		else if (className.equals(ImportOrders.class.getName()))
			return new ImportOrders();
		else if (className.equals(Products2WebStore.class.getName()))
			return new Products2WebStore();
		else if (className.equals(Product2POS.class.getName()))
			return new Product2POS();
		else if (className.equals(ImportLogs.class.getName()))
			return new ImportLogs();
		else if (className.equals(InventoryUpdate.class.getName()))
			return new InventoryUpdate();
		else if (className.equals(ImportCustumer.class.getName()))
			return new ImportCustumer();
		else
			return null;
	}
}
