set DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
set TARGET=C:\Programmierung\OScrabble\build\deployed
java %DEBUG_OPTS% -jar %TARGET%/OScrabble-1.0_BUILD.jar
