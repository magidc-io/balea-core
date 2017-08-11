package com.magidc.balea.proxy.cache.docker;

import java.io.IOException;

import javax.sql.DataSource;

import com.github.dockerjava.api.exception.DockerException;
import com.magidc.balea.docker.DockerDBManager;
import com.magidc.balea.model.exception.DataSourceNotAvailableException;
import com.magidc.balea.proxy.cache.config.DataSourceCacheManagerConfigurer;
import com.magidc.balea.proxy.cache.model.ManagedDataSource;

/**
 * Docker based implementation of data source cache manager actions configurer
 * 
 * @author magidc
 *
 */
public class DockerDataSourceCacheManagerConfigurer implements DataSourceCacheManagerConfigurer {

    private DockerDBManager dockerDBManager;

    public DockerDataSourceCacheManagerConfigurer(DockerDBManager dockerDBManager) {
	super();
	this.dockerDBManager = dockerDBManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.magidc.balea.proxy.cache.config.DataSourceCacheManagerConfigurer#
     * obtainDataSource(java.lang.Object)
     */
    @Override
    public DataSource obtainDataSource(Object dataSourceId) throws IOException {
	try {
	    return dockerDBManager.getContainerDBDataSource(dataSourceId);
	} catch (DockerException e) {
	    throw new IOException(e);
	} catch (InterruptedException e) {
	    throw new IOException(e);
	} catch (DataSourceNotAvailableException e) {
	    throw new IOException(e.getMessage());
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.magidc.balea.proxy.cache.config.DataSourceCacheManagerConfigurer#
     * closeDataSource(java.lang.Object)
     */
    @Override
    public void closeDataSource(Object dataSourceId) {
	dockerDBManager.stopAndRemoveContainerDB(dataSourceId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.magidc.balea.proxy.cache.config.DataSourceCacheManagerConfigurer#
     * checkDataSource(com.magidc.balea.proxy.cache.model.ManagedDataSource)
     */
    @Override
    public boolean validateDataSource(ManagedDataSource managedDataSource) throws IOException {
	return dockerDBManager.isDataSourceContainerDBUp(managedDataSource.getDataSourceId())
		&& dockerDBManager.validateDataSource(managedDataSource.getDataSource());
    }

}
