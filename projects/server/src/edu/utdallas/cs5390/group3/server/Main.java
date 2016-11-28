package edu.utdallas.cs5390.group3.server;

import edu.utdallas.cs5390.group3.core.Console;

import java.lang.Runnable;
import java.lang.Runtime;
import java.lang.Thread;

import java.lang.IllegalStateException;
import java.util.NoSuchElementException;

import java.net.URLDecoder;

/* The Main class implements the server's interactive console and
 * creates the Server object.
 */
public final class Main {
    public static void main(String[] args) throws Exception {

        // This bit of ugliness obtains the directory that the .jar file
        // is located info.
        String path = Main.class.getProtectionDomain().getCodeSource()
            .getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        String basePath = "";
        int endIndex = decodedPath.lastIndexOf("/");
        if (endIndex != -1)
        {
            basePath = decodedPath.substring(0, endIndex + 1);
        }

        // By default, look for server.cfg in the .jar file's directory
        String configFileName = basePath + "server.cfg";
        // But use the first command line argument to locate the config file
        // if it has been specified.
        if(args.length > 0)
            configFileName = args[0];
        Console.info("Using config file: " + configFileName);

        // Create the server
        Server server = Server.instance();
        // Configure it
        try {
            server.configure(configFileName);
        } catch (IllegalStateException | NullPointerException e) {
            Console.fatal("Could not configure server: "
                          + e.getMessage());
            return;
        }
        // Start it.
        try {
            server.start();
        } catch (IllegalStateException e) {
            Console.fatal("Could not start server: "
                          + e.getMessage());
            return;
        }

        // From here on, if anything causes the process to terminate, we
        // want to try to stop() the Server as part of the termination.
        Main.registerShutdownHook();

        // Console input loop
        Console.info("Type 'quit' or 'exit' to terminate (case-insensitive).");
        while (!Thread.interrupted()) {
            // If the WelcomeThread dies, the server is toast.
            if (!server.isAlive()) {
                Console.fatal("Welcome thread terminated unexpectedly.");
                break;
            }

            try {
                String command = Console.nextLine();
                if (command.matches("(?i:)^(quit|exit)$")) {
                    break;
                } else {
                    Console.error("Unknown command: '"
                                  + command + "'");
                }
            } catch (NoSuchElementException |
                     IllegalStateException e) {
                Console.fatal("Console caught input exception: " + e);
                break;
            }
        }
        // If we exited the console input loop due to an interrupt,
        // let the user know what happened.
        if (Thread.interrupted()) {
            Console.fatal("Console thread interrupted.");
        }

        // If we don't make this call, the other threads of the server
        // will happily continue running without the main thread
        // (interactive console).
        server.stop();
        Console.close();
    }

    /* Whenever and wherever the program exits, we should try to call
     * stop() on the server for a clean exit.
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Server.instance().stop();
                    }
                }
            )
        );
    }
}
