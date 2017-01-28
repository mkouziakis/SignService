# SignService

Java application that is watching for new files in a specified file path. When a new file is written, the app reads the contents of the file and creates a hash using DSA algorithm and the digital certicate provided in a .pfx file. Then writes the hash in a new file with a .sig extension.
