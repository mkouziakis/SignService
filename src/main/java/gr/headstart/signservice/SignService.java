package gr.headstart.signservice;

import org.apache.commons.lang3.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SignService application. Watches for new files created in a specified path. For every
 * new file, a .sig file is created containing the digital signature hash of the file contents.
 *
 * @author KouziaMi
 */

public class SignService {
    public static final Logger logger = Logger.getLogger(SignService.class.getName());

    public static void main(String[] args) {
        SignServiceProperties props = new SignServiceProperties();
        final String watchPath = props.getWatchPath();
        if (StringUtils.isNotBlank(watchPath)) {
            logger.log(Level.INFO, "Watch path is {0}", watchPath);
            // Start a new thread to watch for changes in the specified path
            Thread mainThread = new Thread(NodeWatcherFactory.getInstance(props));
            mainThread.start();
        } else {
            // Path has not been set
            logger.log(Level.SEVERE, "Watch path has not been set. SignService cannot start");
            System.exit(1);
        }
    }
}
