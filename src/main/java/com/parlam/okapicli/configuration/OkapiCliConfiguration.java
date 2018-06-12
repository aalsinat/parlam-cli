package com.parlam.okapicli.configuration;

import com.parlam.okapicli.services.ExtractionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkapiCliConfiguration {

	@Bean
	ExtractionService extractionService() {
		return new ExtractionService();
	}
}
