package cm.itkamer.omnempiere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public interface I_OmnEmpiereChannel {

	// AccessLevel = 6 - System - Client
	public BigDecimal accessLevel = BigDecimal.valueOf(6);

	public static final int Table_ID = 1000002;

	// The table name
	public static final String Table_Name = "OmnEmpiere_Channel";

	// The channel ID
	public static final String COLUMNNAME_OmnEmpiere_Channel_ID = "OmnEmpiere_Channel_ID";

	// Get channel ID
	public int getOmnEmpiereChannelID();

	// Set channel ID
	public void setOmnEmpiereChannelID(int ID);

	/** Column name AD_Client_ID */
	public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/**
	 * Get Client. Client/Tenant for this installation.
	 */
	public int getAD_Client_ID();

	/** Column name AD_Org_ID */
	public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/**
	 * Set Organization. Organizational entity within client
	 */
	public void setAD_Org_ID(int AD_Org_ID);

	/**
	 * Get Organization. Organizational entity within client
	 */
	public int getAD_Org_ID();

	/** Column name Created */
	public static final String COLUMNNAME_Created = "Created";

	/**
	 * Get Created. Date this record was created
	 */
	public Timestamp getCreated();

	/** Column name CreatedBy */
	public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/**
	 * Get Created By. User who created this records
	 */
	public int getCreatedBy();

	// Tell if the channel is active or not
	public static final String COLUMNNAME_IsActive = "IsActive";

	// Check if the channel is active or not
	public boolean isActive();

	// The channel if active or not
	public void setIsActive(boolean isActive);

	/** Column name Updated */
	public static final String COLUMNNAME_Updated = "Updated";

	/**
	 * Get Updated. Date this record was updated
	 */
	public Timestamp getUpdated();

	/** Column name UpdatedBy */
	public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/**
	 * Get Updated By. User who updated this records
	 */
	public int getUpdatedBy();

	// The name of the channel
	public static final String COLUMNNAME_NAME = "Name";

	// Get channel name
	public String getName();

	// Set channel name
	public void setName(String name);

	/** Column name Value */
	public static final String COLUMNNAME_Value = "Value";

	/**
	 * Set Search Key. Search key for the record in the format required - must
	 * be unique
	 */
	public void setValue(String Value);

	/**
	 * Get Search Key. Search key for the record in the format required - must
	 * be unique
	 */
	public String getValue();

	// Tell if the channel has multiples stations
	public static final String COLUMNNAME_HasMultiplesStations = "HasMultiplesStations";

	// Check if the channel has multiples stations
	public boolean hasMultiplesStation();

	// The channel has multiples stations or not
	public void setHasMultiplesStations(boolean hasMultiplesStations);
}
