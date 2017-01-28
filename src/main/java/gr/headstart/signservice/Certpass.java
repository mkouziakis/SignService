package gr.headstart.signservice;

/**
 * Holder of the password for the DS certificate as a char array.
 * We must come up with something much more secure...
 *
 * @author KouziaMi
 */
public final class Certpass {
    private static final String TEST_PROVIDER = "TEST_PROVIDER";
    private Certpass() {}

    public static char[] getPass(SignServiceProperties props) {
        switch (props.getProvider()) {
            case TEST_PROVIDER:
                return new char[]{52, 50, 55, 49, 52, 49, 53, 57};
            default:
                throw new RuntimeException("Provider " + props.getProvider() + " is not registered");
        }
    }
}
