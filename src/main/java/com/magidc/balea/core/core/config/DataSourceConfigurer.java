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
package com.magidc.balea.core.core.config;

import javax.sql.DataSource;

/**
 * 
 * @author magidc <info@magidc.io>
 *
 */
public interface DataSourceConfigurer {
	/**
	 * Creates a data source instance
	 * 
	 * @author magidc <info@magidc.io>
	 * @param url
	 * @param port
	 * @return
	 */
	public DataSource createDataSource(String host, int port);

	/**
	 * Solves the location of data directory basing in the given data source id
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @param dataSourceContainerDataVolume
	 *            Container internal location of data directory
	 * @return
	 */
	public String getDataDirPath(Object dataSourceId, String dataSourceContainerDataDirPath);

	/**
	 * Obtains the identifier of data source to be used to route the request in the
	 * gateway
	 * 
	 * @author magidc <info@magidc.io>
	 * @return
	 */
	public Object getDataSourceId();

	/**
	 * Checks if the data source is active
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSource
	 * @return
	 */
	public boolean validateDataSource(DataSource dataSource);

}
