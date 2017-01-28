package gr.headstart.signservice.nodetypes;

import gr.headstart.signservice.NodeWatcher;
import gr.headstart.signservice.SignServiceProperties;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Test implementation of the provided node
 *
 * @author KouziaMi
 */
public class TestNodeWatcher extends NodeWatcher {
    private static final Logger logger = Logger.getLogger(TestNodeWatcher.class.getName());
    
    public TestNodeWatcher(SignServiceProperties props) {
        super(props);
    }

    @Override
    protected String getUnsignedPath() {
        return "/testpath/";
    }

    @Override
    protected String getCodepage() {
        return "Cp1253";
    }
    
    @Override
    public void validateCRC(String completeFileName, String fileName) {
        //Read the file into a configuration object
        logger.log(Level.INFO, "Start validation for file: {0}", new Object[]{completeFileName});
        RESCollection measurements = new RESCollection();
        BufferedReader reader = null;
        try {
            //The file seems to be ready for reading. Proceed loading
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(completeFileName), getCodepage()));
            measurements.load(reader);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while loading measurements file: " + fileName, ex);
        } finally {
            //close reader to release the file
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException ex) {}
            }
        }
        //validate the CRC
        try {
            for (String key : measurements) {
                String value = measurements.getMeasurement(key);
                String crc = measurements.getCRC(key);
                String calculatedCRC = getCRC(value);
                //validate CRC for each key-value pair
                if (crc != null) {
                    if (!crc.equals(calculatedCRC)) {
                        logger.log(Level.SEVERE, "Problem while validating crc. "
                                + "Key: {0} Value: {1} Original CRC: {2} Calculated crc: {3}."
                                + "Aborting file signing for file {4}", new Object[]{key, value, crc, calculatedCRC, completeFileName});
                    }
                } else {
                    logger.log(Level.SEVERE, "Problem while validating crc: crc value cannot be found in the file. Probably the is incomplete");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Generic problem while validating crc", e);
        }
    }
        
    private String getCRC(String s) {
        int iso88597AsciiAlphaCharCode = 193;
        int utf8AlphaCharCode = Character.codePointAt(new char[]{'A'}, 0);
        int difference = utf8AlphaCharCode - iso88597AsciiAlphaCharCode;
        try {
            char[] arr = s.toCharArray();
            int crcValue = 0;
            for (int i = 0; i < arr.length; i++) {
                int utfCharCode = Character.codePointAt(arr, i);
                int asciiCharCode = utfCharCode > 127 ? utfCharCode - difference : utfCharCode;
                crcValue += asciiCharCode * ((i % 5) + 1);
            }
            s = Integer.toString(crcValue);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while generating crc for " + s, e);
        }
        return s;
    }
    
}
