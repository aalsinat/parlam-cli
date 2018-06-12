package com.parlam.okapicli.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parlam.okapicli.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class OkapiService {
	private static final Logger logger = LoggerFactory.getLogger(OkapiService.class);
	@Autowired
	private ObjectMapper mapper;

	public OkapiService() {
	}

	public void extract(List<String> tikalParameters) {
		TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
		try {
			String response = mapper.writeValueAsString(new Response());
			logger.warn(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public void merge(List<String> tikalParameters) {
		TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
		try {
			String response = mapper.writeValueAsString(new Response());
			logger.warn(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	public void wordCount(List<String> tikalParameters) {
		final String wordCount = TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
		try {
			String response = mapper.writeValueAsString(new Response("OK", wordCount));
			logger.warn(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
