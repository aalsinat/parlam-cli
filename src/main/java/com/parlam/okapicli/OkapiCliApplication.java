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
	private static final String CMD_LIST_FILTERS = "listfilters";
	private static final String CMD_HELP = "help";

	private static final String CMD_TIKAL_EXTRACT = "-x";
	private static final String CMD_TKL_MERGE = "-m";
	private static final String CMD_TKL_SEGMENTATION = "-s";
	private static final String CMD_TKL_WORDCOUNT = "-wc";
	private static final String CMD_TKL_LIST_FILTERS = "-lfc";


	private List<String> tikalParameters = new ArrayList<>();

	@Autowired
	private OkapiService okapiService;

	public static void main(String[] args) {
		SpringApplication.run(OkapiCliApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		if (args.containsOption("help")) {
			printUsage();
			System.exit(1);
		}

		List<String> actions = args.getOptionValues("operation");
		if ((actions == null) || (actions.size() == 0)) {
			okapiService.displayError("ERR01", "Operation is not provided.");
			System.exit(1);
		}

		String action = (actions != null) && (actions.size() > 0) ? actions.get(0) : "";
		switch (action) {
			case CMD_EXTRACT:
				tikalParameters.add(CMD_TIKAL_EXTRACT);
				tikalParameters.addAll(getExtractionArguments(args));
				okapiService.extract(tikalParameters);
				break;
			case CMD_MERGE:
				tikalParameters.add(CMD_TKL_MERGE);
				tikalParameters.addAll(getMergingArguments(args));
				okapiService.merge(tikalParameters);
				break;
			case CMD_SEGMENTATION:
				tikalParameters.add(CMD_TKL_SEGMENTATION);
				break;
			case CMD_WORDCOUNT:
				tikalParameters.add(CMD_TKL_WORDCOUNT);
				tikalParameters.addAll(getExtractionArguments(args));
				okapiService.wordCount(tikalParameters);
				break;
			case CMD_LIST_FILTERS:
				tikalParameters.add(CMD_TKL_LIST_FILTERS);
				tikalParameters.addAll(getExtractionArguments(args));
				okapiService.show(tikalParameters);
				break;
			default:
				okapiService.displayError("ERR08", "Unknown operation.");
				System.exit(1);
		}
	}

	/**
	 * Gets expected arguments for extraction operation.
	 *
	 * @param args application arguments where to find
	 */
	private List<String> getExtractionArguments(ApplicationArguments args) {
		List<String> extractionArgs = new ArrayList<>();
		manageSourceFileOption(args, extractionArgs);
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

	private void manageSourceFileOption(ApplicationArguments args, List<String> extractionArgs) {
		if (args.containsOption("sourceFile")) {
			if (getOptionValue("sourceFile", args) != null) {
				extractionArgs.add(getOptionValue("sourceFile", args));
			} else {
				okapiService.displayError("ERR03", "sourceFile option must be followed by a file name.");
				System.exit(1);
			}
		}
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
		if (args.getOptionValues(name)
		        .size() > 0) {
			return args.getOptionValues(name)
			           .get(0);
		} else {
			return null;
		}
	}

	private void printUsage() {
		logger.warn("---------------------------------------------------------------------------------------------------");
		logger.warn("");
		logger.warn("Shows this screen: --help");
		logger.warn("");
		//logger.warn("---------------------------------------------------------------------------------------------------");
		logger.warn("Extracts a file to XLIFF (and optionally segment):");
		logger.warn("   --operation=extract --sourceFile=inputFile  [--targetFile=outputFile] [--sourceLanguage=srcLang]");
		logger.warn("      [--targetLanguage=trgLang] [--segmentation=srxFile] [--noCopy] ");
		logger.warn("");
		//logger.warn("---------------------------------------------------------------------------------------------------");
		logger.warn("Merges an XLIFF document back to its original format:");
		logger.warn("   --operation=merge --xliffFile=xliffFileName [--originalFile=originalFileName] ");
		logger.warn("      [--targetFile=targetFileName]");
		logger.warn("");
		//logger.warn("---------------------------------------------------------------------------------------------------");
		logger.warn("Counts word of a file:");
		logger.warn("   --operation=wordcount --sourceFile=inputFile");
		logger.warn("");
		//logger.warn("---------------------------------------------------------------------------------------------------");
		logger.warn("Lists all available filters:");
		logger.warn("   --operation=listfilters");
		logger.warn("");
		logger.warn("---------------------------------------------------------------------------------------------------");
	}
}