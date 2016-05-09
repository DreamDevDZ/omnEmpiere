package cm.itkamer.omnempiere.modelvalidator;

import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.jfree.util.Log;

import cm.itkamer.omnempiere.model.I_OmnEmpiereChannel;

public class OmnEmpiereModelValidator implements ModelValidator {

	public static final String CLASS_NAME = "cm.itkamer.omnempiere.modelvalidator.OmnEmpiereModelValidator";

	private int m_adClientID;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {

		if (client != null)
			m_adClientID = client.getAD_Client_ID();
		engine.addModelChange(I_OmnEmpiereChannel.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID() {
		return m_adClientID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		Log.info("PO : " + po.toString() + " timing : " + type);
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		return null;
	}

}
