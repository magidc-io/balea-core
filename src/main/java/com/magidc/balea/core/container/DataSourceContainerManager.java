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
package com.magidc.balea.core.container;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.lang.ArrayUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.magidc.balea.core.container.config.DataSourceContainerParameters;
import com.magidc.balea.core.core.config.DataSourceConfigurer;
import com.magidc.balea.core.model.exception.DataSourceNotAvailableException;

/**
 * Management component for Docker based databases
 * 
 * @author magidc <info@magidc.io>
 *
 */
public class DataSourceContainerManager {
	private DataSourceContainerParameters dataSourceContainerParameters;
	private DataSourceConfigurer dataSourceConfigurer;
	private DockerClient dockerClient;
	private String dockerHost;
	private boolean proxyMode = false;

	public DataSourceContainerManager(DataSourceContainerParameters dataSourceContainerParameters, DataSourceConfigurer dataSourceConfigurer,
			DockerClientConfig dockerClientConfig) {
		this.dataSourceContainerParameters = dataSourceContainerParameters;
		this.dataSourceConfigurer = dataSourceConfigurer;
		this.proxyMode = dataSourceContainerParameters.usesDockerProxy();
		this.dockerHost = dockerClientConfig.getDockerHost().getHost();
		this.dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
	}

	public void closeDockerClient() throws IOException {
		if (dockerClient != null)
			dockerClient.close();
	}

	private String createContainerName(Object dataSourceId) {
		return String.format("%s%s", dataSourceContainerParameters.getContainerNamePrefix(), dataSourceId.toString());
	}

	private DataSource createDataSource(String containerId) {
		int port = proxyMode ? findPortBinding(containerId) : dataSourceContainerParameters.getPort();
		String host = proxyMode ? dockerHost : findContainerIP(containerId);
		return dataSourceConfigurer.createDataSource(host, port);
	}

	private List<Bind> createDataVolumeBinds(Object dataSourceId) {
		return Stream.of(dataSourceContainerParameters.getDataVolumes())
				.map(v -> new Bind(dataSourceConfigurer.getDataDirPath(dataSourceId, v), new Volume(v)))
				.collect(Collectors.toList());
	}

	private List<String> createEnv() {
		return dataSourceContainerParameters
				.getEnvironmentVariables().entrySet().stream()
				.map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	private PortBinding createPortBinding() {
		return new PortBinding(
				Binding.bindPort(dataSourceContainerParameters.getPortBindingSupplier().getAvailablePort(getBindedPorts())),
				ExposedPort.tcp(dataSourceContainerParameters.getPort()));
	}

	private Optional<Container> findContainerByName(List<Container> containers, String containerName) {
		String formattedName = getFormatterContainerName(containerName);
		return containers.stream().filter(c -> ArrayUtils.contains(c.getNames(), formattedName)).findAny();
	}

	private Optional<Container> findContainerByName(String containerName) {
		return findContainerByName(dockerClient.listContainersCmd().withShowAll(true).exec(), containerName);
	}

	@SuppressWarnings("deprecation")
	private String findContainerIP(String containerId) {
		return dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
	}

	private int findPortBinding(String containerId) {
		return Integer.parseInt(
				findPortBindings(containerId).get(ExposedPort.tcp(dataSourceContainerParameters.getPort()))[0].getHostPortSpec());
	}

	private Map<ExposedPort, Binding[]> findPortBindings(String containerId) {
		return dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getPorts().getBindings();
	}

	/**
	 * Collecting all linked ports of current containers
	 * 
	 * @author magidc <info@magidc.io>
	 * @return
	 */
	private Collection<Integer> getBindedPorts() {
		return dockerClient.listContainersCmd().withShowAll(true).exec().stream()
				.flatMap(c -> Stream.of(c.getPorts()))
				.map(ContainerPort::getPublicPort)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Gets a data source instance to access to a data base running in a Docker
	 * container. If container is not present, it will be created. If container is
	 * not running, it will be started
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @return
	 * @throws DockerException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws DataSourceNotAvailableException
	 */
	public DataSource getDataSource(Object dataSourceId) throws DockerException, InterruptedException, IOException, DataSourceNotAvailableException {

		String containerName = createContainerName(dataSourceId);
		Container container = findContainerByName(containerName).orElse(null);

		// Container exist but it is not ready to accept connections
		if (container != null && (!isContainerUp(container) || !validateDataSource(createDataSource(container.getId())))) {
			dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
			container = null;
		}

		if (container == null) {
			if (!imageExist())
				dockerClient.pullImageCmd(dataSourceContainerParameters.getImageName()).exec(new PullImageResultCallback()).awaitCompletion();

			HostConfig hostConfig = new HostConfig();
			hostConfig.withBinds(createDataVolumeBinds(dataSourceId));
			if (proxyMode)
				hostConfig.withPortBindings(createPortBinding());

			CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(dataSourceContainerParameters.getImageName())
					.withName(containerName)
					.withHostConfig(hostConfig).withEnv(createEnv());

			CreateContainerResponse createContainerResponse = createContainerCmd.exec();
			dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
			try {
				return waitForActiveDataSource(createDataSource(createContainerResponse.getId()), 0);
			} catch (DataSourceNotAvailableException e) {
				dockerClient.removeContainerCmd(createContainerResponse.getId()).withForce(true).exec();
				throw e;
			}
		}
		return createDataSource(container.getId());
	}

	private String getFormatterContainerName(String containerName) {
		return String.format("/%s", containerName);
	}

	private boolean imageExist() {
		return !dockerClient.listImagesCmd().withImageNameFilter(dataSourceContainerParameters.getImageName()).exec().isEmpty();
	}

	private boolean isContainerUp(Container container) {
		return container.getStatus().startsWith("Up ");
	}

	/**
	 * Checks if the data source corresponds to an active container
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 * @return
	 */
	public boolean isDataSourceContainerUp(Object dataSourceId) {
		Optional<Container> containerOptional = findContainerByName(createContainerName(dataSourceId));
		return containerOptional.isPresent() && isContainerUp(containerOptional.get());
	}

	/**
	 * Stops and removes all managed containers
	 * 
	 * @author magidc <info@magidc.io>
	 */
	public void stopAndAndRemoveAllDataSourceContainers() {
		dockerClient.listContainersCmd().withShowAll(true).exec().stream()
				.filter(c -> c.getNames().length == 1 && c.getNames()[0].startsWith("/" + dataSourceContainerParameters.getContainerNamePrefix()))
				.forEach(c -> dockerClient.removeContainerCmd(c.getId()).withForce(true).exec());
	}

	/**
	 * Stops and remove a running data souce container
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 */
	public void stopAndRemoveDataSourceContainer(Object dataSourceId) {
		Optional<Container> containerOptional = findContainerByName(createContainerName(dataSourceId));
		if (containerOptional.isPresent())
			dockerClient.removeContainerCmd(containerOptional.get().getId()).withForce(true).exec();
	}

	/**
	 * Stops a running data source container
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSourceId
	 */
	public void stopDataSourceContainer(Object dataSourceId) {
		Optional<Container> containerOptional = findContainerByName(createContainerName(dataSourceId));
		if (containerOptional.isPresent())
			dockerClient.stopContainerCmd(containerOptional.get().getId());
	}

	/**
	 * Checks is the data source is active
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSource
	 * @return
	 */
	public boolean validateDataSource(DataSource dataSource) {
		return dataSourceConfigurer.validateDataSource(dataSource);
	}

	/**
	 * Waits until the data source is active
	 * 
	 * @author magidc <info@magidc.io>
	 * @param dataSource
	 * @param waitingTimeMillis
	 * @return
	 * @throws InterruptedException
	 * @throws DataSourceNotAvailableException
	 */
	private DataSource waitForActiveDataSource(DataSource dataSource, long waitingTimeMillis)
			throws InterruptedException, DataSourceNotAvailableException {
		Thread.sleep(1000);
		long newWaitingTimeMillis = waitingTimeMillis;
		while (!validateDataSource(dataSource)) {
			newWaitingTimeMillis += dataSourceContainerParameters.getContainerStartingUpAttempPeriodMillis();
			if (newWaitingTimeMillis > dataSourceContainerParameters.getContainerStartingUpTimeoutMillis())
				throw new DataSourceNotAvailableException();
			Thread.sleep(newWaitingTimeMillis);
		}
		return dataSource;
	}
}
