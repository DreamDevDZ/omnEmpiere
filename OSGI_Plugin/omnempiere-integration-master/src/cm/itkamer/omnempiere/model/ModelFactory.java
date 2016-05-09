package cm.itkamer.omnempiere.model;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class ModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		if (tableName.equals(I_OmnEmpiereChannel.Table_Name))
			return MOmnEmpiereChannel.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		System.out.println("getPO Record ID : " + Record_ID);
		if (tableName.equals(I_OmnEmpiereChannel.Table_Name))
			return new MOmnEmpiereChannel(Env.getCtx(), Record_ID, trxName);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if (tableName.equals(I_OmnEmpiereChannel.Table_Name))
			return new MOmnEmpiereChannel(Env.getCtx(), rs, trxName);
		return null;
	}
}
