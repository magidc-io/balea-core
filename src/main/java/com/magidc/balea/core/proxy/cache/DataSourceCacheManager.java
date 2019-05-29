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
package com.magidc.balea.core.proxy.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.magidc.balea.core.proxy.cache.config.DataSourceCacheManagerConfigurer;
import com.magidc.balea.core.proxy.cache.model.ManagedDataSource;

/**
 * Manager for DataSource cache
 * 
 * @author magidc <info@magidc.io>
 *
 */
public class DataSourceCacheManager {
	private LoadingCache<Object, ManagedDataSource> dataSourceCache;
	private DataSourceCacheManagerConfigurer dataSourceCacheManagerConfigurer;

	public DataSourceCacheManager(final DataSourceCacheManagerConfigurer dataSourceCacheManagerConfigurer, Long cacheExpiringTimeMillis) {
		this.dataSourceCacheManagerConfigurer = dataSourceCacheManagerConfigurer;
		initCache(cacheExpiringTimeMillis);
	}

	private void cleanUpDataSourceCache() throws IOException {
		for (Entry<Object, ManagedDataSource> dataSourceCacheEntry : dataSourceCache.asMap().entrySet()) {
			if (!dataSourceCacheManagerConfigurer.validateDataSource(dataSourceCacheEntry.getValue()))
				dataSourceCache.invalidate(dataSourceCacheEntry.getKey());
		}
		dataSourceCache.cleanUp();
	}

	/**
	 * Gets all cache entries
	 * 
	 * @author magidc <info@magidc.io>
	 * @return
	 */
	public Collection<ManagedDataSource> getAllCached() {
		return dataSourceCache.asMap().values();
	}

	/**
	 * Gets a data source from the cache if it exists or obtain it from method given
	 * in data source cache configurer
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @return
	 */
	public ManagedDataSource getManagedDataSource(Object dataSourceId) {
		try {
			ManagedDataSource managedDataSource = dataSourceCache.get(dataSourceId);
			if (managedDataSource != null)
				managedDataSource.setLastAccess(new Date());
			return managedDataSource;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Initializes cache
	 * 
	 * @author magidc <info@magidc.io> Data source related actions required by cache manager
	 * @param cacheExpiringTimeMillis
	 *            Cache entries expiring time in milliseconds
	 */
	private void initCache(Long cacheExpiringTimeMillis) {
		CacheLoader<Object, ManagedDataSource> dataSourceCacheLoader;
		dataSourceCacheLoader = new CacheLoader<Object, ManagedDataSource>() {
			@Override
			public ManagedDataSource load(final Object dataSourceId) throws IOException {
				return new ManagedDataSource(dataSourceId, dataSourceCacheManagerConfigurer.obtainDataSource(dataSourceId));
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

		dataSourceCache = CacheBuilder.newBuilder()
				.expireAfterAccess(cacheExpiringTimeMillis, TimeUnit.MILLISECONDS)
				.removalListener(removalListener)
				.build(dataSourceCacheLoader);
		scheduleCleanUpCache();
	}

	/**
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @return
	 */
	public boolean isDataSourceManaged(Object dataSourceId) {
		return dataSourceCache.getIfPresent(dataSourceId) != null;
	}

	/**
	 * Scheduling process to clean up expired entries asynchronous (important to
	 * avoid having unnecessary active docker containers)
	 * 
	 * @author magidc <info@magidc.io>
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
}
