package com.magidc.balea.docker.config;

public class DockerDBParameters {
    private String imageName;
    private String[] dockerDBDataVolumes;
    private int port;
    private PortBindingSupplier portBindingSupplier;

    public String getImageName() {
	return imageName;
    }

    public void setImageName(String imageName) {
	this.imageName = imageName;
    }

    public int getPort() {
	return port;
    }

    public void setPort(int port) {
	this.port = port;
    }

    public String[] getDockerDBDataVolumes() {
	return dockerDBDataVolumes;
    }

    public void setDockerDBDataVolumes(String[] dockerDBDataVolumes) {
	this.dockerDBDataVolumes = dockerDBDataVolumes;
    }

    public PortBindingSupplier getPortBindingSupplier() {
	return portBindingSupplier;
    }

    public boolean usesDockerProxy() {
	return portBindingSupplier != null;
    }

    /**
     * Supplier of ports available to be bind with new Docker containers
     * 
     * @author magidc
     * @param portBindingSupplier
     */
    public void useDockerProxy(PortBindingSupplier portBindingSupplier) {
	this.portBindingSupplier = portBindingSupplier;
    }

}
