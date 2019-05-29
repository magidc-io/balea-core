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
package com.magidc.balea.core.proxy.cache.config;

import java.io.IOException;

import javax.sql.DataSource;

import com.magidc.balea.core.proxy.cache.model.ManagedDataSource;

/**
 * Definition of basic data source related actions required by cache manager
 * 
 * @author magidc <info@magidc.io>
 *
 */
public interface DataSourceCacheManagerConfigurer {

	/**
	 * Action to be perform when data source stops to be managed by cache
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @throws IOException
	 */
	public void closeDataSource(Object dataSourceId) throws IOException;

	/**
	 * Given a data source id, produces a ready to use data source instance
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @return
	 * @throws IOException
	 */
	public DataSource obtainDataSource(Object dataSourceId) throws IOException;

	/**
	 * Given a data source, check its availability
	 * 
	 * @author magidc <info@magidc.io>
	 * @param managedDataSource
	 * @return
	 * @throws IOException
	 */
	boolean validateDataSource(ManagedDataSource managedDataSource) throws IOException;

}
