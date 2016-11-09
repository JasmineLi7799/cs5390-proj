package edu.utdallas.cs5390.group3.server;

import java.util.Properties;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public final class Config {
    private static int DEFAULT_BIND_PORT = 9876;

    private static Config _instance;
    private boolean _haveInit;

    private InetSocketAddress _serverSockAddr;

    private Config() {
        _haveInit = false;
    }

    public static Config instance() {
        if (_instance == null) {
            _instance = new Config();
        }
        return _instance;
    }

    public InetSocketAddress serverSockAddr() {
        if (!_haveInit) {
            Console.error("Attempted to access Config.serverSockAddr() "
                + "before Config.init()");
        }
        return _serverSockAddr;
    }

    public boolean init(String configFileName) {
        if (_haveInit) {
            Console.debug("Warning: duplicate initialization of "
                + "Config.");
            Thread.dumpStack();
            return false;
        }

        Properties props = this.loadConfig(configFileName);
        if (props == null) {
            Console.fatal("Could not load configuration file.");
            return false;
        }
        _serverSockAddr = loadServerConfig(props, configFileName);
        if (_serverSockAddr == null) {
            return false;
        }
        _haveInit = true;
        return true;
    }

    private static Properties loadConfig(String configFileName) {
        InputStream configFile = null;
        Properties props = new Properties();
        try {
            configFile = new FileInputStream(configFileName);
            props.load(configFile);
        } catch (IOException e) {
            Console.fatal("IOException while parsing "
                          + "'" + configFileName + "': " + e);
            return null;
        }
        if (configFile != null) {
            try {
                configFile.close();
            } catch (IOException e) {
                Console.warn("IOException while closing "
                            + "'" + configFileName + "': " + e);
            }
        }
        return props;
    }

    private static InetSocketAddress loadServerConfig(
        Properties props, String configFileName) {

        // validate 'bind_addr' format
        String serverAddrString = props.getProperty("bind_addr");
        InetAddress serverAddr;
        if (serverAddrString == null) {
            Console.fatal("Missing required 'bind_addr' property in "
                          + "'" + configFileName + "'.");
            return null;
        }
        if (serverAddrString.equals("*")) {
            serverAddrString = "0.0.0.0";
        }
        try {
            serverAddr = InetAddress.getByName(serverAddrString);
        } catch (UnknownHostException e) {
            Console.fatal("Could not resolve 'bind_addr' property in "
                          + "'" + configFileName + "' to a valid host: "
                          + serverAddrString);
            return null;
        }

        // validate 'bind_port' format
        String serverPortString = props.getProperty("bind_port");
        int serverPort;
        if (serverPortString == null) {
            serverPort = Config.DEFAULT_BIND_PORT;
        } else if (serverPortString.matches("^[0-9]+$")) {
            serverPort = Integer.parseInt(serverPortString);
            if (serverPort < 0 || serverPort > 65535) {
                Console.fatal("Specified 'bind_port' property in "
                              + "'" + configFileName + "' is out-of-range: '"
                              + serverPort + "' (must be 0-65535)");
                return null;
            }
        } else {
            Console.fatal("Malformed 'bind_port' property in "
                          + "'" + configFileName + "': '"
                          + serverPortString + "' (must be 0-65535)");
            return null;
        }

        return new InetSocketAddress(serverAddr, serverPort);
    }

}
