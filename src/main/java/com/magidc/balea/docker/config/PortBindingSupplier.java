package com.magidc.balea.docker.config;

import java.util.Collection;

public interface PortBindingSupplier {
    public int getAvailablePort(Collection<Integer> dockerPortBindingsInUse);
}
