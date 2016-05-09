package cm.itkamer.omnempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

public class X_OmnEmpiereChannel extends PO implements I_OmnEmpiereChannel,
		I_Persistent {

	private static final long serialVersionUID = 3920653671321139204L;

	public X_OmnEmpiereChannel(Properties ctx, int OmnEmpiere_Channel_ID,
			String trxName) {
		super(ctx, OmnEmpiere_Channel_ID, trxName);
	}

	public X_OmnEmpiereChannel(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	public int getOmnEmpiereChannelID() {

		Integer channelID = (Integer) get_Value(COLUMNNAME_OmnEmpiere_Channel_ID);
		if (channelID == null)
			return 0;
		else
			return channelID.intValue();
	}

	@Override
	public void setOmnEmpiereChannelID(int ID) {
		if (ID < 1)
			set_ValueNoCheck(COLUMNNAME_OmnEmpiere_Channel_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_OmnEmpiere_Channel_ID, ID);
	}

	@Override
	public String getName() {
		return (String) get_Value(COLUMNNAME_NAME);
	}

	@Override
	public void setName(String name) {
		set_Value(COLUMNNAME_NAME, name);
	}

	/**
	 * Set Search Key.
	 * 
	 * @param Value
	 *            Search key for the record in the format required - must be
	 *            unique
	 */
	@Override
	public void setValue(String Value) {
		set_Value(COLUMNNAME_Value, Value);
	}

	/**
	 * Get Search Key.
	 * 
	 * @return Search key for the record in the format required - must be unique
	 */
	@Override
	public String getValue() {
		return (String) get_Value(COLUMNNAME_Value);
	}

	@Override
	public boolean hasMultiplesStation() {
		Object oo = get_Value(COLUMNNAME_HasMultiplesStations);
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	@Override
	public void setHasMultiplesStations(boolean hasMultiplesStations) {
		set_Value(COLUMNNAME_HasMultiplesStations,
				Boolean.valueOf(hasMultiplesStations));
	}

	@Override
	protected POInfo initPO(Properties ctx) {
		int omnempiere_channel_ad_table_id = MTable
				.getTable_ID(I_OmnEmpiereChannel.Table_Name);
		POInfo poi = POInfo.getPOInfo(ctx, omnempiere_channel_ad_table_id,
				get_TrxName());
		return poi;
	}

	@Override
	protected int get_AccessLevel() {
		return accessLevel.intValue();
	}
}
