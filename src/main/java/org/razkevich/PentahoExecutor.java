package org.razkevich;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.KettleLoggingEvent;
import org.pentaho.di.core.logging.KettleLoggingEventListener;
import org.pentaho.di.core.logging.LogMessage;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PentahoExecutor {

	private String directory;
	private HashMap<String, String> scriptPropertiesToInject = new HashMap<String, String>();
	private Logger logger;
	private Properties applicationProperties, mappingProperties;
	private ExecutorService executorService = Executors.newFixedThreadPool(4);

	public PentahoExecutor(String directory, Logger logger, Properties applicationProperties, Properties mappingProperties) {
		this.directory = directory;
		this.logger = logger;
		this.applicationProperties = applicationProperties;
		this.mappingProperties = mappingProperties;
	}

	public PentahoExecutor(Properties applicationProperties, Properties mappingProperties) {
		this(null, null, applicationProperties, mappingProperties);
	}

	public void unzip() throws IOException {
		File[] files;
		if (StringUtils.isBlank(directory) || ArrayUtils.isEmpty(files = new File(directory).listFiles()))
			throw new IllegalArgumentException("folder with archives not specified");
		for (final File file : files) {
			executorService.submit(new Callable() {
				public Object call() throws java.io.IOException {
					if (!file.isDirectory() && Helper.checkWithRegExp(file.toString(), applicationProperties.getProperty("zipFileRegexp")))
						Helper.unzip(file.toString(), file.getParent() + "\\out");
					return null;
				}
			});
		}
	}

	public void initScriptProperties() throws InterruptedException, FileNotFoundException {
		final File[] files;
		if (StringUtils.isBlank(directory) || ArrayUtils.isEmpty(files = new File(new File(directory).toString() + "\\out").listFiles()))
			throw new IllegalArgumentException("folder with archives not specified");
		final AtomicInteger filesFound = new AtomicInteger(0);
		executorService.invokeAll(new ArrayList<Callable<Object>>() {{
			for (final File file : files) {
				add(new Callable<Object>() {
					public Object call() throws Exception {
						for (Map.Entry<Object, Object> entry : mappingProperties.entrySet())
							if (Helper.checkWithRegExp(file.getName(), (String) entry.getValue())) {
								scriptPropertiesToInject.put((String) entry.getKey(), file.toString());
								logger.info(String.format("%s will be loaded to: %s", file.toString(), entry.getKey()));
								filesFound.incrementAndGet();
							}
						return null;
					}
				});
			}
		}});

		if (filesFound.get() != mappingProperties.size()) {
			String msg = String.format("%s files found out of %s. Please check mapping configuration (see file dbmapping.properties)", filesFound, mappingProperties.size());
			logger.error(msg);
			throw new FileNotFoundException(msg);
		}
		for (Map.Entry e : applicationProperties.entrySet())
			scriptPropertiesToInject.put(e.getKey().toString(), e.getValue().toString());
		scriptPropertiesToInject.put("workdir", directory);
		executorService.shutdown();
		executorService.awaitTermination(100, TimeUnit.MINUTES);
	}

	public HashMap<String, String> getScriptPropertiesToInject() throws FileNotFoundException, InterruptedException {
		if (scriptPropertiesToInject == null)
			initScriptProperties();
		return scriptPropertiesToInject;
	}

	public void executeScript(String path) throws KettleException, IOException, InterruptedException {
		KettleEnvironment.init();
		KettleLogStore.getAppender().addLoggingEventListener(new KettleLoggingEventListener() {
			public void eventAdded(KettleLoggingEvent kettleLoggingEvent) {
				final LogMessage message = (LogMessage) kettleLoggingEvent.getMessage();
				String log = String.format("Subject: %s; Message: %s", message.getSubject(), message.getMessage());
				if (message.isError()) logger.error("Error " + log);
				else logger.info(log);
			}
		});
		JobMeta jobMeta = new JobMeta(this.getClass().getResource(path).toString(), null);
		Job job = new Job(null, jobMeta);
		job.injectVariables(getScriptPropertiesToInject());
		job.run();
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	void setLogger(Logger logger) {
		this.logger = logger;
	}
}
