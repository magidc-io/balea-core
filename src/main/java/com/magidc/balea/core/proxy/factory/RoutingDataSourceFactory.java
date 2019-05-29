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
package com.magidc.balea.core.proxy.factory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.core.container.DataSourceContainerManager;
import com.magidc.balea.core.core.config.DataSourceConfigurer;
import com.magidc.balea.core.proxy.DataSourceMethodHandler;

import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

/**
 * Factory for data source gateway
 * 
 * @author magidc <info@magidc.io>
 **/
public abstract class RoutingDataSourceFactory {
	@SuppressWarnings("unchecked")
	public static <T extends DataSource> T createRoutingDataSource(Class<T> dataSourceType, Object defaultDataSourceId, DataSourceContainerManager dataSourceContainerManager,
			DataSourceConfigurer dataSourceConfigurer, Long cacheExpiringTimeMillis)
			throws DockerException, InterruptedException, IOException, ExecutionException, InstantiationException, IllegalAccessException {

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setSuperclass(dataSourceType);
		Class<T> proxyClass = proxyFactory.createClass();
		T proxy = proxyClass.newInstance();
		((Proxy) proxy).setHandler(new DataSourceMethodHandler(defaultDataSourceId, dataSourceContainerManager, dataSourceConfigurer, cacheExpiringTimeMillis));

		return proxy;
	}
}
