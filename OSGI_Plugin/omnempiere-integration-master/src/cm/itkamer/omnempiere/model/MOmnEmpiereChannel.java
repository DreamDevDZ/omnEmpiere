package cm.itkamer.omnempiere.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

public class MOmnEmpiereChannel extends X_OmnEmpiereChannel {

	private static final long serialVersionUID = -5923943545583951368L;

	public MOmnEmpiereChannel(Properties ctx, int M_OmnEmpiere_Channel_ID,
			String trxName) {
		super(ctx, M_OmnEmpiere_Channel_ID, trxName);

		if (M_OmnEmpiere_Channel_ID == 0) {
			setHasMultiplesStations(false);
		}
	}

	public MOmnEmpiereChannel(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public static List<MOmnEmpiereChannel> getChannels(Properties ctx,
			String trxName) {

		List<MOmnEmpiereChannel> channels = new Query(ctx, Table_Name,
				COLUMNNAME_IsActive + " = ?", trxName).setParameters("Y")
				.list();
		return channels;
	}

	public static List<MOmnEmpiereChannel> getChannel(Properties ctx,
			String trxName, int channel_id) {

		List<MOmnEmpiereChannel> channels = new Query(ctx, Table_Name,
				COLUMNNAME_IsActive + " = ? AND "
						+ COLUMNNAME_OmnEmpiere_Channel_ID + " = ?", trxName)
				.setParameters("Y", channel_id).list();
		return channels;
	}
}
