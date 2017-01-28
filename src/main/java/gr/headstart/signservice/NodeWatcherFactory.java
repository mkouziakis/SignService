/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.headstart.signservice;

import gr.headstart.signservice.nodetypes.TestNodeWatcher;

/**
 * @author KouziaMi
 *
 * Factory, creation of Filewatcher objects based on configuration
 */

public final class NodeWatcherFactory {
    private static final String TESTNODE = "TESTNODE";

    private NodeWatcherFactory() {}

    public static NodeWatcher getInstance(SignServiceProperties props) {
        switch (props.getType()) {
            case TESTNODE:
                return new TestNodeWatcher(props);
            default:
                throw new RuntimeException("Implementation for type " + props.getType() + " is not ready yet");
        }
    }
}
