package host.thanco.NeptuneBase;

import java.io.File;
// import java.io.FileOutputStream;
import java.io.FileReader;
// import java.io.OutputStreamWriter;

import com.google.gson.GsonBuilder;

import host.thanco.Configurator.Configuration;

public class ConfigurationHandler {
    private final String CONFIGURATION_PATH = "json/configuration.json";
    private Configuration configuration;

    public ConfigurationHandler() {
        initConfiguration();
    }

    private void initConfiguration() {
        if (!new File(CONFIGURATION_PATH).exists()) {
            try {
                new File(CONFIGURATION_PATH).createNewFile();
                configuration = new Configuration().setDefaultConfiguration();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(CONFIGURATION_PATH)) {
            configuration = new GsonBuilder().setPrettyPrinting().create().fromJson(reader, Configuration.class);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // public void saveConfiguration() {
    //     try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CONFIGURATION_PATH))) {
    //         new GsonBuilder().setPrettyPrinting().create().toJson(configuration, writer);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    
}
