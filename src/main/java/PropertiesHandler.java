import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/** use this program to get and set properties
 * set file name and comment before using it or use defaults
  */
public class PropertiesHandler {
    private static String getPropertiesFileName(){return propertiesFileName;}
    private static String propertiesFileName = "config.properties";
    private static String comment = "program properties";

    public void setPropertiesFileComment(String comment) {
        this.comment = comment;
    }
    public void setPropertiesFileName(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    public static void setProperty(String key, String value) throws IOException {
        Properties properties = new Properties();
        try(FileReader fileReader = new FileReader(propertiesFileName)){
            properties.load(fileReader);
        }catch(FileNotFoundException e){};//file not created yet.. will be created in the next line..
        properties.setProperty(key, value);
        try (FileWriter output = new FileWriter(propertiesFileName)) {
            properties.store(output, comment);
        }
    }
    public static String getProperty(String key,String defaultValue) throws IOException {
        Properties properties = new Properties();
        String value=defaultValue;
        try(FileReader fileReader = new FileReader(propertiesFileName)){
            properties.load(fileReader);
            value = properties.getProperty(key, defaultValue);
        }catch(FileNotFoundException e){};//file not created yet.. will be created in the next line..
        properties.setProperty(key, value);//prepare to store it with the original / default value
        try (FileWriter output = new FileWriter(propertiesFileName)) {
            properties.store(output, comment);
        }
        return value;
    }


}
