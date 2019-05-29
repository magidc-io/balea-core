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
package com.magidc.balea.core.proxy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.core.container.DataSourceContainerManager;
import com.magidc.balea.core.core.config.DataSourceConfigurer;
import com.magidc.balea.core.proxy.cache.DataSourceCacheManager;
import com.magidc.balea.core.proxy.cache.docker.DockerDataSourceCacheManagerConfigurer;

import javassist.util.proxy.MethodHandler;

/***
 * Data source proxy that manages multiple data sources and select one according
 * a given data source id
 * 
 * @author magidc <info@magidc.io>
 **/
public class DataSourceMethodHandler implements MethodHandler {
	private final DataSourceCacheManager dataSourceCacheManager;
	private DataSource defaultDataSource;
	private DataSourceConfigurer dataSourceConfigurer;

	public DataSourceMethodHandler(Object defaultDataSourceId, DataSourceContainerManager dataSourceContainerManager, DataSourceConfigurer dataSourceConfigurer,
			Long cacheExpiringTimeMillis)
			throws DockerException, InterruptedException, IOException, ExecutionException {
		this.dataSourceCacheManager = new DataSourceCacheManager(new DockerDataSourceCacheManagerConfigurer(dataSourceContainerManager), cacheExpiringTimeMillis);
		this.dataSourceConfigurer = dataSourceConfigurer;
		dataSourceContainerManager.stopAndAndRemoveAllDataSourceContainers();
		// Default DB (should be running all the time
		this.defaultDataSource = dataSourceCacheManager.getManagedDataSource(defaultDataSourceId).getDataSource();
		this.dataSourceCacheManager.getManagedDataSource(defaultDataSourceId);
	}

	@Override
	public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
		Object dataSourceId = dataSourceConfigurer.getDataSourceId();
		if (dataSourceId != null)
			return method.invoke(dataSourceCacheManager.getManagedDataSource(dataSourceId).getDataSource(), args);
		return method.invoke(defaultDataSource, args);
	}
}