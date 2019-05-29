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
package com.magidc.balea.core.container.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author magidc <info@magidc.io>
 *
 */
public class DataSourceContainerParameters {
	private String imageName;
	private String[] dataVolumes;
	private int port;
	private PortBindingSupplier portBindingSupplier;
	private int containerStartingUpAttempPeriodMillis = 1000;
	private int containerStartingUpTimeoutMillis = 5000;
	private String containerNamePrefix = "managed_";
	private Map<String, String> environmentVariables = new HashMap<String, String>();

	public String getContainerNamePrefix() {
		return containerNamePrefix;
	}

	public int getContainerStartingUpAttempPeriodMillis() {
		return containerStartingUpAttempPeriodMillis;
	}

	public int getContainerStartingUpTimeoutMillis() {
		return containerStartingUpTimeoutMillis;
	}

	public String[] getDataVolumes() {
		return dataVolumes;
	}

	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public String getImageName() {
		return imageName;
	}

	public int getPort() {
		return port;
	}

	public PortBindingSupplier getPortBindingSupplier() {
		return portBindingSupplier;
	}

	public void setContainerNamePrefix(String containerNamePrefix) {
		this.containerNamePrefix = containerNamePrefix;
	}

	public void setContainerStartingUpAttempPeriodMillis(int containerStartingUpAttempPeriodMillis) {
		this.containerStartingUpAttempPeriodMillis = containerStartingUpAttempPeriodMillis;
	}

	public void setContainerStartingUpTimeoutMillis(int containerStartingUpTimeoutMillis) {
		this.containerStartingUpTimeoutMillis = containerStartingUpTimeoutMillis;
	}

	public void setDataVolumes(String[] dataVolumes) {
		this.dataVolumes = dataVolumes;
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setPortBindingSupplier(PortBindingSupplier portBindingSupplier) {
		this.portBindingSupplier = portBindingSupplier;
	}

	/**
	 * Supplier of ports available to be bind with new containers
	 * 
	 * @author magidc <info@magidc.io>
	 * @param portBindingSupplier
	 */
	public void useDockerProxy(PortBindingSupplier portBindingSupplier) {
		this.portBindingSupplier = portBindingSupplier;
	}

	public boolean usesDockerProxy() {
		return portBindingSupplier != null;
	}

}
