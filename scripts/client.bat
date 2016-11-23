set OLD_DIR=%CD%
cd projects\client\build
java -jar client.jar %1
cd %OLD_DIR%
