set OLD_DIR=%CD%
cd projects\server\build
java -jar server.jar %1
cd %OLD_DIR%
