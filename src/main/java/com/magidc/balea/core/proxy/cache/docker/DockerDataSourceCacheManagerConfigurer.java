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
package com.magidc.balea.core.proxy.cache.docker;

import java.io.IOException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.core.container.DataSourceContainerManager;
import com.magidc.balea.core.model.exception.DataSourceNotAvailableException;
import com.magidc.balea.core.proxy.cache.config.DataSourceCacheManagerConfigurer;
import com.magidc.balea.core.proxy.cache.model.ManagedDataSource;

/**
 * Docker based implementation of data source cache manager actions configurer
 * 
 * @author magidc <info@magidc.io>
 *
 */
public class DockerDataSourceCacheManagerConfigurer implements DataSourceCacheManagerConfigurer {

	private DataSourceContainerManager dataSourceContainerManager;

	public DockerDataSourceCacheManagerConfigurer(DataSourceContainerManager dataSourceContainerManager) {
		super();
		this.dataSourceContainerManager = dataSourceContainerManager;
	}

	@Override
	public void closeDataSource(Object dataSourceId) {
		dataSourceContainerManager.stopAndRemoveDataSourceContainer(dataSourceId);
	}

	@Override
	public DataSource obtainDataSource(Object dataSourceId) throws IOException {
		try {
			return dataSourceContainerManager.getDataSource(dataSourceId);
		} catch (DockerException | InterruptedException | DataSourceNotAvailableException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean validateDataSource(ManagedDataSource managedDataSource) throws IOException {
		return dataSourceContainerManager.isDataSourceContainerUp(managedDataSource.getDataSourceId())
				&& dataSourceContainerManager.validateDataSource(managedDataSource.getDataSource());
	}
}
