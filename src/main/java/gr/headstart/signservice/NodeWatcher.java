package gr.headstart.signservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nodewatcher runnable base class, will be used for different implementations of crc checking
 *
 * @author KouziaMi
 */

public abstract class NodeWatcher implements Runnable {
    private static final Logger logger = Logger.getLogger(NodeWatcher.class.getName());
    private SignServiceProperties props;

    public NodeWatcher(SignServiceProperties props) {
        this.props = props;
    }

    protected abstract String getUnsignedPath();

    protected abstract String getCodepage();

    protected abstract void validateCRC(String completeFileName, String fileName);

    @Override
    public void run() {
        WatchService watcher = null;
        WatchKey watchKey = null;
        try {
            logger.log(Level.INFO, "Watching for changes");
            try {
                //watch the unsigned path
                Path path = Paths.get(props.getWatchPath() + getUnsignedPath());
                watcher = path.getFileSystem().newWatchService();
                //Watching only for new or updated files
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

            while (true) {
                watchKey = watcher.take();
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    Path filePath = ((WatchEvent<Path>) event).context();
                    String fileName = filePath.toString();
                    //Check the file to ensure it is not a signature file
                    if (!fileName.endsWith(".sig") && !filePath.toFile().isDirectory() && !fileName.contains("qt_temp")) {
                        signFile(fileName);
                        if (props.getForceValidation()) {
                            validateSignature(fileName);
                        }
                    }
                }

                boolean valid = watchKey.reset();
                if (!valid) {
                    logger.log(Level.WARNING, "Watcher is not valid anymore. Closing...");
                    try {
                        watcher.close();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Problem while trying to close watcher", e);
                    }
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "RESWatcher interrupted. Closing");
        } finally {
            try {
                watcher.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while closing watcher.Not important, the service is closing anyway");
            }
        }
    }

    /**
     * Orchestrates the file signing procedure
     *
     * @param fileName
     */
    private void signFile(String fileName) {
        String completeFileName = readFile(fileName);
        validateCRC(completeFileName, fileName);
        createSignature(completeFileName);
    }

    /**
     * Reads the contents of a file. We are using this in order to be ready that the file
     * update has finished and it ready to be processed. We copy the file to a new path
     * where it will be hidden to the device user.
     *
     * @param fileName
     * @return the complete filename of the copied file
     */
    private String readFile(String fileName) {
        String completeFileName = props.getWatchPath() + "/" + fileName;
        String completeHiddenFileName = props.getWatchPath() + getUnsignedPath() + "/" + fileName;
        //copy the file into the res directory
        BufferedInputStream copyBufin = null;
        BufferedOutputStream copyBufout = null;
        try {
            //Open a reader
            File f = new File(completeHiddenFileName);

            //Check if the file is ready
            while (!f.canRead()) {
                //if not wait for half a second
                try {
                    logger.log(Level.SEVERE, "Waiting for the file {0} to be ready for reading: ", fileName);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    continue;
                }
            }

            //Try to open the file 5 times every 500 millis
            for (int i = 0; i < 5; i++) {
                //if not wait for half a second
                try {
                    logger.log(Level.INFO, "Attempt {0} to open the file: {1}", new Object[]{i + 1, fileName});
                    copyBufin = new BufferedInputStream(new FileInputStream(completeHiddenFileName));
                    copyBufout = new BufferedOutputStream(new FileOutputStream(completeFileName));
                    byte[] buffer = new byte[1024];
                    int len;
                    while (copyBufin.available() != 0) {
                        len = copyBufin.read(buffer);
                        copyBufout.write(buffer, 0, len);
                    }
                    break;
                } catch (Exception e) {
                    //close the stream if necessary first
                    try {
                        copyBufin.close();
                        logger.log(Level.SEVERE, "Error while trying to open the inputstream for the file {0}", fileName);
                    } catch (Exception ge) {
                        logger.log(Level.SEVERE, "Error while trying to close the inputstream for the file {0}", fileName);
                    }
                    try {
                        logger.log(Level.SEVERE, "Error while trying to open the file {0}. Waiting 500 millis", fileName);
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        continue;
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while copying file from unsigned area: " + fileName, ex);
            return null;
        } finally {
            //close buffers to release the file
            if (copyBufin != null) {
                try {
                    copyBufin.close();
                } catch (IOException ex) {
                }
            }
            if (copyBufout != null) {
                try {
                    copyBufout.close();
                } catch (IOException ex) {
                }
            }
        }
        return completeFileName;
    }

    /**
     * Creates the signature file
     *
     * @param completeFileName
     */
    private void createSignature(String completeFileName) {
        //Read the private key
        PrivateKey privateKey = null;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream ksfis = new FileInputStream(props.getKeystoreFile());
            BufferedInputStream ksbufin = new BufferedInputStream(ksfis);

            ks.load(ksbufin, Certpass.getPass(props));

            privateKey = (PrivateKey) ks.getKey(props.getCertAlias(), Certpass.getPass(props));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while reading the private key", ex);
            return;
        }

        //init a signature instance
        Signature dsa = null;
        try {
            dsa = Signature.getInstance("SHA1withRSA", "BC");
            dsa.initSign(privateKey);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while initializing the signature", ex);
            return;
        }

        //apply the signature
        try {
            FileInputStream fis = new FileInputStream(completeFileName);
            BufferedInputStream bufin = new BufferedInputStream(fis);
            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                dsa.update(buffer, 0, len);
            }

            bufin.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while applying the signature", ex);
            return;
        }

        // write the signature hash
        try {
            byte[] realSig = dsa.sign();

            FileOutputStream sigfos = new FileOutputStream(completeFileName + ".sig");
            sigfos.write(realSig);
            sigfos.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while signature hash file", ex);
        }
    }

    /**
     * Validates that the signature file is original.
     * @param fileName
     */
    private void validateSignature(String fileName) {
        String completeFileName = props.getWatchPath() + "/" + fileName;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream ksfis = new FileInputStream(props.getKeystoreFile());
            BufferedInputStream ksbufin = new BufferedInputStream(ksfis);

            ks.load(ksbufin, Certpass.getPass(props));
            Certificate cer = ks.getCertificate(props.getCertAlias());

            // input the signature bytes
            FileInputStream sigfis = new FileInputStream(completeFileName + ".sig");
            byte[] sigToVerify = new byte[sigfis.available()];
            sigfis.read(sigToVerify);

            sigfis.close();

            // create a Signature object and initialize it with the public key
            Signature sig = Signature.getInstance("SHA1withRSA", "BC");
            sig.initVerify(cer);

            // Update and verify the data
            FileInputStream datafis = new FileInputStream(completeFileName);
            BufferedInputStream bufin = new BufferedInputStream(datafis);

            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                sig.update(buffer, 0, len);
            };
            bufin.close();

            if (!sig.verify(sigToVerify)) {
                throw new Exception("Cannot verify signature");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot verify data integrity ", e);
            return;
        }
        logger.log(Level.INFO, "Validation successfull!!!");
    }
}
