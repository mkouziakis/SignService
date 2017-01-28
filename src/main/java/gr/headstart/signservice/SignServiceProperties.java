package gr.headstart.signservice;

import org.apache.commons.configuration.HierarchicalINIConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proxy for accessing application properties stored in
 * signservice.properties file.
 *
 * @author KouziaMi
 */

public class SignServiceProperties {

    private HierarchicalINIConfiguration properties;

    public SignServiceProperties() {
        try {
            properties = new HierarchicalINIConfiguration("signservice.properties");
        } catch (Exception ex) {
            Logger.getLogger(SignServiceProperties.class.getName()).log(Level.SEVERE,
                    "Error while loading codes configuration. Returning null", ex);
        }
    }

    public String getWatchPath() {
        return properties.getString("generic.watch_path");
    }

    public boolean getForceValidation() {
        return Boolean.valueOf(properties.getString("generic.force_validation"));
    }

    public String getKeystoreFile() {
        return properties.getString("keystore.file");
    }

    public String getCertAlias() {
        return properties.getString("keystore.cert_alias");
    }

    public String getType() {
        return properties.getString("generic.type");
    }

    public String getProvider() {
        return properties.getString("generic.provider");
    }
}
