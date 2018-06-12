package com.parlam.okapicli.services;

import net.sf.okapi.applications.tikal.logger.ILogHandler;
import net.sf.okapi.applications.tikal.logger.LogHandlerFactory;
import net.sf.okapi.common.*;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.*;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.filterwriter.XLIFFWriterParameters;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.plugins.PluginsManager;
import net.sf.okapi.common.query.IQuery;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.connectors.apertium.ApertiumMTConnector;
import net.sf.okapi.connectors.bifile.BilingualFileConnector;
import net.sf.okapi.connectors.globalsight.GlobalSightTMConnector;
import net.sf.okapi.connectors.google.GoogleMTv2Connector;
import net.sf.okapi.connectors.lingo24.Lingo24Connector;
import net.sf.okapi.connectors.microsoft.MicrosoftMTConnector;
import net.sf.okapi.connectors.mmt.MMTConnector;
import net.sf.okapi.connectors.mymemory.MyMemoryTMConnector;
import net.sf.okapi.connectors.pensieve.PensieveTMConnector;
import net.sf.okapi.connectors.tda.TDASearchConnector;
import net.sf.okapi.connectors.translatetoolkit.TranslateToolkitTMConnector;
import net.sf.okapi.filters.mosestext.FilterWriterParameters;
import net.sf.okapi.lib.tkit.jarswitcher.VersionManager;
import net.sf.okapi.lib.tkit.step.OriginalDocumentXliffMergerStep;
import net.sf.okapi.lib.tkit.step.SkeletonXliffMergerStep;
import net.sf.okapi.lib.tkit.writer.XLIFFAndSkeletonWriter;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.common.RawDocumentWriterStep;
import net.sf.okapi.steps.formatconversion.FormatConversionStep;
import net.sf.okapi.steps.formatconversion.Parameters;
import net.sf.okapi.steps.formatconversion.TableFilterWriterParameters;
import net.sf.okapi.steps.leveraging.LeveragingStep;
import net.sf.okapi.steps.moses.ExtractionStep;
import net.sf.okapi.steps.moses.MergingParameters;
import net.sf.okapi.steps.moses.MergingStep;
import net.sf.okapi.steps.scopingreport.ScopingReportStep;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import net.sf.okapi.steps.wordcount.CharacterCountStep;
import net.sf.okapi.steps.wordcount.WordCountStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikalWrapper {
	private final static int CMD_EXTRACT = 0;
	private final static int CMD_MERGE = 1;
	private final static int CMD_EDITCONFIG = 2;
	private final static int CMD_QUERYTRANS = 3;
	private final static int CMD_CONV2PO = 4;
	private final static int CMD_CONV2TMX = 5;
	private final static int CMD_CONV2TABLE = 6;
	private final static int CMD_CONV2PEN = 7;
	private final static int CMD_TRANSLATE = 8;
	private final static int CMD_EXTRACTTOMOSES = 9;
	private final static int CMD_LEVERAGEMOSES = 10;
	private final static int CMD_SEGMENTATION = 11;
	private final static int CMD_SHOWCONFIGS = 12;
	// private final static int CMD_ADDTRANS = 13; // No longer supported
	private final static int CMD_REPORT = 14;
	private final static int CMD_WORDCOUNT = 15;
	private static final Logger logger = LoggerFactory.getLogger(TikalWrapper.class);
	private static final String DEFAULT_SEGRULES = "-";
	private static final String MSG_ONLYWITHUICOMP = "UI-based commands are available only in the distributions with UI components.";
	private static final List<String> MERGE_PARAMS = Collections.unmodifiableList(Arrays.asList(
			"-fc", "-ie", "-oe", "-sl", "-tl", "-sd", "-od"
	                                                                                           ));
	private static ILogHandler logHandler;
	private ArrayList<String> inputs;
	private String skeleton;
	private String originalFile;
	private String output;
	private String specifiedConfigId;
	private String specifiedConfigIdPath;
	private String configId;
	private String inputEncoding;
	private String outputEncoding;
	private LocaleId srcLoc;
	private LocaleId trgLoc;
	private int command = -1;
	private String query;
	private boolean useGoogleV2;
	private String googleV2Params;
	private boolean useTransToolkit;
	private String transToolkitParams;
	private boolean useGlobalSight;
	private String globalSightParams;
	private boolean useTDA;
	private String tdaParams;
	private boolean useMyMemory;
	private String myMemoryParams;
	private boolean useApertium;
	private String apertiumParams;
	private boolean usePensieve;
	private String pensieveData;
	private boolean useMicrosoft;
	private String microsoftParams;
	private boolean useBifile;
	private boolean useLingo24;
	private String lingo24Params;
	private boolean useMMT;
	private String mmtUrl;
	private String mmtContext;
	private String bifileData;
	private boolean genericOutput = false;
	private String tableConvFormat;
	private String tableConvCodes;
	private int convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_TARGETOREMPTY;
	private boolean convApprovedEntriesOnly = false;
	private boolean convSkipEntriesWithoutText = true;
	private boolean convOverwrite = false;
	private String segRules;
	private boolean showTraceHint = true;
	private String tmOptions;
	private boolean levOptFillTarget = true;
	private String levOptTMXPath;
	private boolean extOptCopy = true; // Copy source in empty target by default
	private boolean extOptAltTrans = true; // Output alt-trans by default
	private boolean extOptCodeAttrs = false; // Disable extended code attributes by default
	private boolean mosesCopyToTarget = false;
	private boolean mosesOverwriteTarget = false;
	private boolean moses2Outputs = false;
	private boolean mosesUseGModeInAltTrans = true;
	private boolean abortOnFailure = true;
	private String mosesFromPath;
	private String mosesFromPathParam;
	private String mosesToPathParam;
	private String skeletonDir;
	private String outputDir;
	private String targetFilename;
	private String rootDir = System.getProperty("user.dir");
	private ExecutionContext context;
	private boolean newSkel = false;
	private VersionManager versionManager;
	private boolean autoJarVersion = false;
	private String jarVersion = null;
	private String jarPath = null;
	private String configPath = null;
	private FilterConfigurationMapper fcMapper;
	private Hashtable<String, String> extensionsMap;
	private boolean useExternalClassLoader = false; // For now we call a previous Tikal version packaged as a single jar

	private TikalWrapper() {
		inputs = new ArrayList<String>();
		context = new ExecutionContext();
		context.setApplicationName("Tikal");
		context.setIsNoPrompt(true);
	}

	/**
	 * Try the guess the encoding of the console.
	 *
	 * @return the guessed name of the console's encoding.
	 */
	private static String getConsoleEncodingName() {
		String osName = System.getProperty("os.name");
		String enc = null;
		if (osName.contains("OS X")) {
			enc = "UTF-8"; // Apparently the default for bash on Mac
		} else if (osName.startsWith("Windows")) {
			enc = "cp850"; // Not perfect, but covers many languages
		} else {
			// Default: Assumes unique encoding overall
			enc = Charset.defaultCharset()
			             .name();
		}
		// Now check if we have a user setting
		UserConfiguration uc = new UserConfiguration();
		uc.load("Tikal");
		return uc.getProperty("displayEncoding", enc);
	}

	public static String process(String[] originalArgs) {
		StringBuilder sb = new StringBuilder();
		for (String st : originalArgs) {
			sb.append(st);
			sb.append(" ");
		}
		logger.debug(sb.toString());

		TikalWrapper prog = new TikalWrapper();

		boolean useLogger = true;
		boolean showTrace = false;
		try {

			// Remove all empty arguments
			// This is to work around the "$1" issue in bash
			List<String> args = new ArrayList<String>();
			for (String tmp : originalArgs) {
				if (tmp.length() > 0) args.add(tmp);
			}

			// Check early so the option does not get 'eaten' by a bad syntax
			if (args.contains("-trace"))
				showTrace = true;
			if (args.contains("-logger"))
				useLogger = true;

			// Create an encoding-aware output for the console
			// System.out uses the default system encoding that
			// may not be the right one (e.g. windows-1252 vs cp850)
			if (!useLogger) {
				PrintStream ps = new PrintStream(System.out, true, getConsoleEncodingName());
				logHandler = LogHandlerFactory.getLogHandler();
				logHandler.initialize(ps);
				if (showTrace) logHandler.setLogLevel(ILogHandler.LogLevel.TRACE);
			}

			//prog.printBanner();
			if (args.size() == 0) {
				prog.printUsage();
				return null;
			}
			if (args.contains("-?")) {
				prog.printUsage();
				return null; // Overrides all arguments
			}
			if (args.contains("-h") || args.contains("--help") || args.contains("-help")) {
				prog.showHelp();
				return null; // Overrides all arguments
			}
			if (args.contains("-i") || args.contains("--info") || args.contains("-info")) {
				prog.showInfo();
				return null; // Overrides all arguments
			}

			// Set the default resource for the default engine.
			prog.transToolkitParams = "https://amagama-live.translatehouse.org/api/v1/";

			for (int i = 0; i < args.size(); i++) {
				String arg = args.get(i);
				if (arg.equals("-fc")) {
					prog.specifiedConfigId = prog.getArgument(args, ++i);
				} else if (arg.equals("-sl")) {
					prog.srcLoc = LocaleId.fromString(prog.getArgument(args, ++i));
				} else if (arg.equals("-tl")) {
					prog.trgLoc = LocaleId.fromString(prog.getArgument(args, ++i));
				} else if (arg.equals("-ie")) {
					prog.inputEncoding = prog.getArgument(args, ++i);
				} else if (arg.equals("-oe")) {
					prog.outputEncoding = prog.getArgument(args, ++i);
				} else if (arg.equals("-od")) {
					prog.outputDir = prog.getArgument(args, ++i);
				} else if (arg.equals("-tf")) {
					prog.targetFilename = prog.getArgument(args, ++i);
				} else if (arg.equals("-of")) {
					prog.originalFile = prog.getArgument(args, ++i);
				} else if (arg.equals("-sd")) {
					prog.skeletonDir = prog.getArgument(args, ++i);
				} else if (arg.equals("-rd")) {
					prog.rootDir = prog.getArgument(args, ++i);
				} else if (arg.equals("-pd")) {
					// This value will be overridden if -fc is
					// also specified
					prog.specifiedConfigIdPath = prog.getArgument(args, ++i);
				} else if (arg.equals("-x")) { // Default extraction
					prog.command = CMD_EXTRACT;
					prog.newSkel = false;
				} else if (arg.equals("-x1")) { // "Old" extraction
					prog.command = CMD_EXTRACT;
					prog.newSkel = false;
				} else if (arg.equals("-x2")) { // "New" extraction
					prog.command = CMD_EXTRACT;
					prog.newSkel = true;
					logger.warn(
							"Please, note that extracting/merging using the JSON skeleton is deprecated and will be removed soon.");
				} else if (arg.equals("-xm")) {
					prog.command = CMD_EXTRACTTOMOSES;
				} else if (arg.equals("-av")) { // Jar version automatically selected for merge
					prog.autoJarVersion = true;
				} else if (arg.equals("-v")) { // Specific jar version for merge
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.jarVersion = args.get(++i);
						}
					}
				} else if (arg.equals("-vp")) { // Specific jar for merge
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.jarPath = args.get(++i);

							// Check trailing slash for a dir path
							if (!prog.jarPath.endsWith(".jar")) {
								// dir paths not supported now, only jars
								prog.jarPath = Util.ensureSeparator(prog.jarPath, false);
							}
						}
					}
				} else if (arg.equals("-vc")) { // Path to the versions configuration file
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.configPath = args.get(++i);
						}
					}
				} else if (arg.equals("-2")) {
					prog.moses2Outputs = true;
				} else if (arg.equals("-t")) {
					prog.command = CMD_TRANSLATE;
				} else if (arg.equals("-m")) { // Default merge
					prog.command = CMD_MERGE;
					prog.newSkel = false;
				} else if (arg.equals("-m1")) { // "Old" merge
					prog.command = CMD_MERGE;
					prog.newSkel = false;
				} else if (arg.equals("-m2")) { // "New" merge
					prog.command = CMD_MERGE;
					prog.newSkel = true;
					logger.warn(
							"Please, note that extracting/merging using the JSON skeleton is deprecated and will be removed soon.");
				} else if (arg.equals("-lm")) {
					prog.command = CMD_LEVERAGEMOSES;
				} else if (arg.equals("-totrg")) {
					prog.mosesCopyToTarget = true;
					prog.mosesOverwriteTarget = false;
				} else if (arg.equals("-overtrg")) {
					prog.mosesCopyToTarget = true;
					prog.mosesOverwriteTarget = true;
				} else if (arg.equals("-bpt")) {
					prog.mosesUseGModeInAltTrans = false;
				} else if (arg.equals("-over")) {
					prog.convOverwrite = true;
				} else if (arg.equals("-from")) {
					prog.mosesFromPathParam = prog.getArgument(args, ++i);
				} else if (arg.equals("-to")) {
					prog.mosesToPathParam = prog.getArgument(args, ++i);
				} else if (arg.equals("-2po")) {
					prog.command = CMD_CONV2PO;
				} else if (arg.equals("-2tmx")) {
					prog.command = CMD_CONV2TMX;
				} else if (arg.equals("-2tbl")) {
					prog.command = CMD_CONV2TABLE;
				} else if (arg.equals("-csv")) {
					prog.tableConvFormat = "csv";
				} else if (arg.equals("-tab")) {
					prog.tableConvFormat = "tab";
				} else if (arg.equals("-xliff")) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_XLIFF;
				} else if (arg.equals("-xliffgx")) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_XLIFFGX;
				} else if (arg.equals("-tmx")) {
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_TMX;
				} else if (arg.equals("-all")) {
					prog.convSkipEntriesWithoutText = false;
				} else if (arg.equals("-approved")) {
					prog.convApprovedEntriesOnly = true;
				} else if (arg.equals("-nofill")) {
					prog.levOptFillTarget = false;
				} else if (arg.equals("-nocopy")) {
					prog.extOptCopy = false;
				} else if (arg.equals("-noalttrans")) {
					prog.extOptAltTrans = false;
				} else if (arg.equals("-codeattrs")) {
					prog.extOptCodeAttrs = true;
				} else if (arg.equals("-maketmx")) {
					prog.levOptTMXPath = "pretrans.tmx";
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.levOptTMXPath = args.get(++i);
						}
					}
				} else if (arg.equals("-trgsource")) {
					prog.convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_FORCESOURCE;
				} else if (arg.equals("-trgempty")) {
					prog.convTargetStyle = net.sf.okapi.steps.formatconversion.Parameters.TRG_FORCEEMPTY;
				} else if (arg.equals("-imp")) {
					prog.command = CMD_CONV2PEN;
					prog.pensieveData = prog.getArgument(args, ++i);
				} else if (arg.equals("-exp")) {
					prog.command = CMD_CONV2TMX;
					prog.specifiedConfigId = "okf_pensieve";
				} else if (arg.equals("-e")) {
					prog.command = CMD_EDITCONFIG;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.specifiedConfigId = args.get(++i);
						}
					}
				} else if (arg.equals("-generic")) {
					prog.genericOutput = true;
					prog.tableConvCodes = TableFilterWriterParameters.INLINE_GENERIC;
				} else if (arg.equals("-q")) {
					prog.command = CMD_QUERYTRANS;
					prog.query = prog.getArgument(args, ++i);
				} else if (arg.equals("-opt")) {
					prog.tmOptions = prog.getArgument(args, ++i);
				} else if (arg.equals("-gg") || arg.equals("-google")) {
					prog.useGoogleV2 = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.googleV2Params = args.get(++i);
						}
					}
				} else if (arg.equals("-tt")) {
					prog.useTransToolkit = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.transToolkitParams = args.get(++i);
						}
					}
				} else if (arg.equals("-gs")) {
					prog.useGlobalSight = true;
					prog.globalSightParams = prog.getArgument(args, ++i);
				} else if (arg.equals("-tda")) {
					prog.useTDA = true;
					prog.tdaParams = prog.getArgument(args, ++i);
				} else if (arg.equals("-ms")) {
					prog.useMicrosoft = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.microsoftParams = args.get(++i);
						}
					}
				} else if (arg.equals("-apertium")) {
					prog.useApertium = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.apertiumParams = args.get(++i);
						}
					}
				} else if (arg.equals("-mm")) {
					prog.useMyMemory = true;
					// Key is optional (left for backward compatibility)
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.myMemoryParams = prog.getArgument(args, ++i);
						}
					}
				} else if (arg.equals("-pen")) {
					prog.usePensieve = true;
					prog.pensieveData = "http://localhost:8080";
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.pensieveData = args.get(++i);
						}
					}
				} else if (arg.equals("-bi")) {
					prog.useBifile = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.bifileData = args.get(++i);
						}
					}
				} else if (arg.equals("-lingo24")) {
					prog.useLingo24 = true;
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.lingo24Params = args.get(++i);
						}
					}
				} else if (arg.equals("-mmt")) {
					prog.useMMT = true;
					// The URL is a mandatory parameter
					prog.mmtUrl = prog.getArgument(args, ++i);
					// The context is optional
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.mmtContext = args.get(++i);
						}
					}
				} else if (arg.endsWith("-listconf") || arg.equals("-lfc")) {
					prog.command = CMD_SHOWCONFIGS;
				} else if (arg.equals("-s")) {
					prog.command = CMD_SEGMENTATION;
					prog.segRules = DEFAULT_SEGRULES;
				} else if (arg.equals("-seg")) {
					prog.segRules = DEFAULT_SEGRULES; // Default
					if (args.size() > i + 1) {
						if (!args.get(i + 1)
						         .startsWith("-")) {
							prog.segRules = args.get(++i);
						}
					}
				} else if (arg.equals("-trace") || arg.equals("-logger")) {
					// Already set. This is just to avoid warnings about invalid parameters
				} else if (arg.equals("-continue")) {
					prog.abortOnFailure = false;
				} else if (arg.equals("-safe")) {
					prog.context.setIsNoPrompt(false);
				} else if (arg.equals("-sr")) {
					prog.command = CMD_REPORT;
				} else if (arg.equals("-wc")) {
					prog.command = CMD_WORDCOUNT;
				}
				//=== Input file or error
				else if (!arg.startsWith("-")) {
					prog.inputs.add(args.get(i));
				} else {
					prog.showTraceHint = false; // Using trace is not helpful to the user for this error
					throw new InvalidParameterException(
							String.format("Invalid command-line argument '%s'.", args.get(i)));
				}
			}

			// Forgive having the extension .fprm from configuration ID if there is one
			if (prog.specifiedConfigId != null) {
				String cfgPath = Util.getDirectoryName(prog.specifiedConfigId);
				if (!cfgPath.isEmpty()) {
					prog.specifiedConfigIdPath = cfgPath;
					prog.specifiedConfigId = Util.getFilename(prog.specifiedConfigId, true);
				}
				if (prog.specifiedConfigId.endsWith(FilterConfigurationMapper.CONFIGFILE_EXT)) {
					prog.specifiedConfigId = Util.getFilename(prog.specifiedConfigId, false);
				}
			}

			// Check inputs and command
			if (prog.command == -1) {
				logger.warn("No command specified. Please use one of the command described below:");
				prog.printUsage();
				return null;
			}
			if (prog.command == CMD_EDITCONFIG) {
				if (prog.specifiedConfigId == null) {
					prog.editAllConfigurations();
				} else {
					prog.editConfiguration();
				}
				return null;
			}
			if (prog.command == CMD_SHOWCONFIGS) {
				prog.showAllConfigurations();
				return null;
			}
			if (prog.command == CMD_QUERYTRANS) {
				prog.processQuery();
				return null;
			}
			if (prog.command == CMD_WORDCOUNT) {
				final String wordCount = prog.printWordCount();
				return wordCount;
			}
			if (prog.command == CMD_REPORT) {
				prog.printScopingReport();
				return null;
			}
			if (prog.inputs.size() == 0) {
				throw new OkapiException("No input document specified.");
			}

			// Process all input files
			TikalWrapper.Timer timer = new TikalWrapper.Timer();
			int errorCount = 0;
			for (int i = 0; i < prog.inputs.size(); i++) {
				if (i > 0) {
					displayDivider();
				}
				try {
					prog.process(prog.inputs.get(i), args);
				} catch (Throwable e) {
					displayError(e, showTrace, prog.showTraceHint);
					if (prog.abortOnFailure) {
						System.exit(1);
					} else {
						errorCount++;
					}
				}
			}
			if (prog.inputs.size() > 1) {
				displayDivider();
				displaySummary(prog.inputs.size(), errorCount, timer);
			}
		} catch (Throwable e) {
			displayError(e, showTrace, prog.showTraceHint);
			System.exit(1); // Error
		}
		return null;
	}

	private static void displayDivider() {
		logger.info("------------------------------------------------------------"); //$NON-NLS-1$
	}

	private static void displaySummary(int fileCount,
	                                   int errorCount,
	                                   TikalWrapper.Timer t) {
		logger.info("Files: " + fileCount + ", Errors: " + errorCount + ", Time: " + t);
	}

	private static void displayError(Throwable e,
	                                 boolean showTrace,
	                                 boolean showTraceHint) {
		if (showTrace) e.printStackTrace();
		else {
			logger.error(e.getMessage());
			Throwable e2 = e.getCause();
			if (e2 != null) logger.error(e2.getMessage());
			if (showTraceHint) logger.info("You can use the -trace option for more details.");
		}
	}

	protected String getArgument(List<String> args,
	                             int index) {
		if (index >= args.size()) {
			showTraceHint = false; // Using trace is not helpful to the user for this error
			throw new OkapiException(String.format(
					"Missing parameter after '%s'", args.get(index - 1)));
		}
		return args.get(index);
	}

	private void initialize() {
		// Create the mapper and load it with all parameters editor info
		fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, false, true);

		// Instead create a map with extensions -> filter
		extensionsMap = new Hashtable<String, String>();

		extensionsMap.put(".docx", "okf_openxml");
		extensionsMap.put(".pptx", "okf_openxml");
		extensionsMap.put(".xlsx", "okf_openxml");

		extensionsMap.put(".odt", "okf_openoffice");
		extensionsMap.put(".swx", "okf_openoffice");
		extensionsMap.put(".ods", "okf_openoffice");
		extensionsMap.put(".swc", "okf_openoffice");
		extensionsMap.put(".odp", "okf_openoffice");
		extensionsMap.put(".sxi", "okf_openoffice");
		extensionsMap.put(".odg", "okf_openoffice");
		extensionsMap.put(".sxd", "okf_openoffice");

		extensionsMap.put(".htm", "okf_html");
		extensionsMap.put(".html", "okf_html");

		extensionsMap.put(".xlf", "okf_xliff");
		extensionsMap.put(".xlif", "okf_xliff");
		extensionsMap.put(".xliff", "okf_xliff");

		extensionsMap.put(".tmx", "okf_tmx");

		extensionsMap.put(".properties", "okf_properties");
		extensionsMap.put(".lang", "okf_properties-skypeLang");

		extensionsMap.put(".po", "okf_po");

		extensionsMap.put(".xml", "okf_xml");
		extensionsMap.put(".resx", "okf_xml-resx");

		extensionsMap.put(".srt", "okf_regex-srt");

		extensionsMap.put(".dtd", "okf_dtd");
		extensionsMap.put(".ent", "okf_dtd");

		extensionsMap.put(".ts", "okf_ts");

		extensionsMap.put(".txt", "okf_plaintext");

		extensionsMap.put(".csv", "okf_table_csv");

		extensionsMap.put(".ttx", "okf_ttx");

		extensionsMap.put(".json", "okf_json");

		extensionsMap.put(".pentm", "okf_pensieve");

		extensionsMap.put(".yml", "okf_yaml");

		extensionsMap.put(".idml", "okf_idml");

		extensionsMap.put(".mif", "okf_mif");

		extensionsMap.put(".txp", "okf_transifex");

		extensionsMap.put(".rtf", "okf_tradosrtf");

		extensionsMap.put(".zip", "okf_archive");

		extensionsMap.put(".txml", "okf_txml");

		extensionsMap.put(".md", "okf_markdown");

		if (specifiedConfigIdPath != null) {
			fcMapper.setCustomConfigurationsDirectory(specifiedConfigIdPath);
		}

		loadFromPluginsAndUpdate();
	}

	private String getConfigurationId(String ext) {
		// Get the configuration for the extension
		String id = extensionsMap.get(ext);
		if (id == null) {
			throw new OkapiException(String.format(
					"Could not guess the configuration for the extension '%s'", ext));
		}
		return id;
	}

	private void editAllConfigurations() {
		initialize();
		guessMissingLocales(null);
		// Add the custom configurations
		fcMapper.updateCustomConfigurations();

		// Edit
		try {
			// Invoke the editor using dynamic instantiation so we can compile non-UI distributions
			IFilterConfigurationListEditor editor =
					(IFilterConfigurationListEditor) Class.forName("net.sf.okapi.common.ui.filters.FilterConfigurationEditor")
					                                      .newInstance();
			// Call the editor
			editor.editConfigurations(fcMapper);
		} catch (InstantiationException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		} catch (IllegalAccessException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		} catch (ClassNotFoundException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		}
	}

	private void editConfiguration() {
		initialize();
		guessMissingLocales(null);

		if (specifiedConfigId == null) {
			throw new OkapiException("You must specified the configuration to edit.");
		}
		configId = specifiedConfigId;
		if (!prepareFilter(configId)) return; // Next input

		try {
			// Invoke the editor using dynamic instantiation so we can compile non-UI distributions
			IFilterConfigurationEditor editor =
					(IFilterConfigurationEditor) Class.forName("net.sf.okapi.common.ui.filters.FilterConfigurationEditor")
					                                  .newInstance();
			// Call the editor
			editor.editConfiguration(configId, fcMapper);
		} catch (InstantiationException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		} catch (IllegalAccessException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		} catch (ClassNotFoundException e) {
			throw new OkapiException(MSG_ONLYWITHUICOMP);
		}
	}

	private void showAllConfigurations() {
		initialize();
		// Add the custom configurations
		fcMapper.updateCustomConfigurations();

		logger.info("List of all filter configurations available:");
		Iterator<FilterConfiguration> iter = fcMapper.getAllConfigurations();
		FilterConfiguration config;
		while (iter.hasNext()) {
			config = iter.next();
			logger.info(" - {} = {}", config.configId, config.description);
		}
	}

	private boolean prepareFilter(String configId) {
		// Is it a default configuration?
		if (fcMapper.getConfiguration(configId) != null) {
			return true;
		}
		// Else: Try to find the filter for that configuration
		Iterator<FilterConfiguration> configs = fcMapper.getAllConfigurations();
		while (configs.hasNext()) {
			FilterConfiguration fc = configs.next();
			if (configId.startsWith(fc.configId)) {
				// If the given configuration is not one of the pre-defined
				if (fcMapper.getConfiguration(configId) == null) {
					// Assume it is a custom one
					fcMapper.addCustomConfiguration(configId);
				}
				return true;
			}
		}

		// Could not guess
		logger.error("Could not guess the filter for the configuration '{}'", configId);
		return false;
	}

	private void loadFromPluginsAndUpdate() {
		// Discover and add plug-ins
		PluginsManager mgt = new PluginsManager();
		mgt.discover(new File(getAppRootDirectory() + File.separator + "dropins"), true);
		fcMapper.addFromPlugins(mgt);
	}

	private void guessMissingLocales(String inputPath) {
		// If both locales are already set: just use those
		if ((srcLoc != null) && (trgLoc != null)) return;

		// Try to see if we can get one or both from the input file
		if (inputPath != null) {
			List<String> guessed = FileUtil.guessLanguages(inputPath);
			if (guessed.size() > 0) {
				if (srcLoc == null) {
					srcLoc = LocaleId.fromString(guessed.get(0));
				}
				if (guessed.size() > 1) {
					if (trgLoc == null) {
						trgLoc = LocaleId.fromString(guessed.get(1));
					}
				}
			}
		}

		// Make sure we do have a source
		if (srcLoc == null) {
			srcLoc = LocaleId.fromString("en");
		}
		// Make sure we do have a target
		if (trgLoc == null) {
			trgLoc = new LocaleId(Locale.getDefault());
			if (trgLoc.sameLanguageAs(srcLoc)) {
				trgLoc = LocaleId.fromString("fr");
			}
		}
	}

	private void guessMissingParameters(String inputOfConfig) {
		if (specifiedConfigId == null) {
			String ext = Util.getExtension(inputOfConfig);
			if (Util.isEmpty(ext)) {
				throw new OkapiException(String.format(
						"The input file '%s' has no extension to guess the filter from.", inputOfConfig));
			}
			configId = getConfigurationId(ext.toLowerCase());
		} else {
			configId = specifiedConfigId;
		}
		guessMissingEncodings();
	}

	private void guessMissingEncodings() {
		if (outputEncoding == null) {
			if (inputEncoding != null) outputEncoding = inputEncoding;
			else outputEncoding = Charset.defaultCharset()
			                             .name();
		}
		if (inputEncoding == null) {
			inputEncoding = Charset.defaultCharset()
			                       .name();
		}
	}

	String pathChangeFolder(String newFolder,
	                        String oldPath) {
		String result;
		if (newFolder == null) {
			result = oldPath;
		} else {
			File file = new File(newFolder, Util.getFilename(oldPath, true));
			result = file.toString();
		}
		return result;
	}

	String pathChangeFilename(String newName, String oldPath) {
		String result;
		if (newName == null) {
			result = oldPath;
		} else {
			File file = new File(Util.getDirectoryName(oldPath), newName);
			//File file = new File(newFolder, Util.getFilename(oldPath, true));
			result = file.toString();
		}
		return result;
	}

	String pathInsertOutBeforeExt(String oldPath) {
		String ext = Util.getExtension(oldPath);
		int n = oldPath.lastIndexOf('.');
		if (n == -1) {
			// Filename with no extension
			return oldPath + ".out";
		}
		return oldPath.substring(0, n) + ".out" + ext; //$NON-NLS-1$
	}

	private void guessMergingArguments(String input) {
		String ext = Util.getExtension(input);
		if (!ext.equals(".xlf")) {
			throw new OkapiException(String.format(
					"The input file '%s' does not have the expected .xlf extension.", input));
		}
		if (originalFile == null) {
			int n = input.lastIndexOf('.');
			skeleton = input.substring(0, n);
		} else {
			skeleton = originalFile;
		}

		if (outputDir == null) {
			output = targetFilename == null ? pathInsertOutBeforeExt(skeleton) : targetFilename;
		} else {
			output = targetFilename == null ?
			         pathChangeFolder(outputDir, skeleton) :
			         pathChangeFolder(outputDir, targetFilename);
		}

		if (newSkel) skeleton += ".skl";
		skeleton = pathChangeFolder(skeletonDir, skeleton);
	}

	private void guessMergingMosesArguments(String input) {
		// TikalWrapper input is the original file, not the Moses file
		// The Moses file is specified with -from or null
		if (!Util.isEmpty(mosesFromPathParam)) {
			mosesFromPath = mosesFromPathParam;
		} else {
			// We guess the Moses filename:
			mosesFromPath = input + "." + trgLoc.toString();
		}
		// Output path
		if (!Util.isEmpty(mosesToPathParam)) {
			output = mosesToPathParam;
		} else {
			output = pathInsertOutBeforeExt(input);
		}
	}

	protected void process(String input, List<String> args) throws URISyntaxException {
		TikalWrapper.Timer timer = new TikalWrapper.Timer();
		initialize();

		// Add the custom configurations
		fcMapper.updateCustomConfigurations();

		File file;

		switch (command) {
			case CMD_TRANSLATE:
				logger.info("Translation");
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				guessMissingLocales(input);
				file = new File(input);
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc);) {
					rd.setFilterConfigId(configId);
					translateFile(rd);
				}
				break;

			case CMD_SEGMENTATION:
				logger.info("Segmentation");
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				guessMissingLocales(input);
				file = new File(input);
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc);) {
					rd.setFilterConfigId(configId);
					segmentFile(rd);
				}
				break;

			case CMD_EXTRACT:
				logger.info("Extraction");
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				guessMissingLocales(input);
				file = new File(input);
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc)) {
					rd.setFilterConfigId(configId);
					extractFile(rd, newSkel);
				}
				break;

			case CMD_EXTRACTTOMOSES:
				logger.info("Extraction to Moses InlineText");
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				guessMissingLocales(input);
				file = new File(input);
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc);) {
					rd.setFilterConfigId(configId);
					extractFileToMoses(rd);
				}
				break;

			case CMD_MERGE:
				logger.info("Merging");
				guessMergingArguments(input);
				guessMissingLocales(input);
				if (newSkel) {
					guessMissingEncodings();
					logger.info("Source language: {}", srcLoc);
					logger.info("Target language: {}", trgLoc);
					logger.info("Output encoding: {}", outputEncoding);
					logger.info("Skeleton: {}", skeleton);
					logger.info("XLIFF: {}", input);
					logger.info("Output: {}", (output == null) ? "<auto-defined>" : output);

					IPipelineDriver driver = new PipelineDriver();
					BatchItemContext bic = new BatchItemContext(
							new RawDocument(Util.toURI(input), StandardCharsets.UTF_8.name(), srcLoc, trgLoc),
							Util.toURI(output),
							outputEncoding,
							new RawDocument(Util.toURI(skeleton), StandardCharsets.UTF_8.name(), LocaleId.ENGLISH));
					driver.addBatchItem(bic);
					driver.addStep(new SkeletonXliffMergerStep());
					driver.addStep(new RawDocumentWriterStep());
					driver.processBatch();
					driver.destroy();
				} else {
					guessMissingParameters(skeleton);
					if (!prepareFilter(configId)) return; // Next input

					// Merge with a different version of filters or other external classes
					if (jarPath != null || jarVersion != null || autoJarVersion) {
						versionManager = new VersionManager();

						if (configPath != null) {
							logger.info("Path to the versions configuration file to use: {}", configPath);
							versionManager.load(FileUtil.fileToUrl(new File(configPath)));
						}

						if (jarPath != null) {
							logger.info("Path to the library version to use: {}", jarPath);
							versionManager.loadVersion(FileUtil.fileToUrl(new File(jarPath)));
						} else if (jarVersion != null) {
							logger.info("Library version to use: {}", jarVersion);
							versionManager.loadVersion(jarVersion);
						}
						if (autoJarVersion) {
							logger.info("The library version to use will be auto-detected");
							versionManager.loadVersion(new File(input));
						}

						String appLibPath = versionManager.getPath();

						// Prepare child process CL parameters
						List<String> params = new ArrayList<String>();
						params.add("java");

						// XXX External class loader implementation is not finished, don't remove the commented out block
						if (useExternalClassLoader) {
							params.add("-cp");
							params.add(System.getProperty("java.class.path"));
							params.add("-Djava.system.class.loader=net.sf.okapi.lib.tkit.jarswitcher.VMClassLoader");
							params.add(this.getClass()
							               .getName());
						} else {
							params.add("-jar");
							// Convert to system-specific path
							appLibPath = new File(versionManager.getPath()).getAbsolutePath();
							params.add(appLibPath);
						}

						// Collect params for the recursive process() as its args
						params.add("-m");
						params.add("-trace");
						for (int i = 0; i < args.size(); i++) {
							String arg = args.get(i);
							if (MERGE_PARAMS.contains(arg)) {
								// All merge params are duple
								params.add(arg);
								params.add(getArgument(args, ++i));
							}
						}
						params.add(input); // One input at this iteration of process()

						logger.info("Starting child process:\n" + ListUtil.listAsString(params, " "));
						ProcessBuilder pb = new ProcessBuilder(params);
						pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
						pb.redirectError(ProcessBuilder.Redirect.INHERIT);
						try {
							Process p = pb.start();
							if (useExternalClassLoader) {
								DataOutputStream dos = new DataOutputStream(p.getOutputStream());

								dos.writeUTF(appLibPath);
								dos.writeUTF(ClassUtil.getQualifiedClassName(this)); // appRootName
								dos.writeUTF(ClassUtil.getPath(this.getClass()));
								dos.writeUTF(ClassUtil.getClassFilePath(this.getClass()));
								dos.flush(); // Pass data to VMClassLoader
							}

						} catch (IOException e) {
							throw new OkapiIOException(e);
						}

						break;
					}

					logger.info("Source language: {}", srcLoc);
					logger.info("Target language: {}", trgLoc);
					logger.info("Default input encoding: {}", inputEncoding);
					logger.info("Output encoding: {}", outputEncoding);
					logger.info("Filter configuration: {}", configId);
					logger.info("XLIFF: {}", input);
					logger.info("Output: {}", (output == null) ? "<auto-defined>" : output);

					// original document aka "skeleton"
					RawDocument originalDoc = new RawDocument(Util.toURI(skeleton), inputEncoding, srcLoc, trgLoc);
					originalDoc.setFilterConfigId(configId);

					IPipelineDriver driver = new PipelineDriver();
					driver.setFilterConfigurationMapper(fcMapper);
					BatchItemContext bic = new BatchItemContext(
							// xliff RawDocument
							new RawDocument(Util.toURI(input), StandardCharsets.UTF_8.name(), srcLoc, trgLoc),
							Util.toURI(output),
							outputEncoding,
							originalDoc);
					driver.addBatchItem(bic);
					driver.addStep(new OriginalDocumentXliffMergerStep());
					driver.addStep(new RawDocumentWriterStep());
					driver.processBatch();
					driver.destroy();
				}
				break;

			case CMD_LEVERAGEMOSES:
				logger.info("Merging Moses InlineText");
				guessMissingLocales(input);
				guessMergingMosesArguments(input);
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				file = new File(input);
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc, configId);) {
					leverageFileWithMoses(rd);
				}
				break;

			case CMD_CONV2PO:
			case CMD_CONV2TMX:
			case CMD_CONV2PEN:
			case CMD_CONV2TABLE:
				if (command == CMD_CONV2PO) {
					logger.info("Conversion to PO");
				} else if (command == CMD_CONV2TMX) {
					logger.info("Conversion to TMX");
				} else if (command == CMD_CONV2TABLE) {
					logger.info("Conversion to Table");
				} else {
					logger.info("Importing to Pensieve TM");
				}
				guessMissingParameters(input);
				if (!prepareFilter(configId)) return; // Next input
				guessMissingLocales(input);

				file = new File(input);
				String output = input;
				if (command == CMD_CONV2PO) {
					output += ".po";
				} else if (command == CMD_CONV2TMX) {
					output += ".tmx";
				} else if (command == CMD_CONV2TABLE) {
					output += ".txt";
				} else { // Pensieve
					output = checkPensieveDirExtension();
				}
				URI outputURI = new File(output).toURI();
				try (RawDocument rd = new RawDocument(file.toURI(), inputEncoding, srcLoc, trgLoc);) {
					rd.setFilterConfigId(configId);

					logger.info("Source language: {}", srcLoc);
					logger.info("Target language: {}", trgLoc);
					logger.info("Default input encoding: {}", inputEncoding);
					logger.info("Filter configuration: {}", configId);
					logger.info("Output: {}", output);

					convertFile(rd, outputURI);
				}
				break;
		}
		logger.info("Done in " + timer);
	}

	private void printBanner() {
		if (getClass().getPackage() == null) return;
		logger.info("-------------------------------------------------------------------------------"); //$NON-NLS-1$
		logger.info("Okapi Tikal - Localization Toolset");
		// The version will show as 'null' until the code is build as a JAR.
		logger.info("Version: {}",
		            getClass().getPackage()
		                      .getImplementationVersion());
		logger.info("-------------------------------------------------------------------------------"); //$NON-NLS-1$
	}

	private void showInfo() {
		Runtime rt = Runtime.getRuntime();
		rt.runFinalization();
		rt.gc();
		logger.info("Java version: {}", System.getProperty("java.version")); //$NON-NLS-1$
		logger.info("Platform: {}, {}, {}",
		            System.getProperty("os.name"), //$NON-NLS-1$
		            System.getProperty("os.arch"), //$NON-NLS-1$
		            System.getProperty("os.version")); //$NON-NLS-1$
		NumberFormat nf = NumberFormat.getInstance();
		logger.info("Java VM memory: free={} KB, total={} KB", //$NON-NLS-1$
		            nf.format(rt.freeMemory() / 1024),
		            nf.format(rt.totalMemory() / 1024));
		logger.info("Tikal display encoding: {}", getConsoleEncodingName());
		logger.info("-------------------------------------------------------------------------------"); //$NON-NLS-1$
	}

	private String getAppRootDirectory() {
		try {
// Old code
//			URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
//			String path = new File(url.toURI()).getCanonicalPath();
//			return Util.getDirectoryName(Util.getDirectoryName(path));

			// Get the location of the process class source
			logger.debug("1 " + getClass());
			logger.debug("2 " + getClass().getProtectionDomain());
			logger.debug("3 " + getClass().getProtectionDomain()
			                              .getCodeSource());
			logger.debug("4 " + getClass().getProtectionDomain()
			                              .getCodeSource()
			                              .getLocation());
			logger.debug("5 " + getClass().getProtectionDomain()
			                              .getCodeSource()
			                              .getLocation()
			                              .getFile());
			File file = new File(getClass().getProtectionDomain()
			                               .getCodeSource()
			                               .getLocation()
			                               .getFile());
			String appRootFolder = URLDecoder.decode(file.getAbsolutePath(), "utf-8"); //$NON-NLS-1$
			// Remove the JAR file if running an installed version
			boolean fromJar = appRootFolder.endsWith(".jar");
			if (fromJar) appRootFolder = Util.getDirectoryName(appRootFolder);
			// Remove the application folder in all cases
			return Util.getDirectoryName(appRootFolder);
		} catch (IOException e) {
			throw new OkapiIOException(e);
		}
	}

	private void showHelp() throws MalformedURLException {
		Util.openWikiTopic("Tikal");
	}

	private void printUsage() {
		logger.info("Shows this screen: -?");
		logger.info("Shows version and other information: -i or --info");
		logger.info("Opens the user guide page: -h or --help");
		logger.info("Lists all available filter configurations: -lfc or --listconf");
		logger.info("Outputs all messages to the current logger instead of the console: -logger");
		logger.info("Outputs debug messages when in console mode (no effect on logger): -trace");
		logger.info("Does not abort batch processing in case of individual errors: -continue");
		logger.info("Edits or view filter configurations (UI-dependent command):");
		logger.info("   -e [[-fc] configId] [-pd configDirectory]");
		logger.info("Extracts a file to XLIFF (and optionally segment and pre-translate):");
		logger.info("   -x[1|2] inputFile [inputFile2...] [-fc configId] [-ie encoding] [-sl srcLang]");
		logger.info("      [-tl trgLang] [-seg [srxFile]] [-tt [url]|-mm [key]");
		logger.info("      |-pen tmDirectory|-gs configFile|-apertium [configFile]|-mmt url [context]");
		logger.info("      |-ms configFile|-tda configFile|-gg configFile|-bi bilingFile|-lingo24 configFile]");
		logger.info("      [-maketmx [tmxFile]] [-opt threshold] [-od outputDirectory]");
		logger.info("      [-rd rootDirectory] [-nocopy] [-noalttrans] [-pd configDirectory]");
		logger.info("      -x and -x1: use original file for merge, -x2: use JSON skeleton file for merge");
		logger.info("Merges an XLIFF document back to its original format:");
		logger.info("   -m[1|2] xliffFile [xliffFile2...] [-fc configId] [-ie encoding] [-oe encoding]");
		logger.info("      [-sd sourceDirectory] [-od outputDirectory] [-pd configDirectory]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-av|-v <libVersion>|-vp <jarPath>]|-vc <configPath>");
		logger.info("      -m and -m1: use original file for merge, -m2: use JSON skeleton file for merge");
		logger.info("Translates a file:");
		logger.info("   -t inputFile [inputFile2...] [-fc configId] [-ie encoding] [-oe encoding]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-seg [srxFile]] [-tt [url]");
		logger.info("      |-mm [key]|-pen tmDirectory|-gs configFile|-apertium [configFile]|-mmt url [context]");
		logger.info("      |-ms configFile|-tda configFile|-gg configFile|-bi bilingFile|-lingo24 [configFile]");
		logger.info("      [-maketmx [tmxFile]] [-opt threshold] [-pd configDirectory]");
		logger.info("      [-rd rootDirectory]");
		logger.info("Extracts a file to Moses InlineText:");
		logger.info("   -xm inputFile [-fc configId] [-ie encoding] [-seg [srxFile]] [-2]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-rd rootDirectory]");
		logger.info("      [-to srcOutputFile] (single input only)");
		logger.info("Leverages a file with Moses InlineText:");
		logger.info("   -lm inputFile [-fc configId] [-ie encoding] [-oe encoding] [-sl srcLang]");
		logger.info("      [-tl trgLang] [-seg [srxFile]] [-totrg|-overtrg] [-bpt]");
		logger.info("      [-rd rootDirectory] [-noalttrans]");
		logger.info("      [-from mosesFile] [-to outputFile] (single input only)");
		logger.info("Segments a file:");
		logger.info("   -s inputFile [-fc configId] [-ie encoding] [-rd rootDirectory]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-seg [srxFile]] [-pd configDirectory]");
		logger.info("Queries translation resources:");
		logger.info("   -q \"source text\" [-sl srcLang] [-tl trgLang]");
		logger.info("      [-tt [url]] [-mm [key]] [-pen tmDirectory] [-gs configFile] [-mmt url [context]]");
		logger.info("      [-apertium [configFile]] [-ms configFile] [-tda configFile] [-lingo24 [configFile]]");
		logger.info("      [-gg configFile] [-bi bilingFile] [-lingo24 configFile] [-opt threshold[:maxhits]]");
		//logger.info("Adds translation to a resources:");
		//logger.info("   -a \"source text\" \"target text\" [rating] [-sl srcLang] [-tl trgLang]");
		//logger.info("      -ms configFile");
		logger.info("Converts to PO format:");
		logger.info("   -2po inputFile [inputFile2...] [-fc configId] [-ie encoding] [-all]");
		logger.info("      [-generic] [-sl srcLang] [-tl trgLang] [-trgsource|-trgempty]");
		logger.info("      [-rd rootDirectory] [-pd configDirectory] [-approved]");
		logger.info("Converts to TMX format:");
		logger.info("   -2tmx inputFile [inputFile2...] [-fc configId] [-ie encoding] [-all]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-trgsource|-trgempty] [-rd rootDirectory]");
		logger.info("      [-pd configDirectory] [-approved]");
		logger.info("Converts to table format:");
		logger.info("   -2tbl inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-trgsource|-trgempty] [-csv|-tab]");
		logger.info("      [-xliff|-xliffgx|-tmx|-generic] [-all] [-rd rootDirectory]");
		logger.info("      [-pd configDirectory] [-approved]");
		logger.info("Imports to Pensieve TM:");
		logger.info("   -imp tmDirectory inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-trgsource|-trgempty] [-all] [-over]");
		logger.info("      [-rd rootDirectory] [-pd configDirectory] [-approved]");
		logger.info("Exports Pensieve TM as TMX:");
		logger.info("   -exp tmDirectory1 [tmDirectory2...] [-sl srcLang] [-tl trgLang]");
		logger.info("      [-trgsource|-trgempty] [-all]");
		logger.info("Prints a Scoping Report:");
		logger.info("   -sr inputFile [inputFile2...] [-fc configId] [-ie encoding]");
		logger.info("      [-sl srcLang] [-tl trgLang] [-pd configDirectory] [-seg [srxFile]]");
		logger.info("      [-tt [url]|-mm [key]|-pen tmDirectory|-gs configFile|-mmt url [context]");
		logger.info("      |-apertium [configFile]|-ms configFile|-tda configFile|-gg configFile");
		logger.info("      |-bi bilingFile|-lingo24 [configFile] [-maketmx [tmxFile]] [-opt threshold]");
	}

	private void displayQuery(IQuery conn) {
		int count;
		String name = conn.getClass()
		                  .getName();
		if (name.endsWith("PensieveTMConnector")
				|| name.endsWith("GoogleMTv2Connector")
				|| name.endsWith("MyMemoryTMConnector")
				|| name.endsWith("MicrosoftMTConnector")
				|| name.endsWith("GlobalSightTMConnector")
				|| name.endsWith("BilingualFileConnector")
				|| name.endsWith("Lingo24Connector")
				|| name.endsWith("MMTConnector")) { // Connectors supporting inline codes
			count = conn.query(parseToTextFragment(query));
		} else { // Raw text otherwise
			count = conn.query(query);
		}

		logger.info("\n= From {} ({}->{})", name, conn.getSourceLanguage(), conn.getTargetLanguage());
		if (conn instanceof ITMQuery) {
			ITMQuery tmConn = (ITMQuery) conn;
			logger.info("  Threshold={}, Maximum hits={}",
			            tmConn.getThreshold(), tmConn.getMaximumHits());
		}

		boolean first = true;
		if (count > 0) {
			QueryResult qr;
			while (conn.hasNext()) {
				qr = conn.next();
				// Engine info for the first occurrence
				if (first && !Util.isEmpty(qr.engine)) {
					first = false;
					logger.info("  Engine: '{}'", qr.engine);
				}
				logger.info("score: {}, origin: '{}'{}",
				            qr.getCombinedScore(),
				            (qr.origin == null ? "" : qr.origin),
				            (qr.fromMT() ? " (from MT)" : ""));
				logger.info("  Source: \"{}\"", qr.source.toText());
				logger.info("  Target: \"{}\"", qr.target.toText());
			}
		} else {
			logger.info("  Source: \"{}\"", query);
			logger.info("  <No translation has been found>");
		}
	}

	private void processQuery() {
		guessMissingLocales(null);
		if (!useGoogleV2 && !useTransToolkit && !useMyMemory
				&& !usePensieve && !useGlobalSight && !useApertium && !useMicrosoft
				&& !useTDA && !useBifile && !useLingo24 && !useMMT) {
			useTransToolkit = true; // Default if none is specified
		}
		// Query options
		int[] opt = parseTMOptions();
		int threshold = opt[0];
		int maxhits = opt[1];

		IQuery conn;
		if (useGoogleV2) {
			conn = new GoogleMTv2Connector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (usePensieve) {
			conn = new PensieveTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useTransToolkit) {
			conn = new TranslateToolkitTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useGlobalSight) {
			conn = new GlobalSightTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useTDA) {
			conn = new TDASearchConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useMicrosoft) {
			conn = new MicrosoftMTConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useMyMemory) {
			conn = new MyMemoryTMConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useApertium) {
			conn = new ApertiumMTConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useBifile) {
			conn = new BilingualFileConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			setTMOptionsIfPossible(conn, threshold, maxhits);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useLingo24) {
			conn = new Lingo24Connector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
		if (useMMT) {
			conn = new MMTConnector();
			conn.setParameters(prepareConnectorParameters(conn.getClass()
			                                                  .getName()));
			conn.setLanguages(srcLoc, trgLoc);
			conn.open();
			displayQuery(conn);
			conn.close();
		}
	}

	private int[] parseTMOptions() {
		int[] opt = new int[2];
		opt[0] = -1;
		opt[1] = -1;
		if (!Util.isEmpty(tmOptions)) {
			try {
				// Expected format: "threshold[:maxhits]"
				int n = tmOptions.indexOf(':');
				if (n == -1) { // Threshold only
					opt[0] = Integer.parseInt(tmOptions);
				} else {
					opt[0] = Integer.parseInt(tmOptions.substring(0, n));
					opt[1] = Integer.parseInt(tmOptions.substring(n + 1));
					if (opt[1] < 0) {
						throw new OkapiException(String.format("Invalid TM options: '%s' Maximum hits must be more than 0.",
						                                       tmOptions));
					}
				}
				if ((opt[0] < 0) || (opt[0] > 100)) {
					throw new OkapiException(String.format("Invalid TM options: '%s' Thresold must be between 0 and 100.",
					                                       tmOptions));
				}
			} catch (NumberFormatException e) {
				throw new OkapiException(String.format("Invalid TM options: '%s'", tmOptions));
			}
		}
		return opt;
	}

	private void setTMOptionsIfPossible(IQuery conn,
	                                    int threshold,
	                                    int maxhits) {
		ITMQuery tmConn = (ITMQuery) conn;
		if (threshold > -1) tmConn.setThreshold(threshold);
		if (maxhits > -1) tmConn.setMaximumHits(maxhits);
	}

	private void convertFile(RawDocument rd, URI outputURI) {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));

		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		FormatConversionStep fcStep = new FormatConversionStep();
		net.sf.okapi.steps.formatconversion.Parameters params = fcStep.getParameters();
		if (command == CMD_CONV2PO) {
			params.setOutputFormat(Parameters.FORMAT_PO);
			params.setOutputPath("output.po");
		} else if (command == CMD_CONV2TMX) {
			params.setOutputFormat(Parameters.FORMAT_TMX);
			params.setOutputPath("output.tmx");
		} else if (command == CMD_CONV2TABLE) {
			params.setOutputFormat(Parameters.FORMAT_TABLE);
			TableFilterWriterParameters opt = new TableFilterWriterParameters();
			opt.fromArguments(tableConvFormat, tableConvCodes);
			params.setFormatOptions(opt.toString());
			params.setOutputPath("output.txt");
		} else if (command == CMD_CONV2PEN) {
			params.setOutputFormat(Parameters.FORMAT_PENSIEVE);
			params.setOutputPath(checkPensieveDirExtension());
		}

		params.setSingleOutput(command == CMD_CONV2PEN);

		// These options may or may not be used depending on the output format
		params.setUseGenericCodes(genericOutput);
		params.setTargetStyle(convTargetStyle);
		params.setSkipEntriesWithoutText(convSkipEntriesWithoutText);
		params.setOverwriteSameSource(convOverwrite);
		params.setApprovedEntriesOnly(convApprovedEntriesOnly);

		driver.addStep(fcStep);
		driver.addBatchItem(rd, outputURI, outputEncoding);
		driver.processBatch();
	}

	private IPipelineStep addSegmentationStep() {
		if (segRules.equals(DEFAULT_SEGRULES)) { // Defaults
			segRules = getAppRootDirectory();
			segRules += File.separator + "config" + File.separator + "defaultSegmentation.srx";
		} else {
			if (Util.isEmpty(Util.getExtension(segRules))) {
				segRules += ".srx";
			}
		}
		SegmentationStep segStep = new SegmentationStep();
		net.sf.okapi.steps.segmentation.Parameters segParams
				= (net.sf.okapi.steps.segmentation.Parameters) segStep.getParameters();
		segParams.setSegmentSource(true);
		segParams.setSegmentTarget(true);
		File f = new File(segRules);
		segParams.setSourceSrxPath(f.getAbsolutePath());
		segParams.setTargetSrxPath(f.getAbsolutePath());
		segParams.setCopySource(extOptCopy);
		logger.info("Segmentation: {}", f.getAbsolutePath());
		return segStep;
	}

	private IPipelineStep addLeveragingStep() {
		LeveragingStep levStep = new LeveragingStep();
		net.sf.okapi.steps.leveraging.Parameters levParams
				= (net.sf.okapi.steps.leveraging.Parameters) levStep.getParameters();
		if (usePensieve) {
			levParams.setResourceClassName(PensieveTMConnector.class.getName());
		} else if (useTransToolkit) {
			levParams.setResourceClassName(TranslateToolkitTMConnector.class.getName());
		} else if (useMyMemory) {
			levParams.setResourceClassName(MyMemoryTMConnector.class.getName());
		} else if (useGoogleV2) {
			levParams.setResourceClassName(GoogleMTv2Connector.class.getName());
		} else if (useGlobalSight) {
			levParams.setResourceClassName(GlobalSightTMConnector.class.getName());
		} else if (useTDA) {
			levParams.setResourceClassName(TDASearchConnector.class.getName());
		} else if (useMicrosoft) {
			levParams.setResourceClassName(MicrosoftMTConnector.class.getName());
		} else if (useApertium) {
			levParams.setResourceClassName(ApertiumMTConnector.class.getName());
		} else if (useBifile) {
			levParams.setResourceClassName(BilingualFileConnector.class.getName());
		} else if (useLingo24) {
			levParams.setResourceClassName(Lingo24Connector.class.getName());
		} else if (useMMT) {
			levParams.setResourceClassName(MMTConnector.class.getName());
		}
		IParameters p = prepareConnectorParameters(levParams.getResourceClassName());
		if (p != null) levParams.setResourceParameters(p.toString());
		levParams.setFillTarget(levOptFillTarget);
		// Query options
		int[] opt = parseTMOptions();
		if (opt[0] > -1) levParams.setThreshold(opt[0]);
		if (levOptTMXPath != null) {
			levParams.setMakeTMX(true);
			levParams.setTMXPath(levOptTMXPath);
		}
		return levStep;
	}

	// TODO: Analyse this method
	private void extractFile(RawDocument rd,
	                         boolean newSkel)
			throws URISyntaxException {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));
		driver.setExecutionContext(context);

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		// Add segmentation step if requested
		if (segRules != null) {
			driver.addStep(addSegmentationStep());
		}

		// Add leveraging step if requested
		if (useGoogleV2 || useTransToolkit || useMyMemory || usePensieve
				|| useGlobalSight || useApertium || useMicrosoft || useTDA || useBifile
				|| useLingo24 || useMMT) {
			driver.addStep(addLeveragingStep());
		}

		// Filter events to raw document final step (using the XLIFF writer)
		FilterEventsWriterStep fewStep = new FilterEventsWriterStep();
		XLIFFWriterParameters paramsXliff;
		if (newSkel) {
			XLIFFAndSkeletonWriter writer = new XLIFFAndSkeletonWriter();
			fewStep.setFilterWriter(writer);
			paramsXliff = (XLIFFWriterParameters) writer.getParameters();
		} else {
			XLIFFWriter writer = new XLIFFWriter();
			fewStep.setFilterWriter(writer);
			paramsXliff = (XLIFFWriterParameters) writer.getParameters();
		}
		paramsXliff.setPlaceholderMode(true);
		paramsXliff.setCopySource(extOptCopy);
		paramsXliff.setIncludeAltTrans(extOptAltTrans);
		paramsXliff.setIncludeCodeAttrs(extOptCodeAttrs);

		fewStep.setDocumentRoots(rootDir);
		driver.addStep(fewStep);

		// Create the raw document and set the output
		String tmp = rd.getInputURI()
		               .getPath();
		// If the input is a directory, it ends with a separator, then we remove it
		if (tmp.endsWith("/") || tmp.endsWith("\\")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}
		tmp += ".xlf";

		tmp = pathChangeFilename(targetFilename, tmp);

		tmp = pathChangeFolder(outputDir, tmp);
		driver.addBatchItem(rd, new File(tmp).toURI(), outputEncoding);

		logger.info("Source language: {}", srcLoc);
		logger.info("Target language: {}", trgLoc);
		logger.info("Default input encoding: {}", inputEncoding);
		logger.info("Filter configuration: {}", configId);
		logger.info("Output: {}", tmp);

		// Process
		driver.processBatch();
	}

	private void segmentFile(RawDocument rd) throws URISyntaxException {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		driver.addStep(addSegmentationStep());

		// Filter events to raw document final step
		FilterEventsToRawDocumentStep ferdStep = new FilterEventsToRawDocumentStep();
		driver.addStep(ferdStep);

		// Create the raw document and set the output
		String tmp = rd.getInputURI()
		               .getPath();

		output = pathInsertOutBeforeExt(tmp);

		logger.info("Source language: {}", srcLoc);
		logger.info("Target language: {}", trgLoc);
		logger.info("Default input encoding: {}", inputEncoding);
		logger.info("Output encoding: {}", outputEncoding);
		logger.info("Filter configuration: {}", configId);
		logger.info("Output: {}", output);

		driver.addBatchItem(rd, new File(output).toURI(), outputEncoding);

		// Process
		driver.processBatch();
	}

	private void leverageFileWithMoses(RawDocument rd) {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));
		driver.addStep(new RawDocumentToFilterEventsStep());

		// Add segmentation step if requested
		if (segRules != null) {
			driver.addStep(addSegmentationStep());
		}

		MergingStep mrgStep = new MergingStep();
		MergingParameters params = (MergingParameters) mrgStep.getParameters();
		params.setCopyToTarget(mosesCopyToTarget);
		params.setOverwriteExistingTarget(mosesOverwriteTarget);
		params.setForceAltTransOutput(extOptAltTrans);
		params.setUseGModeInAltTrans(mosesUseGModeInAltTrans);
		driver.addStep(mrgStep);

		driver.addStep(new FilterEventsToRawDocumentStep());

		// Two parallel inputs: 1=the original file, 2=the Moses translated file
		try (RawDocument rdMoses = new RawDocument(new File(mosesFromPath).toURI(), "UTF-8", trgLoc);) {
			driver.addBatchItem(new BatchItemContext(rd, new File(output).toURI(), outputEncoding, rdMoses));
			// Execute
			driver.processBatch();
		}
	}

	private void extractFileToMoses(RawDocument rd) throws URISyntaxException {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		// Add segmentation step if requested
		if (segRules != null) {
			driver.addStep(addSegmentationStep());
		}

		// Filter events to raw document final step (using the XLIFF writer)
		ExtractionStep extStep = new ExtractionStep();
		if (moses2Outputs) {
			FilterWriterParameters p = (FilterWriterParameters) extStep.getParameters();
			p.setSourceAndTarget(true);
		}
		driver.addStep(extStep);

		// Create the raw document and set the output
		String outPath;
		if (!Util.isEmpty(mosesToPathParam)) {
			outPath = mosesToPathParam;
		} else {
			outPath = rd.getInputURI()
			            .getPath();
		}
		if (!outPath.endsWith("." + srcLoc.toString())) {
			outPath = outPath + ("." + srcLoc.toString());
		}
		driver.addBatchItem(rd, new File(outPath).toURI(), "UTF-8");

		logger.info("Source language: {}", srcLoc);
		if (moses2Outputs) {
			logger.info("Target language: {}", trgLoc);
		}
		logger.info("Default input encoding: {}", inputEncoding);
		logger.info("Filter configuration: {}", configId);

		// Process
		driver.processBatch();
	}

	private void translateFile(RawDocument rd) throws URISyntaxException {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.setRootDirectories(rootDir,
		                          Util.getDirectoryName(rd.getInputURI()
		                                                  .getPath()));

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		// Add segmentation step if requested
		if (segRules != null) {
			driver.addStep(addSegmentationStep());
		}

		// Add leveraging step
		if (useGoogleV2 || useTransToolkit || useMyMemory || usePensieve
				|| useGlobalSight || useApertium || useMicrosoft || useTDA || useBifile
				|| useLingo24 || useMMT) {
			driver.addStep(addLeveragingStep());
		} else { // Or indicate that we won't translate
			logger.info("No valid translation resource has been specified: The text will not be modified.");
		}

		// Filter events to raw document final step
		FilterEventsToRawDocumentStep ferdStep = new FilterEventsToRawDocumentStep();
		driver.addStep(ferdStep);

		// Create the raw document and set the output
		String tmp = rd.getInputURI()
		               .getPath();

		output = pathInsertOutBeforeExt(tmp);

		logger.info("Source language: {}", srcLoc);
		logger.info("Target language: {}", trgLoc);
		logger.info("Default input encoding: {}", inputEncoding);
		logger.info("Output encoding: {}", outputEncoding);
		logger.info("Filter configuration: {}", configId);
		logger.info("Output: {}", output);

		driver.addBatchItem(rd, new File(output).toURI(), outputEncoding);

		// Process
		driver.processBatch();
	}

	private String checkPensieveDirExtension() {
		String ext = Util.getExtension(pensieveData);
		if (Util.isEmpty(ext)) pensieveData += ".pentm";
		return pensieveData;
	}

	private IParameters prepareConnectorParameters(String connectorClassName) {
		if (connectorClassName.equals(PensieveTMConnector.class.getName())) {
			net.sf.okapi.connectors.pensieve.Parameters params
					= new net.sf.okapi.connectors.pensieve.Parameters();
			if (pensieveData.startsWith("http:")) {
				params.setHost(pensieveData);
				params.setUseServer(true);
			} else {
				params.setDbDirectory(checkPensieveDirExtension());
			}
			return params;
		}

		if (connectorClassName.equals(TranslateToolkitTMConnector.class.getName())) {
			net.sf.okapi.connectors.translatetoolkit.Parameters params
					= new net.sf.okapi.connectors.translatetoolkit.Parameters();
			// Sets the parameters
			params.setUrl(transToolkitParams);
			return params;
		}

		if (connectorClassName.equals(MyMemoryTMConnector.class.getName())) {
			net.sf.okapi.connectors.mymemory.Parameters params
					= new net.sf.okapi.connectors.mymemory.Parameters();
			params.setKey(myMemoryParams);
			return params;
		}

		if (connectorClassName.equals(GlobalSightTMConnector.class.getName())) {
			net.sf.okapi.connectors.globalsight.Parameters params
					= new net.sf.okapi.connectors.globalsight.Parameters();
			URI paramURI = (new File(globalSightParams).toURI());
			params.load(Util.URItoURL(paramURI), false);
			return params;
		}

		if (connectorClassName.equals(TDASearchConnector.class.getName())) {
			net.sf.okapi.connectors.tda.Parameters params
					= new net.sf.okapi.connectors.tda.Parameters();
			URI paramURI = (new File(tdaParams).toURI());
			params.load(Util.URItoURL(paramURI), false);
			return params;
		}

		if (connectorClassName.equals(MicrosoftMTConnector.class.getName())) {
			net.sf.okapi.connectors.microsoft.Parameters params
					= new net.sf.okapi.connectors.microsoft.Parameters();
			// Use the specified parameters if available, otherwise use the default
			if (microsoftParams != null) {
				URI paramURI = (new File(microsoftParams).toURI());
				params.load(Util.URItoURL(paramURI), false);
			}
			return params;
		}

		if (connectorClassName.equals(GoogleMTv2Connector.class.getName())) {
			net.sf.okapi.connectors.google.GoogleMTv2Parameters params
					= new net.sf.okapi.connectors.google.GoogleMTv2Parameters();
			// Use the specified parameters if available, otherwise use the default
			if (googleV2Params != null) {
				URI paramURI = (new File(googleV2Params).toURI());
				params.load(Util.URItoURL(paramURI), false);
			}
			return params;
		}

		if (connectorClassName.equals(ApertiumMTConnector.class.getName())) {
			net.sf.okapi.connectors.apertium.Parameters params
					= new net.sf.okapi.connectors.apertium.Parameters();
			if (apertiumParams != null) {
				URI paramURI = (new File(apertiumParams).toURI());
				params.load(Util.URItoURL(paramURI), false);
			} // Use default otherwise
			return params;
		}

		if (connectorClassName.equals(BilingualFileConnector.class.getName())) {
			net.sf.okapi.connectors.bifile.Parameters params
					= new net.sf.okapi.connectors.bifile.Parameters();
			params.setBiFile(bifileData);
			params.setInputEncoding(inputEncoding);
			return params;
		}

		if (connectorClassName.equals(Lingo24Connector.class.getName())) {
			net.sf.okapi.connectors.lingo24.Parameters params
					= new net.sf.okapi.connectors.lingo24.Parameters();
			if (lingo24Params != null) {
				params.setUserKey(lingo24Params);
			}
			return params;
		}

		if (connectorClassName.equals(MMTConnector.class.getName())) {
			net.sf.okapi.connectors.mmt.Parameters params
					= new net.sf.okapi.connectors.mmt.Parameters();
			params.setUrl(mmtUrl);
			if (mmtContext != null) {
				params.setContext(mmtContext);
			}
			return params;
		}

		// Other connector: no parameters
		return null;
	}

	private String printWordCount() {
		initialize();

		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);

		for (String input : inputs) {
			guessMissingParameters(input);
			guessMissingLocales(input);

			driver.addBatchItem(new RawDocument(Util.toURI(input), StandardCharsets.UTF_8.name(), srcLoc, trgLoc, configId),
			                    null,
			                    null);
		}

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		String template = "[" + ScopingReportStep.PROJECT_TOTAL_WORD_COUNT + "]";

		WordCountStep wcStep = new WordCountStep();
		driver.addStep(wcStep);

		ScopingReportStep srStep = new ScopingReportStep();
		net.sf.okapi.steps.scopingreport.Parameters params = new net.sf.okapi.steps.scopingreport.Parameters();
		params.setCustomTemplateString(template);
		params.setOutputPath(null);
		srStep.setParameters(params);
		driver.addStep(srStep);

		driver.processBatch();
		driver.destroy();
		return srStep.getReportGenerator()
		             .generate();
	}


	private void printScopingReport() {
		initialize();

		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);

		for (String input : inputs) {
			guessMissingParameters(input);
			guessMissingLocales(input);

			driver.addBatchItem(new RawDocument(Util.toURI(input), StandardCharsets.UTF_8.name(), srcLoc, trgLoc, configId),
			                    null,
			                    null);
		}

		// Raw document to filter events step
		RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		String template = "\n[Scoping Report]\nDate: [" + ScopingReportStep.PROJECT_DATE + "]\n"
				+ "Files:\n  [[" + ScopingReportStep.ITEM_NAME + "]]\n"
				+ "Totals:\n  [" + ScopingReportStep.PROJECT_TOTAL_WORD_COUNT + "] words\n"
				+ "  [" + ScopingReportStep.PROJECT_TOTAL_CHARACTER_COUNT + "] characters\n";

		// Add segmentation step if requested
		if (segRules != null) {
			driver.addStep(addSegmentationStep());
		}

		// Add leveraging step if requested
		if (useGoogleV2 || useTransToolkit || useMyMemory || usePensieve
				|| useGlobalSight || useApertium || useMicrosoft || useTDA || useBifile || useLingo24 || useMMT) {
			driver.addStep(addLeveragingStep());
			template += "\nExact Local Context: [" + ScopingReportStep.PROJECT_EXACT_LOCAL_CONTEXT + "] words\n"
					+ "                     [" + ScopingReportStep.PROJECT_EXACT_LOCAL_CONTEXT_CHARACTERS + "] characters\n"
					+ "100% Match: [" + ScopingReportStep.PROJECT_GMX_LEVERAGED_MATCHED_WORD_COUNT + "] words\n"
					+ "            [" + ScopingReportStep.PROJECT_GMX_LEVERAGED_MATCHED_CHARACTER_COUNT + "] characters\n"
					+ "Fuzzy Match: [" + ScopingReportStep.PROJECT_GMX_FUZZY_MATCHED_WORD_COUNT + "] words\n"
					+ "             [" + ScopingReportStep.PROJECT_GMX_FUZZY_MATCHED_CHARACTER_COUNT + "] characters\n"
					+ "Repetitions: [" + ScopingReportStep.PROJECT_GMX_REPETITION_MATCHED_WORD_COUNT + "] words\n"
					+ "             [" + ScopingReportStep.PROJECT_GMX_REPETITION_MATCHED_CHARACTER_COUNT + "] characters";
		}

		WordCountStep wcStep = new WordCountStep();
		driver.addStep(wcStep);

		CharacterCountStep ccStep = new CharacterCountStep();
		driver.addStep(ccStep);

		ScopingReportStep srStep = new ScopingReportStep();
		net.sf.okapi.steps.scopingreport.Parameters params = new net.sf.okapi.steps.scopingreport.Parameters();
		params.setCustomTemplateString(template);
		params.setOutputPath(null);
		srStep.setParameters(params);
		driver.addStep(srStep);

		driver.processBatch();
		driver.destroy();
		logger.info(srStep.getReportGenerator()
		                  .generate());
	}

	/**
	 * Converts the plain text string into a TextFragment, using HTML-like patterns are inline codes.
	 *
	 * @param text the plain text to convert to TextFragment
	 * @return a new TextFragment (with possibly inline codes).
	 */
	public TextFragment parseToTextFragment(String text) {
		// Parses any thing within <...> into opening codes
		// Parses any thing within </...> into closing codes
		// Parses any thing within <.../> into placeholder codes
		Pattern patternOpening = Pattern.compile("\\<(\\w+)[ ]*[^\\>/]*\\>");
		Pattern patternClosing = Pattern.compile("\\</(\\w+)[ ]*[^\\>]*\\>");
		Pattern patternPlaceholder = Pattern.compile("\\<(\\w+)[ ]*[^\\>]*/\\>");

		TextFragment tf = new TextFragment();
		tf.setCodedText(text);

		int n;
		int start = 0;
		int diff = 0;
		Matcher m = patternOpening.matcher(text);
		while (m.find(start)) {
			n = m.start();
			diff += tf.changeToCode(n + diff,
			                        (n + diff) + m.group()
			                                      .length(),
			                        TextFragment.TagType.OPENING,
			                        m.group(1));
			start = (n + m.group()
			              .length());
		}

		text = tf.getCodedText();
		start = diff = 0;
		m = patternClosing.matcher(text);
		while (m.find(start)) {
			n = m.start();
			diff += tf.changeToCode(n + diff,
			                        (n + diff) + m.group()
			                                      .length(),
			                        TextFragment.TagType.CLOSING,
			                        m.group(1));
			start = (n + m.group()
			              .length());
		}

		text = tf.getCodedText();
		start = diff = 0;
		m = patternPlaceholder.matcher(text);
		while (m.find(start)) {
			n = m.start();
			diff += tf.changeToCode(n + diff,
			                        (n + diff) + m.group()
			                                      .length(),
			                        TextFragment.TagType.PLACEHOLDER,
			                        null);
			start = (n + m.group()
			              .length());
		}
		return tf;
	}

	// SV 2014-04-19 made public to fix java.lang.IllegalAccessError when called from a child Tikal process (of jar switcher)
	public static class Timer {
		private long startMillis = System.currentTimeMillis();

		public double elapsedSeconds() {
			return (double) (System.currentTimeMillis() - startMillis) / 1000;
		}

		@Override
		public String toString() {
			return "" + elapsedSeconds() + "s";
		}
	}
}
