CREATE TABLE adempiere.omnempiere_channel (
	omnempiere_channel_id numeric(10, 0) NOT NULL,
	ad_client_id numeric(10,0) NOT NULL,
	ad_org_id numeric(10,0) NOT NULL,  
	isactive character(1) NOT NULL,
	created timestamp without time zone NOT NULL DEFAULT now(),
	createdby numeric(10,0) NOT NULL,
	updated timestamp without time zone NOT NULL DEFAULT now(),
	updatedby numeric(10,0) NOT NULL,
	name character varying(60) NOT NULL,
	value character varying(60) NOT NULL,
	hasmultiplesstations character(1) NOT NULL,
	CONSTRAINT omnempiere_channel_pkey PRIMARY KEY (omnempiere_channel_id),
	CONSTRAINT omnempiere_channel_client_fk FOREIGN KEY (ad_client_id) REFERENCES adempiere.ad_client (ad_client_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED,
	CONSTRAINT omnempiere_channel_org_fk FOREIGN KEY (ad_org_id) REFERENCES adempiere.ad_org (ad_org_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED
);