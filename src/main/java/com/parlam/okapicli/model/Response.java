package com.parlam.okapicli.model;

import com.fasterxml.jackson.annotation.*;

@JsonPropertyOrder({"status", "errorCode", "result"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName(value = "ParlamOkapiResponse")
public class Response {
	public static final String OK = "OK";
	public static final String KO = "KO";
	private String status;
	private String result;
	@JsonProperty(value="code")
	private String errorCode;

	public Response() {
		this.status = OK;
	}

	public Response(String status, String result) {
		this.status = status;
		this.result = result;
	}

	public Response(String status, String result, String errorCode) {
		this.status = status;
		this.result = result;
		this.errorCode = errorCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
}
