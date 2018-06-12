package com.parlam.okapicli.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parlam.okapicli.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ExtractionService {
	private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);
	@Autowired
	private ObjectMapper mapper;

	public ExtractionService() {
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
}
