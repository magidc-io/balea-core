package com.magidc.balea.proxy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.config.DataSourceConfigurer;
import com.magidc.balea.docker.DockerDBManager;
import com.magidc.balea.proxy.cache.DataSourceCacheManager;
import com.magidc.balea.proxy.cache.docker.DockerDataSourceCacheManagerConfigurer;
import com.magidc.balea.proxy.cache.model.ManagedDataSource;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Data source CGLIB proxy that manages multiple data sources and select one
 * according a given data source id
 * 
 * @author magidc
 *
 */
public class DataSourceBalancerProxy implements MethodInterceptor {
    private final DataSourceCacheManager dataSourceCacheManager;
    private DataSource defaultDataSource;
    private DataSourceConfigurer dataSourceConfigurer;

    public DataSourceBalancerProxy(Object defaultDataSourceId, DockerDBManager dockerDBManager,
	    DataSourceConfigurer dataSourceConfigurer, Long cacheExpiringTimeMillis)
	    throws DockerException, InterruptedException, IOException, ExecutionException {
	this.dataSourceCacheManager = new DataSourceCacheManager(
		new DockerDataSourceCacheManagerConfigurer(dockerDBManager), cacheExpiringTimeMillis);
	this.dataSourceConfigurer = dataSourceConfigurer;
	dockerDBManager.stopAndAndRemoveAllContainerDBs();
	// Default DB (should be running all the time
	this.defaultDataSource = dataSourceCacheManager.getManagedDataSource(defaultDataSourceId).getDataSource();
	this.dataSourceCacheManager.getManagedDataSource(defaultDataSourceId);
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
	Object dataSourceId = dataSourceConfigurer.getDataSourceId();

	if (method.getReturnType().equals(Void.TYPE)) {
	    for (ManagedDataSource managedDataSource : dataSourceCacheManager.getAllCached()) {
		methodProxy.invoke(managedDataSource.getDataSource(), args);
	    }
	    return Void.TYPE;
	} else {
	    if (dataSourceId != null) {
		DataSource dataSource = dataSourceCacheManager.getManagedDataSource(dataSourceId).getDataSource();
		return methodProxy.invoke(dataSource, args);
	    } else
		return methodProxy.invoke(defaultDataSource, args);
	}
    }

}