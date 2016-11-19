package edu.utdallas.cs5390.group3.client;

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
    private static final String DEFAULT_SERVER_ADDR = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 9876;
    private static final boolean DEFAULT_DEBUG_MODE = false;
    // 3 seconds
    private static final int DEFAULT_TIMEOUT_INTERVAL = 3000;

    // Bookkeeping
    private String _configFileName;

    // Properties
    private InetAddress _serverAddr;
    private int _serverPort;
    private boolean _debug;
    private int _clientId;
    private String _privateKey;
    private int _timeoutInterval;

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
        if(_debug) {
            Console.enterDebugMode();
        }
    }

    // =========================================================================
    // Property Accessors
    // =========================================================================

    public InetAddress serverAddr() {
        return _serverAddr;
    }

    public int serverPort() {
        return _serverPort;
    }

    public boolean debug() {
        return _debug;
    }

    public int clientId() {
        return _clientId;
    }

    public String privateKey() {
        return _privateKey;
    }

    public int timeoutInterval() {
        return _timeoutInterval;
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
        _serverAddr = this.validateServerAddr(props);
        _serverPort = this.validateServerPort(props);
        _debug = this.validateDebug(props);
        _clientId = this.validateClientId(props);
        _privateKey = this.validatePrivateKey(props);
        _timeoutInterval = this.validateTimeoutInterval(props);
    }

    /* Validates 'server_addr' property.
     *
     * Rules:
     *   Can be ommited (has default)
     *   Can be '*' (will be translated to 0.0.0.0)
     *   Must be resolvable.
     *
     * @return Validated InetAddress object.
     */
    private InetAddress validateServerAddr(final Properties props)
        throws NullPointerException {

        String serverAddrProp = props.getProperty("server_addr");

        // default value if ommitted.
        if (serverAddrProp == null) {
            serverAddrProp = Config.DEFAULT_SERVER_ADDR;
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
            Console.fatal("Could not resolve 'server_addr' property in "
                          + "'" + _configFileName + "' to a valid host: "
                          + serverAddrProp);
            throw new NullPointerException();
        }

        // Validation succeeded.
        return serverAddr;
    }

    /* Validates 'server_port' property.
     *
     * Rules:
     *   Can be omitted (has default)
     *   Must be integer
     *   Between 0 and 65535
     *
     * @return Validated port number.
     */
    private int validateServerPort(final Properties props)
        throws NullPointerException {

        String serverPortProp = props.getProperty("server_port");

        // Default value if ommitted.
        if (serverPortProp == null) {
            return Config.DEFAULT_SERVER_PORT;
        }

        // Check format.
        if (!serverPortProp.matches("^[0-9]+$")) {
            Console.fatal("Malformed 'server_port' property in "
                          + "'" + _configFileName + "': '"
                          + serverPortProp + "' (must be 0-65535)");
            throw new NullPointerException();
        }

        // Check bounds.
        int serverPort = Integer.parseInt(serverPortProp);
        if (serverPort < 0 || serverPort > 65535) {
            Console.fatal("Specified 'server_port' property in "
                            + "'" + _configFileName + "' is out-of-range: '"
                            + serverPort + "' (must be 0-65535)");
            throw new NullPointerException();
        }

        // Validation succeeded.
        return serverPort;
    }

    /* Validate 'debug' property.
     *
     * Rules:
     *   Can be omitted (has default)
     *   Must be true/false, yes/no, on/off
     *
     * @return Validated debug mode.
     */
    private boolean validateDebug(final Properties props) throws NullPointerException {
        String debugProp = props.getProperty("debug");

        // Default value if ommitted.
        if (debugProp == null) {
            return Config.DEFAULT_DEBUG_MODE;
        }

        if (debugProp.matches("(?i)^(true|on|yes)\\s*$"))
            return true;
        else if (debugProp.matches("(?i)^(false|off|no)\\s*$"))
            return false;
        else {
            Console.fatal("Specified 'debug' property in "
                            + "'" + _configFileName + "' is invalid: '"
                            + " Must be 'true/false', 'yes/no', or 'on/off'");
            throw new NullPointerException();
        }
    }

    /* Validate 'client_id' property.
     *
     * Rules:
     *   Required property (can't omit)
     *   Must be a non-negative integer
     *
     * @return Validated debug mode.
     */
    private int validateClientId(final Properties props)
        throws NullPointerException {

        String clientIdProp = props.getProperty("client_id");

        if (clientIdProp == null || clientIdProp.equals("")) {
            Console.fatal("Missing required property 'client_id' in "
                          + "'" + _configFileName + "'");
            throw new NullPointerException();
        }

        if (!clientIdProp.matches("^[0-9]*$")) {
            Console.fatal("Malformed 'client_id' property in "
                          + "'" + _configFileName + "': "
                          + clientIdProp);
            throw new NullPointerException();
        }

        return Integer.parseInt(clientIdProp);
    }

    /* Validate 'private_key' property.
     *
     * Rules:
     *   Required property (can't omit)
     *   Can't be the empty string
     *
     * @return Validated debug mode.
     */
    private String validatePrivateKey(final Properties props)
        throws NullPointerException {

        String pkProp = props.getProperty("private_key");

        if (pkProp == null || pkProp.equals("")) {
            Console.fatal("Missing or empty required property 'private_key' "
                          + "in '" + _configFileName + "'");
            throw new NullPointerException();
        }

        return pkProp;
    }

    /* Validates 'timeout_interval' property
     *
     * @return Validated timeout interval.
     */
    private int validateTimeoutInterval(final Properties props)
        throws NullPointerException {

        String timeoutIntervalProp = props.getProperty("timeout_interval");

        // Default value if ommitted.
        if (timeoutIntervalProp == null) {
            return Config.DEFAULT_TIMEOUT_INTERVAL;
        }

        // Check format.
        if (!timeoutIntervalProp.matches("^[0-9]+$")) {
            Console.fatal("Malformed 'timeout_interval' property in "
                          + "'" + _configFileName + "': '"
                          + timeoutIntervalProp
                          + "' (must be non-negative integer)");
            throw new NullPointerException();
        }
        int timeoutInterval = Integer.parseInt(timeoutIntervalProp);

        // Validation succeeded.
        return timeoutInterval;
    }
}
