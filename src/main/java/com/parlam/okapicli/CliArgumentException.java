package com.parlam.okapicli;

import net.sf.okapi.common.exceptions.OkapiException;

public class CliArgumentException extends OkapiException {
	String errorCode;

	public CliArgumentException(String message, String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
}
