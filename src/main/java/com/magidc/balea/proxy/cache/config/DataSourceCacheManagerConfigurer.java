package com.magidc.balea.proxy.cache.config;

import java.io.IOException;

import javax.sql.DataSource;

import com.magidc.balea.proxy.cache.model.ManagedDataSource;

/**
 * Definition of basic data source related actions required by cache manager
 * 
 * @author magidc
 *
 */
public interface DataSourceCacheManagerConfigurer {

    /**
     * Given a data source id, produces a ready to use data source instance
     * 
     * @author magidc
     * @param dataSourceId
     * @return
     * @throws IOException
     */
    public DataSource obtainDataSource(Object dataSourceId) throws IOException;

    /**
     * Given a data source, check its availability
     * 
     * @author magidc
     * @param managedDataSource
     * @return
     * @throws IOException
     */
    boolean validateDataSource(ManagedDataSource managedDataSource) throws IOException;

    /**
     * Action to be perform when data source stops to be managed by cache
     * 
     * @author magidc
     * @param dataSourceId
     * @throws IOException
     */
    public void closeDataSource(Object dataSourceId) throws IOException;

}
