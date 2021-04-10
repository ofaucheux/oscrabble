@echo off
for /f "delims=" %%x in ('dir /od /b scrabble-dictionary\build\libs\*.jar') do set latestjar=%%x
start "Dictionary" java -jar scrabble-dictionary\build\libs\%latestjar%

rem start "gnufind scrabble-dictionary\build\libs -type f | grep -v fixtures | xargs java -jar"
rem start "gnufind scrabble-server\build\libs -type f | grep -v fixtures | xargs java -jar"
rem start "gnufind scrabble-client-ai\build\libs -type f | grep -v fixtures | xargs java -jar"
