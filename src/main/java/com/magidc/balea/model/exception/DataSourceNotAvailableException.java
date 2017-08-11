package com.magidc.balea.model.exception;

public class DataSourceNotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public DataSourceNotAvailableException() {
	super("Data source is not available");
    }

}
