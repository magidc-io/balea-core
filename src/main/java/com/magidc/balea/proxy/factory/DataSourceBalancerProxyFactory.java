package com.magidc.balea.proxy.factory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.config.DataSourceConfigurer;
import com.magidc.balea.docker.DockerDBManager;
import com.magidc.balea.proxy.DataSourceBalancerProxy;

import net.sf.cglib.proxy.Enhancer;

/**
 * Factory for data source balancer proxy
 * 
 * @author magidc
 *
 */
public abstract class DataSourceBalancerProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T extends DataSource> T createDataSourceBalancer(Class<T> dataSourceType, Object defaultDataSourceId,
	    DockerDBManager dockerDBManager, DataSourceConfigurer dataSourceConfigurer, Long cacheExpiringTimeMillis)
	    throws DockerException, InterruptedException, IOException, ExecutionException {
	return (T) Enhancer.create(dataSourceType, new DataSourceBalancerProxy(defaultDataSourceId, dockerDBManager,
		dataSourceConfigurer, cacheExpiringTimeMillis));
    }
}
