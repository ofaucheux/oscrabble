set DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
java %DEBUG_OPTS% -cp lib/* -jar OScrabble.jar
