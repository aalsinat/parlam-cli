package com.parlam.okapicli.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExtractionService {
	private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);

	public ExtractionService() {
	}

	public void extract(List<String> tikalParameters) {
		TikalWrapper.process(tikalParameters.toArray(new String[tikalParameters.size()]));
	}
}
