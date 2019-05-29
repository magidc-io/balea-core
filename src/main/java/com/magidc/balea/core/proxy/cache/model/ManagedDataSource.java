/*
 *
 *  Copyright 2019 magidc.io
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package com.magidc.balea.core.proxy.cache.model;

import java.util.Date;

import javax.sql.DataSource;

/**
 * Managed data source holder
 * 
 * @author magidc <info@magidc.io>
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

	public Date getAddedOn() {
		return addedOn;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public Object getDataSourceId() {
		return dataSourceId;
	}

	public Date getLastAccess() {
		return lastAccess;
	}

	public void setAddedOn(Date addedOn) {
		this.addedOn = addedOn;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setDataSourceId(Object dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public void setLastAccess(Date lastAccess) {
		this.lastAccess = lastAccess;
	}
}
