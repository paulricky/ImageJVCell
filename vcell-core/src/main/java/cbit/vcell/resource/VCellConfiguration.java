package cbit.vcell.resource;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.FileBasedBuilderParameters;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vcell.util.ConfigurationException;

public class VCellConfiguration {
	private final static Logger lg = LogManager.getLogger(VCellConfiguration.class);

	private static FileBasedConfigurationBuilder<PropertiesConfiguration> configurationBuilder = null;
	private static PropertiesConfiguration propertiesConfiguration = null;

	private static synchronized Configuration getConfiguration() throws ConfigurationException {
		if (configurationBuilder==null){
			Parameters params = new Parameters();
			File propertiesFile = new File(ResourceUtil.getVcellHome(),"vcellconfig.properties");
			if (!propertiesFile.exists()){
				try {
					propertiesFile.createNewFile();
				} catch (IOException e) {
					lg.error(e);
				}
			}
			FileBasedBuilderParameters thing = params.fileBased().setFile(propertiesFile);
			configurationBuilder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class).configure(thing);
			configurationBuilder.setAutoSave(true);
			try {
				propertiesConfiguration = configurationBuilder.getConfiguration();
				propertiesConfiguration.setSynchronizer(new ReadWriteSynchronizer());
			}catch (org.apache.commons.configuration2.ex.ConfigurationException e){
				lg.error(e);
				throw new ConfigurationException("failed to create configuration from file "+propertiesFile+": "+e.getMessage());
			}
		}
		return propertiesConfiguration;
	}
	
	public static void showProperties(){
		Iterator<String> keys = getConfiguration().getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			System.out.println(key + " = " + getConfiguration().getProperty(key));
		}
	}

	public static File getFileProperty(String filePropertyKey) {
		String filename = getConfiguration().getString(filePropertyKey);
		if (filename == null){
			return null;
		}else{
			return new File(filename.replaceAll("\"", ""));
		}
	}

	public static File setFileProperty(String filePropertyKey, File file) {
		getConfiguration().setProperty(filePropertyKey, "\""+file.getAbsolutePath()+"\"");
		return getFileProperty(filePropertyKey);
	}
	public static File resetFileProperty(String filePropertyKey) {
		getConfiguration().setProperty(filePropertyKey, null);	// property will go away
		return getFileProperty(filePropertyKey);
	}

	public static String getValue(String stringPropertyKey, String stringPropertyDefault) {
		String value = getConfiguration().getString(stringPropertyKey);
		if (value!=null){
			return value;
		}else{
			return stringPropertyDefault;
		}
	}

	public static void putValue(String stringPropertyKey, String value) {
		getConfiguration().setProperty(stringPropertyKey, value);
	}	
	

}
