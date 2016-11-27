# CS5390 Team 3 - Server-based Chat

A simple IRC-like client/server chat system.

## Quick Start ##

1. `cd cs5390-proj`
2. Run `ant`
3. Run `./server.sh`
4. Run `./client.sh`
5. Run `./client.sh client2.cfg`
6. Run `./client.sh client3.cfg`
7. Run `./client.sh client4.cfg`
8. Run `./client.sh client5.cfg`

This will start the server and 5 clients, each with their own numeric client id
(1-5).

If you are using cmd.exe on Windows, simply replace `server.sh` and `client.sh`
with `server.bat` and `client.bat`.

## Detailed Instructions

### Project Directory Structure

The project is subdivided into three sub-projects: the client, the server, and a
shared library of common code called "core".

Files marked with `*` are emitted by the build system.

    cs5390-proj/
        common/           Project-wide ant and ivy configuration
        projects/
            client/
              * build/    Client build artifacts
              * lib/      Client runtime libraries (core.jar)
                src/      Client source code
            server/
              * build/    Server build artifacts
              * lib/      Server runtime libraries (core.jar)
                src/      Client source code
            core/
              * build/    Core library build artifacts
              * lib/      (unused)
                src/      Core library source files
        repository/       Local ant repository

        scripts/          Source directory for client and server shell scripts
      * client.bat        Client start script for cmd.exe
      * client.sh         Client start script Linux/Mac/etc.
      * server.bat        Server start script for cmd.exe
      * server.sh         Server start script Linux/Mac/etc.

### Build instructions

You will need Apache `ant` to build the project. This is already installed on
the cs*N*.utdallas.edu servers and net*XY*.utdallas.edu.

To build the project, simply run `ant` or `ant publish` from the `cs5390-proj`
folder.

In addition to the default `ant` target (`publish`), you may also run:

  * `ant clean` - Removes the build artifacts (`build` sub-directories)
  * `ant clean-harder` - Removes the build artifacts, the local repository, and
    the Ivy cache.

It is not ordinarily necessary to run these; they are provided as means to
restart the build process from scratch if something has gone wrong with the
build process.

### Running the Client and Server

If you do not wish to use the provided start scripts, it is best to run the .jar
files directly from the `build` sub-directory, as they will attempt to load the
core library from `../lib/core.jar`, relative to the .jar file's directory.

In other words, from the `cs5390-proj` directory run:

`java -jar projects/client/build/client.jar`
`java -jar projects/server/build/server.jar`

### Configuring the Client/Server

By default, `client.jar` will look for a file named `client.cfg` in the
directory that `client.jar` resides in (not necessarily your working
directory). Similarly, `server.jar` looks for `server.cfg` in its own directory.

You may specify that the client or server should use a different configuration
file by passing a file name as the first command line argument to the .jar. Ex:

`java -jar projects/client/build/client.jar /home/john-doe/my-client.cfg`

The specified configuration file may be an absolute path, or relative to your
working directory.

As eluded to in the Quick Start section, we have provided a default a server
configuration file and five client configuration files, emitted to their
respective `build` sub-directories:

    server.cfg
    client.cfg
    client2.cfg
    client3.cfg
    client4.cfg
    client5.cfg

You can edit these files, but be aware that they will be overwritten from source
the next time you run `ant`. The source for the default configuration files is:

    projects/server/server.cfg
    projects/client/client.cfg
    projects/client/client2.cfg
    ...
    etc.

You may edit these configuration files instead, but it will have no effect until
the next time you run `ant`, or manually copy them into the relevant `build`
subdirectory.
