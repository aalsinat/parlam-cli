package com.parlam.okapicli;

import com.parlam.okapicli.services.ExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class OkapiCliApplication implements ApplicationRunner {
	private static final Logger logger = LoggerFactory.getLogger(OkapiCliApplication.class);
	private static final String CMD_EXTRACT = "extract";
	private static final String CMD_MERGE = "merge";
	private static final String CMD_SEGMENTATION = "segment";
	private static final String CMD_NONE = "none";

	private List<String> tikalParameters = new ArrayList<>();

	@Autowired
	private ExtractionService extractionService;

	public static void main(String[] args) {
		SpringApplication.run(OkapiCliApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
	/*
		Action parameter. Allowed values:
		- extract
		- merge
	 */

		List<String> actions = args.getOptionValues("operation");
		String action = (actions != null) && (actions.size() > 0) ? actions.get(0) : "";
		switch (action) {
			case CMD_EXTRACT:
				tikalParameters.add("-x");
				tikalParameters.addAll(getExtractionArguments(args));
				break;
			case CMD_MERGE:
				tikalParameters.add("-m");
				break;
			case CMD_SEGMENTATION:
				tikalParameters.add("-s");
				break;
		}
		extractionService.extract(tikalParameters);
	}

	/**
	 * Gets expected arguments for extraction operation.
	 *
	 * @param args application arguments where to find
	 */
	private List<String> getExtractionArguments(ApplicationArguments args) {
		List<String> extractionArgs = new ArrayList<>();
		if (args.containsOption("inputFile"))
			extractionArgs.add(getOptionValue("inputFile", args));
		if (args.containsOption("outputFile"))
			extractionArgs.addAll(Arrays.asList("-of", getOptionValue("outputFile", args)));
		if (args.containsOption("sourceLanguage"))
			extractionArgs.addAll(Arrays.asList("-sl", getOptionValue("sourceLanguage", args)));
		if (args.containsOption("targetLanguage"))
			extractionArgs.addAll(Arrays.asList("-tl", getOptionValue("targetLanguage", args)));
		if (args.containsOption("segmentation"))
			extractionArgs.addAll(Arrays.asList("-seg", getOptionValue("segmentation", args)));
		return extractionArgs;
	}

	/**
	 * Method used to get option value when only interested in the first one.
	 *
	 * @param name name of the option to be fetched
	 * @param args application arguments where to look for options
	 * @return first value of specified option
	 */
	private String getOptionValue(String name, ApplicationArguments args) {
		return args.getOptionValues(name)
		           .get(0);
	}
}
