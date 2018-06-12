package com.parlam.okapicli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.parlam.okapicli.services.ExtractionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkapiCliConfiguration {

	@Bean
	ExtractionService extractionService() {
		return new ExtractionService();
	}

	@Bean
	ObjectMapper objectMapper() {
		XmlMapper mapper = new XmlMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper;
	}
}
