package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.util.Properties;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.io.FileInputStream;
import java.io.InputStream;

import java.io.IOException;
import java.lang.IllegalStateException;
import java.lang.NullPointerException;
import java.net.UnknownHostException;

/* The Config class is a wrapper around the configuration
 * file. It uses java.util.Properties for low-level file parsing,
 * validates the raw property strings, and converts them to accessor
 * methods that return an appropriate type.
 */
public final class Config {
    // Defaults
    private static final String DEFAULT_BIND_ADDR = "0.0.0.0";
    private static final int DEFAULT_BIND_PORT = 9876;
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static final int[] DEFAULT_USER_IDS = {1,2};
    private static final String[] DEFAULT_PRIVATE_KEYS = {"foo","bar"};

    // Bookkeeping
    private String _configFileName;

    // Properties
    private InetAddress _bindAddr;
    private int _bindPort;
    private boolean _debugMode;
    private int[] _userIDs;
    private String[] _privateKeys;

    // =========================================================================
    // Constructor
    // =========================================================================

    /* Initializes a Config object from a java.util.Properties style
     * config file.
     *
     * @param configFileName Name of the config file to parse.
     */
    public Config(final String configFileName)
        throws NullPointerException {

        // Parse config file with java.util.Properties
        _configFileName = configFileName;
        Properties props = this.loadProperties();
        if (props == null) {
            throw new NullPointerException(
                "Could not load configuration file: '"
                + _configFileName + "'");
        }

        // Validate properties
        try {
            this.validate(props);
        } catch (NullPointerException e) {
            // If any property failed to validate, die.
            throw new NullPointerException();
        }

        // Since Console is a static class, its static blocks may be
        // executed before the Config(). In other words, it can't
        // configure itself, so we have to configure it from here
        // instead.
        if(_debugMode)
            Console.enterDebugMode();
    }

    // =========================================================================
    // Property Accessors
    // =========================================================================

    public InetAddress bindAddr() {
        return _bindAddr;
    }

    public int bindPort() {
        return _bindPort;
    }

    public boolean debugMode() {
        return _debugMode;
    }

    public int[] userIDs() {
        return _userIDs;
    }

    public String[] privateKeys() {
        return _privateKeys;
    }

    // =========================================================================
    // Initialization
    // =========================================================================

    /* Perorms low-level file parsing with java.util.Properties
     *
     * @return The initialized Properties object
     */
    private Properties loadProperties() {
        InputStream configFile = null;
        Properties props = new Properties();
        try {
            configFile = new FileInputStream(_configFileName);
            props.load(configFile);
        } catch (IOException e) {
            Console.fatal("IOException while parsing "
                          + "'" + _configFileName + "': " + e);
            return null;
        }
        if (configFile != null) {
            try {
                configFile.close();
            } catch (IOException e) {
                // Not fatal, but certainly strange.
                Console.warn("IOException while closing "
                            + "'" + _configFileName + "': " + e);
            }
        }
        return props;
    }

    // =========================================================================
    // Property Validation
    // =========================================================================

    /* Call each property's validator.
     *
     * @throws NullPointerException Thrown if any property is null
     * (fails validation).
     */
    private void validate(Properties props) throws NullPointerException {
        _bindAddr = this.validateBindAddress(props);
        _bindPort = this.validateBindPort(props);
        _debugMode = this.validateDebug(props);
        _userIDs = this.validateUserIDs(props);
        _privateKeys = this.validatePrivateKeys(props, _userIDs.length);
    }

    /* Validates 'bind_addr' property.
     *
     * @return Validated InetAddress object.
     */
    private InetAddress validateBindAddress(final Properties props)
        throws NullPointerException {

        String serverAddrProp = props.getProperty("bind_addr");

        // default value if ommitted.
        if (serverAddrProp == null) {
            serverAddrProp = Config.DEFAULT_BIND_ADDR;
        } else if (serverAddrProp.equals("*")) {
            // translate shorthand '*' -> '0.0.0.0'
            serverAddrProp = "0.0.0.0";
        }

        // Use InetAddress.getByName to validate the address.
        // The address isn't necessarily bindable (could be non-local), but
        // we'll kick that can to WelcomeSocket.open().
        InetAddress serverAddr;
        try {
            serverAddr = InetAddress.getByName(serverAddrProp);
        } catch (UnknownHostException e) {
            Console.fatal("Could not resolve 'bind_addr' property in "
                          + "'" + _configFileName + "' to a valid host: "
                          + serverAddrProp);
            throw new NullPointerException();
        }

        // Validation succeeded.
        return serverAddr;
    }

    /* Validates 'bind_port' property
     *
     * @return Validated port number.
     */
    private int validateBindPort(final Properties props)
        throws NullPointerException {

        String bindPortProp = props.getProperty("bind_port");

        // Default value if ommitted.
        if (bindPortProp == null) {
            return Config.DEFAULT_BIND_PORT;
        }

        // Check format.
        if (!bindPortProp.matches("^[0-9]+$")) {
            Console.fatal("Malformed 'bind_port' property in "
                          + "'" + _configFileName + "': '"
                          + bindPortProp + "' (must be 0-65535)");
            throw new NullPointerException();
        }

        // Check bounds.
        int bindPort = Integer.parseInt(bindPortProp);
        if (bindPort < 0 || bindPort > 65535) {
            Console.fatal("Specified 'bind_port' property in "
                            + "'" + _configFileName + "' is out-of-range: '"
                            + bindPort + "' (must be 0-65535)");
            throw new NullPointerException();
        }

        // Validation succeeded.
        return bindPort;
    }

    /* Validate 'debug' property.
     *
     * @return Validated debug mode.
     */
    private boolean validateDebug(final Properties props) throws NullPointerException {
        String debugProp = props.getProperty("debug");

        // Default value if ommitted.
        if (debugProp == null) {
            return Config.DEFAULT_DEBUG_MODE;
        }

        if (debugProp.equalsIgnoreCase("true"))
            return true;
        else if (debugProp.equalsIgnoreCase("false"))
            return false;
        else {
            Console.fatal("Specified 'debug' property in "
                            + "'" + _configFileName + "' is invalid: '"
                            + " Must be 'true' or 'false'");
            throw new NullPointerException();
        }
    }

    private int[] validateUserIDs(final Properties props)
        throws NullPointerException {

        String userProp = props.getProperty("user_ids");

        // Default value if ommitted.
        if (userProp == null || userProp.equals("")) {
            return DEFAULT_USER_IDS;
        }

        String[] userPropArr = userProp.split(",");
        int[] userIDs = new int[userPropArr.length];

        for(int i=0; i<userPropArr.length; i++)
            if (userPropArr[i].matches("^[1-9][0-9]*$")) {
                userIDs[i] = Integer.parseInt(userPropArr[i]);
            } else {
                Console.fatal("Malformed 'user_id' property in "
                              + "'" + _configFileName + "': "
                              + userPropArr[i]);
                throw new NullPointerException();
            }

        return userIDs;
    }

    private String[] validatePrivateKeys(final Properties props, final int userIDsLength)
        throws NullPointerException {

        String pkProp = props.getProperty("private_keys");

        // Default value if ommitted.
        if (pkProp == null || pkProp.equals("")) {
            return DEFAULT_PRIVATE_KEYS;
        }

        String[] pkPropArr = pkProp.split(",");

        if(pkPropArr.length != userIDsLength){
            Console.fatal("The number of user IDs is not equal to " +
                "the number of private keys in '" + _configFileName + "'");
            throw new NullPointerException();
        }

        for(String pk: pkPropArr)
            if (pk.equals("")) {
                Console.fatal("Empty private key in "
                              + "'" + _configFileName + "'");
                throw new NullPointerException();
            }

        return pkPropArr;
    }

}
