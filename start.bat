
start java -jar scrabble-dictionary/build/libs/scrabble-dictionary-1.1.jar
start java -jar scrabble-server/build/libs/scrabble-server-1.1.jar
timeout /t 10 /nobreak > NUL
start java -jar scrabble-client-swing/build/libs/scrabble-client-swing-1.1.jar
