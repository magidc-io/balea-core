# Balea

Balea creates a proxy of Java data source, providing dynamic routing to multiple Docker based databases.
Docker is used as manager to create, start or stop database containers on demand and link correspondent data volumes. Thanks to this approach is possible to distribute data across multiple databases keeping active only the necessary ones in each moment maximizing the use of system resources. Java application is completely unaware of this distribution of data as everything is managed behind the data source load balancer.
Also, it makes a standard SQL database like Postgresql or MySQL to be maintained, backed up or versioned with simple file system operations as the data volumes are attached dynamically to the managed Docker containers.
Docker integration allows also to manage database containers in remote hosts.

## Basic usage

#### Adding Maven ependency
```xml
<dependency>
	<groupId>com.magidc</groupId>
	<artifactId>balea</artifactId>
	<version>0.1</version>
</dependency>
```

#### Configuration objects

```java
private DataSourceConfigurer getDataSourceConfigurer() {
	return new DataSourceConfigurer() {
	    @Override
	    public DataSource createDataSource(String url, int port) {
    		DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
    		dataSource.setDriverClassName("org.postgresql.Driver");
    		dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/myDB", url, port));
    		dataSource.setUsername("user");
    		dataSource.setPassword("pwd");
    		dataSource.setMaxActive(100);
    		dataSource.setMinIdle(10);
    		return dataSource;
    	    }

	    @Override
	    public String getDataDirPath(Object dataSourceId, String dockerDBDataVolume) {
		    return findDataFolder(dataSourceId);
	        }
	    
	    @Override
	    public Object getDataSourceId() {
		    return getRequestContextDataModelVersionId();
		    }
		
		@Override
	    public boolean validateDataSource(DataSource dataSource){
			Connection connection;
			try{
				connection = dataSource.getConnection();
				connection.close();
				return true;
				}
			catch (Exception e){
				return false;
				}
			}
	    };
    }

    private DockerDBParameters getDockerDBParameters() {
    	DockerDBParameters dockerDBParameters = new DockerDBParameters();
    	dockerDBParameters.setDockerDBDataVolumes(new String[] { "/var/lib/postgresql/data" });
    	dockerDBParameters.setImageName("postgres:9.4");
    	dockerDBParameters.setPort(5432);
    	dockerDBParameters.useDockerProxy(getPortBindingSupplier());
    
    	return dockerDBParameters;
        }

    private DockerClientConfig getDockerClientConfig() {
    	return DefaultDockerClientConfig.createDefaultConfigBuilder()
    	    .withDockerHost("tcp://1.1.1.1:2376")
    		.withRegistryUrl("https://index.docker.io/v1/")
    		.withRegistryUsername("myDockerHubId")
    		.withRegistryPassword("pwd")
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
    public DataSource dataSourceBalancer() 
        throws DockerException, InterruptedException, IOException, ExecutionException {
	    DataSourceConfigurer dataSourceConfigurer = getDataSourceConfigurer();
	    DockerDBManager dockerDBManager = new DockerDBManager(
	    getDockerDBParameters(), dataSourceConfigurer, getDockerClientConfig());
	    return DataSourceBalancerProxyFactory
	        .createDataSourceBalancer(DataSource.class, 1L, dockerDBManager, dataSourceConfigurer, 60000L);
	    }
```    

License
----

MIT
