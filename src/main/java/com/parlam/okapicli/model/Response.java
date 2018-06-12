package com.parlam.okapicli.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonPropertyOrder({"status", "result"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName(value = "ParlamOkapiResponse")
public class Response {
	private final String OK = "OK";
	private final String KO = "KO";
	private String status;
	private String result;

	public Response() {
		this.status = OK;
	}
	public Response(String status, String result) {
		this.status = status;
		this.result = result;
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
}
