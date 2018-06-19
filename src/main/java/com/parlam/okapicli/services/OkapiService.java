package com.parlam.okapicli.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parlam.okapicli.CliArgumentException;
import com.parlam.okapicli.model.Response;
import net.sf.okapi.common.exceptions.OkapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

public class OkapiService {
	private static final Logger logger = LoggerFactory.getLogger(OkapiService.class);
	@Autowired
	private ObjectMapper mapper;

	public OkapiService() {
	}

	public void extract(List<String> tikalParameters) {
		try {
			TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
			String response = mapper.writeValueAsString(new Response());
			logger.warn(response);
		} catch (UnsupportedEncodingException |  MalformedURLException | JsonProcessingException | URISyntaxException e) {
			e.printStackTrace();
		} catch (CliArgumentException e) {
			this.displayError(e.getErrorCode(), e.getMessage());
		}
	}

	public void merge(List<String> tikalParameters) {
		try {
			TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
			String response = mapper.writeValueAsString(new Response());
			logger.warn(response);
		} catch (UnsupportedEncodingException |  MalformedURLException | JsonProcessingException | URISyntaxException e) {
			e.printStackTrace();
		} catch (CliArgumentException e) {
			this.displayError(e.getErrorCode(), e.getMessage());
		}
	}

	public void wordCount(List<String> tikalParameters) {
		try {
			final String wordCount = TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
			String response = mapper.writeValueAsString(new Response("OK", wordCount));
			logger.warn(response);
		} catch (UnsupportedEncodingException |  MalformedURLException | JsonProcessingException | URISyntaxException e) {
			e.printStackTrace();
		} catch (CliArgumentException e) {
			this.displayError(e.getErrorCode(), e.getMessage());
		}
	}

	public void show(List<String> tikalParameters) {
		try {
			TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
		} catch (UnsupportedEncodingException |  MalformedURLException  | URISyntaxException e) {
			e.printStackTrace();
		} catch (CliArgumentException e) {
			this.displayError(e.getErrorCode(), e.getMessage());
		}
	}


	public void displayError(String code, String status) {
		String response = null;
		try {
			response = mapper.writeValueAsString(new Response(Response.KO, status, code));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		logger.warn(response);
	}
}
