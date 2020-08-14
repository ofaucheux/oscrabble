
start "Dictionary" java -jar scrabble-dictionary/build/libs/scrabble-dictionary-1.2.jar
start "Server" java -jar scrabble-server/build/libs/scrabble-server-1.2.jar
timeout /t 5 /nobreak > NUL
start "Client" java -jar scrabble-client-swing/build/libs/scrabble-client-swing-1.2.jar
