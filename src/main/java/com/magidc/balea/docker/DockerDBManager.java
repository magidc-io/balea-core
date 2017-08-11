package com.magidc.balea.docker;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.magidc.balea.config.DataSourceConfigurer;
import com.magidc.balea.docker.config.DockerDBParameters;
import com.magidc.balea.model.exception.DataSourceNotAvailableException;

/**
 * Management component for Docker based databases
 * 
 * @author magidc
 *
 */
public class DockerDBManager
    {
    private static final int DOCKER_CONTAINER_DB_STARTING_UP_ATTEMP_PERIOD_MILLIS = 1000;
    private static final int DOCKER_CONTAINER_DB_STARTING_UP_TIMEOUT_MILLIS = 5000;
    private static final String DOCKER_CONTAINER_NAME_PATTERN = "managed_%s";
    private DockerDBParameters dockerDBParameters;
    private DataSourceConfigurer dataSourceConfigurer;
    private DockerClient dockerClient;
    private URI dockerHost;
    private boolean proxyMode = false;

    /**
     * @author magidc
     * @param dockerDBParameters
     * @param dataSourceConfigurer
     * @param dockerClientConfig
     */
    public DockerDBManager(DockerDBParameters dockerDBParameters, DataSourceConfigurer dataSourceConfigurer,
	    DockerClientConfig dockerClientConfig)
	{
	this.dockerDBParameters = dockerDBParameters;
	this.dataSourceConfigurer = dataSourceConfigurer;
	this.proxyMode = dockerDBParameters.usesDockerProxy();
	this.dockerHost = dockerClientConfig.getDockerHost();

	dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
	}

    public void closeDockerClient() throws IOException
	{
	if (dockerClient != null)
	    dockerClient.close();
	}

    private List<Bind> createDataVolumeBinds(Object dataSourceId)
	{
	List<Bind> binds = new ArrayList<Bind>(dockerDBParameters.getDockerDBDataVolumes().length);
	for (String dockerDBDataVolume : dockerDBParameters.getDockerDBDataVolumes())
	    {
	    binds.add(new Bind(dataSourceConfigurer.getDataDirPath(dataSourceId, dockerDBDataVolume),
		    new Volume(dockerDBDataVolume)));
	    }

	return binds;
	}

    private PortBinding createPortBinding()
	{
	return new PortBinding(
		Binding.bindPort(dockerDBParameters.getPortBindingSupplier().getAvailablePort(getBindedPorts())),
		ExposedPort.tcp(dockerDBParameters.getPort()));
	}

    /**
     * Collecting all linked ports of current containers
     * 
     * @author magidc
     * @return
     */
    private Collection<Integer> getBindedPorts()
	{
	Set<Integer> bindedPorts = new HashSet<Integer>();
	for (Container container : dockerClient.listContainersCmd().withShowAll(true).exec())
	    {
	    for (ContainerPort containerPort : container.getPorts())
		{
		if (containerPort.getPublicPort() != null)
		    bindedPorts.add(containerPort.getPublicPort());
		}
	    }
	return bindedPorts;
	}

    /**
     * Gets a data source to connect to a data base based on Docker container. If container is not present, it will created. If container is not running, it will be started
     * 
     * @author magidc
     * @param dataSourceId
     * @return
     * @throws DockerException
     * @throws InterruptedException
     * @throws IOException
     * @throws DataSourceNotAvailableException
     */
    public DataSource getContainerDBDataSource(Object dataSourceId)
	    throws DockerException, InterruptedException, IOException, DataSourceNotAvailableException
	{

	String containerName = createContainerName(dataSourceId);
	Container container = findContainerByName(containerName);

	// Container exist but it is not ready to accept connections
	if (container != null
		&& (!isContainerUp(container) || !validateDataSource(getContainerDBDataSource(container.getId()))))
	    {
	    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
	    container = null;
	    }

	if (container == null)
	    {
	    if (!imageExist())
		dockerClient.pullImageCmd(dockerDBParameters.getImageName()).exec(new PullImageResultCallback())
			.awaitSuccess();

	    CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(dockerDBParameters.getImageName())
		    .withName(containerName).withBinds(createDataVolumeBinds(dataSourceId));
	    if (proxyMode)
		createContainerCmd = createContainerCmd.withPortBindings(createPortBinding());

	    CreateContainerResponse createContainerResponse = createContainerCmd.exec();
	    dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
	    try
		{
		return waitForActiveDataSource(getContainerDBDataSource(createContainerResponse.getId()), 0);
		}
	    catch (DataSourceNotAvailableException e)
		{
		dockerClient.removeContainerCmd(createContainerResponse.getId()).withForce(true).exec();
		throw e;
		}
	    }

	return getContainerDBDataSource(container.getId());
	}

    /**
     * Checks is the data source corresponds to an active Docker database
     * 
     * @author magidc
     * @param dataSourceId
     * @return
     */
    public boolean isDataSourceContainerDBUp(Object dataSourceId)
	{
	Container container = findContainerByName(createContainerName(dataSourceId));
	return container != null && isContainerUp(container);
	}

    private boolean isContainerUp(Container container)
	{
	return container.getStatus().startsWith("Up ");
	}

    /**
     * Waits until the data source is active
     * 
     * @author magidc
     * @param dataSource
     * @param waitingTimeMillis
     * @return
     * @throws InterruptedException
     * @throws DataSourceNotAvailableException
     */
    private DataSource waitForActiveDataSource(DataSource dataSource, long waitingTimeMillis)
	    throws InterruptedException, DataSourceNotAvailableException
	{
	Thread.sleep(1000);
	long newWaitingTimeMillis = waitingTimeMillis;
	while (!validateDataSource(dataSource))
	    {
	    newWaitingTimeMillis += DOCKER_CONTAINER_DB_STARTING_UP_ATTEMP_PERIOD_MILLIS;
	    if (newWaitingTimeMillis > DOCKER_CONTAINER_DB_STARTING_UP_TIMEOUT_MILLIS)
		throw new DataSourceNotAvailableException();
	    Thread.sleep(newWaitingTimeMillis);
	    }
	return dataSource;
	}

    /**
     * Checks is the data source is active
     * 
     * @author magidc
     * @param dataSource
     * @return
     */
    public boolean validateDataSource(DataSource dataSource)
	{
	return dataSourceConfigurer.validateDataSource(dataSource);
	}

    /**
     * Stops and removes all managed containers
     * 
     * @author magidc
     */
    public void stopAndAndRemoveAllContainerDBs()
	{
	for (Container container : dockerClient.listContainersCmd().withShowAll(true).exec())
	    {
	    if (container.getNames().length == 1 && container.getNames()[0].startsWith("/managed_"))
		dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
	    }
	}

    /**
     * Stops a running database container
     * 
     * @author magidc
     * @param dataSourceId
     */
    public void stopContainerDB(Object dataSourceId)
	{
	dockerClient.stopContainerCmd(findContainerByName(createContainerName(dataSourceId)).getId());
	}

    /**
     * Stops and remove a running database container
     * 
     * @author magidc
     * @param dataSourceId
     */
    public void stopAndRemoveContainerDB(Object dataSourceId)
	{
	Container container = findContainerByName(createContainerName(dataSourceId));
	if (container != null)
	    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
	}

    private DataSource getContainerDBDataSource(String containerId)
	{
	int port = proxyMode ? findPortBinding(containerId) : dockerDBParameters.getPort();
	String host = proxyMode ? dockerHost.getHost() : findContainerIP(containerId);

	return dataSourceConfigurer.createDataSource(host, port);
	}

    @SuppressWarnings("deprecation")
    private String findContainerIP(String containerId)
	{
	return dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
	}

    private int findPortBinding(String containerId)
	{
	return Integer.parseInt(
		findPortBindings(containerId).get(ExposedPort.tcp(dockerDBParameters.getPort()))[0].getHostPortSpec());
	}

    private Map<ExposedPort, Binding[]> findPortBindings(String containerId)
	{
	return dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getPorts().getBindings();
	}

    private Container findContainerByName(String containerName)
	{
	List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
	return containers.isEmpty() ? null : findContainerByName(containers, containerName);
	}

    private Container findContainerByName(List<Container> containers, String containerName)
	{
	String formattedName = getFormatterContainerName(containerName);
	for (Container container : containers)
	    {
	    if (Arrays.asList(container.getNames()).contains(formattedName))
		return container;
	    }
	return null;
	}

    private boolean imageExist()
	{
	return !dockerClient.listImagesCmd().withImageNameFilter(dockerDBParameters.getImageName()).exec().isEmpty();
	}

    private String createContainerName(Object dataSourceId)
	{
	return String.format(DOCKER_CONTAINER_NAME_PATTERN, dataSourceId.toString());
	}

    private String getFormatterContainerName(String containerName)
	{
	return String.format("/%s", containerName);
	}
    }
