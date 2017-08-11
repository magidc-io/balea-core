package com.magidc.balea.config;

import javax.sql.DataSource;

/**
 * 
 * @author magidc
 *
 */
public interface DataSourceConfigurer {
    /**
     * Given a valid URL, produces a data source object
     * 
     * @author magidc
     * @param url
     * @param port
     * @return
     */
    public DataSource createDataSource(String url, int port);

    /**
     * Solves the location of data bases data directory basing in the given data
     * source id
     * 
     * @author magidc
     * @param dataSourceId
     * @param dataBaseDataVolume
     * @return
     */
    public String getDataDirPath(Object dataSourceId, String dataBaseDataVolume);

    /**
     * Obtains the identifier of data source to be used when selecting the real
     * data source inside load balancer
     * 
     * @author magidc
     * @return
     */
    public Object getDataSourceId();

    /**
     * Checks and wait until the data source is active
     * 
     * @author magidc
     * @param dataSource
     * @return
     */
    public boolean validateDataSource(DataSource dataSource);

}
