package com.magidc.balea.proxy.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.magidc.balea.proxy.cache.config.DataSourceCacheManagerConfigurer;
import com.magidc.balea.proxy.cache.model.ManagedDataSource;

/**
 * Manager for DataSource cache
 * 
 * @author magidc
 *
 */
public class DataSourceCacheManager {
    private LoadingCache<Object, ManagedDataSource> dataSourceCache;
    private DataSourceCacheManagerConfigurer dataSourceCacheManagerConfigurer;

    public DataSourceCacheManager(final DataSourceCacheManagerConfigurer dataSourceCacheManagerConfigurer,
	    Long cacheExpiringTimeMillis) {
	this.dataSourceCacheManagerConfigurer = dataSourceCacheManagerConfigurer;
	initCache(cacheExpiringTimeMillis);
    }

    /**
     * Initializes cache
     * 
     * @author magidc Data source related actions required by cache manager
     * @param cacheExpiringTimeMillis
     *            Cache entries expiring time in milliseconds
     */
    private void initCache(Long cacheExpiringTimeMillis) {

	CacheLoader<Object, ManagedDataSource> dataSourceCacheLoader;
	dataSourceCacheLoader = new CacheLoader<Object, ManagedDataSource>() {
	    @Override
	    public ManagedDataSource load(final Object dataSourceId) throws IOException {
		return new ManagedDataSource(dataSourceId,
			dataSourceCacheManagerConfigurer.obtainDataSource(dataSourceId));
	    }
	};

	RemovalListener<Object, ManagedDataSource> removalListener = new RemovalListener<Object, ManagedDataSource>() {
	    @Override
	    public void onRemoval(RemovalNotification<Object, ManagedDataSource> removalNotification) {
		if (removalNotification.wasEvicted()) {
		    try {
			dataSourceCacheManagerConfigurer.closeDataSource(removalNotification.getKey());
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }
	};

	dataSourceCache = CacheBuilder.newBuilder().expireAfterAccess(cacheExpiringTimeMillis, TimeUnit.MILLISECONDS)
		.removalListener(removalListener).build(dataSourceCacheLoader);
	scheduleCleanUpCache();
    }

    /**
     * Scheduling process to clean up expired entries asynchronous (important to
     * avoid having unnecessary active docker containers)
     * 
     * @author magidc
     */
    private void scheduleCleanUpCache() {
	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	scheduler.scheduleAtFixedRate(new Runnable() {
	    @Override
	    public void run() {
		try {
		    cleanUpDataSourceCache();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}, 30, 10, TimeUnit.SECONDS);
    }

    private void cleanUpDataSourceCache() throws IOException {
	for (Entry<Object, ManagedDataSource> dataSourceCacheEntry : dataSourceCache.asMap().entrySet()) {
	    if (!dataSourceCacheManagerConfigurer.validateDataSource(dataSourceCacheEntry.getValue()))
		dataSourceCache.invalidate(dataSourceCacheEntry.getKey());
	}
	dataSourceCache.cleanUp();
    }

    /**
     * Gets a data source from the cache if it exists or obtain it from method
     * given in data source cache configurer
     * 
     * @author magidc
     * @param dataSourceId
     * @return
     * @throws ExecutionException
     */
    public ManagedDataSource getManagedDataSource(Object dataSourceId) throws ExecutionException {
	ManagedDataSource managedDataSource = dataSourceCache.get(dataSourceId);
	if (managedDataSource != null)
	    managedDataSource.setLastAccess(new Date());
	return managedDataSource;
    }

    /**
     * Gets all cache entries
     * 
     * @author magidc
     * @return
     */
    public Collection<ManagedDataSource> getAllCached() {
	return dataSourceCache.asMap().values();
    }

    /**
     * 
     * @author magidc
     * @param dataSourceId
     * @return
     */
    public boolean isDataSourceManaged(Object dataSourceId) {
	return dataSourceCache.getIfPresent(dataSourceId) != null;
    }
}
