package com.magidc.balea.proxy.cache.model;

import java.util.Date;

import javax.sql.DataSource;

/**
 * Managed data source holder
 * 
 * @author magidc
 *
 */
public class ManagedDataSource {
    private Object dataSourceId;
    private DataSource dataSource;
    private Date lastAccess = new Date();
    private Date addedOn = new Date();

    public ManagedDataSource(Object dataSourceId, DataSource dataSource) {
	super();
	this.dataSource = dataSource;
	this.dataSourceId = dataSourceId;
    }

    public DataSource getDataSource() {
	return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
    }

    public Date getLastAccess() {
	return lastAccess;
    }

    public void setLastAccess(Date lastAccess) {
	this.lastAccess = lastAccess;
    }

    public Date getAddedOn() {
	return addedOn;
    }

    public void setAddedOn(Date addedOn) {
	this.addedOn = addedOn;
    }

    public Object getDataSourceId() {
	return dataSourceId;
    }

    public void setDataSourceId(Object dataSourceId) {
	this.dataSourceId = dataSourceId;
    }
}
