package com.parlam.okapicli;

import com.parlam.okapicli.services.OkapiService;
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
	private static final String CMD_WORDCOUNT = "wordcount";
	private static final String CMD_NONE = "none";

	private List<String> tikalParameters = new ArrayList<>();

	@Autowired
	private OkapiService okapiService;

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
				okapiService.extract(tikalParameters);
				break;
			case CMD_MERGE:
				tikalParameters.add("-m");
				tikalParameters.addAll(getMergingArguments(args));
				okapiService.merge(tikalParameters);
				break;
			case CMD_SEGMENTATION:
				tikalParameters.add("-s");
				break;
			case CMD_WORDCOUNT:
				tikalParameters.add("-wc");
				tikalParameters.addAll(getExtractionArguments(args));
				okapiService.wordCount(tikalParameters);
				break;
		}
	}

	/**
	 * Gets expected arguments for extraction operation.
	 *
	 * @param args application arguments where to find
	 */
	private List<String> getExtractionArguments(ApplicationArguments args) {
		List<String> extractionArgs = new ArrayList<>();
		if (args.containsOption("sourceFile"))
			extractionArgs.add(getOptionValue("sourceFile", args));
		if (args.containsOption("targetFile"))
			extractionArgs.addAll(Arrays.asList("-tf", getOptionValue("targetFile", args)));
		if (args.containsOption("sourceLanguage"))
			extractionArgs.addAll(Arrays.asList("-sl", getOptionValue("sourceLanguage", args)));
		if (args.containsOption("targetLanguage"))
			extractionArgs.addAll(Arrays.asList("-tl", getOptionValue("targetLanguage", args)));
		if (args.containsOption("segmentation"))
			extractionArgs.addAll(Arrays.asList("-seg", getOptionValue("segmentation", args)));
		if (args.containsOption("noCopy"))
			extractionArgs.addAll(Arrays.asList("-nocopy"));
		return extractionArgs;
	}

	/**
	 * Gets expected arguments for merging operation.
	 *
	 * @param args
	 * @return
	 */
	private List<String> getMergingArguments(ApplicationArguments args) {
		List<String> mergingArgs = new ArrayList<>();
		if (args.containsOption("xliffFile"))
			mergingArgs.add(getOptionValue("xliffFile", args));
		if (args.containsOption("originalFile"))
			mergingArgs.addAll(Arrays.asList("-of", getOptionValue("originalFile", args)));
		if (args.containsOption("targetFile"))
			mergingArgs.addAll(Arrays.asList("-tf", getOptionValue("targetFile", args)));
		if (args.containsOption("sourceLanguage"))
			mergingArgs.addAll(Arrays.asList("-sl", getOptionValue("sourceLanguage", args)));
		if (args.containsOption("targetLanguage"))
			mergingArgs.addAll(Arrays.asList("-tl", getOptionValue("targetLanguage", args)));
		return mergingArgs;
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
