# Balea


Balea creates a proxy of javax.sql.DataSource, providing dynamic routing to multiple container based databases following a federated database system architecture.  
Docker is used as manager to create, start or stop database containers on demand and link correspondent data volumes. Thanks to this approach is possible to distribute data across multiple databases keeping active only the necessary ones in each moment maximizing the use of system resources. The distribution and management of data is hidden under the hood, any process that uses this data source interface (i.e. Hibernate) will perceive it as a single source of data.
It makes possible for standard SQL database like Postgresql or MySQL to be maintained, backed up or versioned with simple file system operations as the data volumes are attached dynamically to the managed Docker containers.
Docker integration allows also to manage database containers in remote hosts.

## Basic usage

#### Making Docker API accessible via TCP
Edit file:

```
/lib/systemd/system/docker.service 
```
Modify line with:

```
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:4243
```
#### Adding Maven ependency
```xml
<dependency>
	<groupId>io.magidc</groupId>
	<artifactId>balea</artifactId>
	<version>1.0</version>
</dependency>
```

#### Configuration objects

```java
	private DataSourceConfigurer getDataSourceConfigurer() {
		return new DataSourceConfigurer() {
			@Override
			public DataSource createDataSource(String host, int port) {
				PGSimpleDataSource dataSource = new PGSimpleDataSource();
				dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, POSTGRES_DB));
				dataSource.setUser("user");
				dataSource.setPassword("pass");
				dataSource.setDatabaseName("myDb");

				return dataSource;
			}

			@Override
			public String getDataDirPath(Object dataSourceId, String dataSourceContainerDataDirPath) {
				return findDataFolder(dataSourceId);
			}

			@Override
			public Object getDataSourceId() {
				return getRequestContextDataModelVersionId();
			}

			@Override
			public boolean validateDataSource(DataSource dataSource) {
				Connection connection;
				try {
					connection = dataSource.getConnection();
					connection.close();
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		};
	}	
	
	private DataSourceContainerParameters getDataSourceContainerParameters() {
		DataSourceContainerParameters dataSourceContainerParameters = new DataSourceContainerParameters();
		dataSourceContainerParameters.setDataVolumes(new String[] { "/var/lib/postgresql/data" });
		dataSourceContainerParameters.setImageName("postgres:9.4");
		dataSourceContainerParameters.setPort(5432);
		dataSourceContainerParameters.useDockerProxy(getPortBindingSupplier());
		dataSourceContainerParameters.getEnvironmentVariables().put("POSTGRES_USER", POSTGRES_USER);
		dataSourceContainerParameters.getEnvironmentVariables().put("POSTGRES_PASSWORD", POSTGRES_PASSWORD);
		dataSourceContainerParameters.getEnvironmentVariables().put("POSTGRES_DB", POSTGRES_DB);

		return dataSourceContainerParameters;
	}
	
	
	private DockerClientConfig getDockerClientConfig() {
		return DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://0.0.0.0:4243")
				.withRegistryUrl("https://index.docker.io/v1/")
				.withRegistryUsername("myDockerHubId")
				.withRegistryPassword("pass")
				.withRegistryEmail("me@dockerhub.com")
				.build();
	}

	private PortBindingSupplier getPortBindingSupplier() {
		return new PortBindingSupplier() {
			@Override
			public int getAvailablePort(Collection<Integer> dockerPortBindingsInUse) {
				if (dockerPortBindingsInUse.isEmpty())
					return 5432;
				return Collections.max(dockerPortBindingsInUse) + 1;
			}
		};
	}

```
#### Creating data source balancer
```java
	@Bean
	@Primary
	public DataSource routingDataSource() 
	throws DockerException, InterruptedException, IOException, ExecutionException, 	InstantiationException, IllegalAccessException {
		DataSourceConfigurer dataSourceConfigurer = getDataSourceConfigurer();
		DataSourceContainerManager dataSourceContainerManager = new DataSourceContainerManager(getDataSourceContainerParameters(), dataSourceConfigurer, getDockerClientConfig());
		return RoutingDataSourceFactory.createRoutingDataSource(PGSimpleDataSource.class, 1L, dataSourceContainerManager, dataSourceConfigurer, 60000L);
	}

```
#### License
----

Apache 2.0
