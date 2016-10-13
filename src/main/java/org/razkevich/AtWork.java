package org.razkevich;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Properties;

import static org.razkevich.Helper.printHelp;

public class AtWork {

	private static final ApplicationContext SPRING_CONTEXT =
			new AnnotationConfigApplicationContext(org.razkevich.spring.ApplicationContext.class);

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption(OptionBuilder
				.isRequired(true)
				.hasArg()
				.withDescription("Use this flag to set directory containing SEPA archives, e.g. -dC:\\sepa_data\\")
				.withLongOpt("directory")
				.create("d"));
		CommandLine cmd = null;
		try {
			cmd = new PosixParser().parse(options, args);
		} catch (Exception e) {
			printHelp(e.toString(), options, SPRING_CONTEXT.getBean("standardLogger", Logger.class));
			System.exit(-1);
		}
		PentahoExecutor executor = SPRING_CONTEXT.getBean(PentahoExecutor.class);
		executor.setDirectory(cmd.getOptionValue("directory"));
		executor.setLogger(SPRING_CONTEXT.getBean("fileOnlyLogger", Logger.class));
		try {
			executor.unzip();
			executor.initScriptProperties();
			executor.executeScript(SPRING_CONTEXT.getBean("applicationProperties", Properties.class).getProperty("script_path"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
