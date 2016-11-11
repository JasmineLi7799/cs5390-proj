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

public final class Config {
    // Defaults
    private static final String DEFAULT_BIND_ADDR = "0.0.0.0";
    private static final int DEFAULT_BIND_PORT = 9876;
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static final int[] DEFAULT_USER_IDS = {1,2};
    private static final String[] DEFAULT_PRIVATE_KEYS = {"foo","bar"};

    private static Config _instance;

    // Bookkeeping
    private boolean _haveInit;
    private String _configFileName;

    // Configuration properties
    private InetAddress _bindAddr;
    private int _bindPort;
    private boolean _debugMode;
    private int[] _userIDs;
    private String[] _privateKeys;

    private Config() {
        _haveInit = false;
    }

    public static Config instance() {
        if (_instance == null) {
            _instance = new Config();
        }
        return _instance;
    }

    public InetAddress bindAddr() {
        this.checkGetState();
        return _bindAddr;
    }

    public int bindPort() {
        this.checkGetState();
        return _bindPort;
    }

    public boolean debugMode() {
        this.checkGetState();
        return _debugMode;
    }

    public int[] userIDs() {
        this.checkGetState();
        return _userIDs;
    }

    public String[] privateKeys() {
        this.checkGetState();
        return _privateKeys;
    }

    private void checkGetState() {
        if (!_haveInit) {
            throw new IllegalStateException(
                "Must call Config.init() before getting properties.");
        }
    }

    public boolean init(final String configFileName) {
        if (_haveInit) {
            throw new IllegalStateException(
                "Duplicate call to Config.init().");
        }
        _configFileName = configFileName;

        // Parse config file
        Properties props = this.loadProperties();
        if (props == null) {
            Console.fatal("Could not load configuration file: '"
                          + _configFileName + "'");
            return false;
        }

        // Validate properties
        try {
            _bindAddr = this.validateBindAddress(props);
            _bindPort = this.validateBindPort(props);
            _debugMode = this.validateDebug(props);
            _userIDs = this.validateUserIDs(props);
            _privateKeys = this.validatePrivateKeys(props, _userIDs.length);
        } catch (NullPointerException e) {
            // If any property failed to validate, init() fails.
            return false;
        }

        if(_debugMode)
            Console.enterDebugMode();
        _haveInit = true;
        return true;
    }

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
                Console.warn("IOException while closing "
                            + "'" + _configFileName + "': " + e);
            }
        }
        return props;
    }

    private InetAddress validateBindAddress(final Properties props) throws NullPointerException {
        String serverAddrProp = props.getProperty("bind_addr");

        // default value if ommitted.
        if (serverAddrProp == null) {
            serverAddrProp = Config.DEFAULT_BIND_ADDR;
        // translate '*' to an equiavlent that InetAddress.getByName()
        // understands.
        } else if (serverAddrProp.equals("*")) {
            serverAddrProp = "0.0.0.0";
        }

        // use InetAddress.getByName to validate the address.
        //
        // Note: this doesn't mean we can succesfully bind to the
        // address, just that it is a valid address. If the address
        // doesn't belong to this host, we'll catch that when we
        // create the WelcomePort.
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

    private int validateBindPort(final Properties props) throws NullPointerException {
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

    private boolean validateDebug(final Properties props) throws NullPointerException {
        String debugProp = props.getProperty("debug");

        // Default value if ommitted.
        if (debugProp == null) {
            return DEFAULT_DEBUG_MODE;
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
