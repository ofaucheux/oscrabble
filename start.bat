@echo off

for %%y in (scrabble-dictionary scrabble-server scrabble-client-swing) do (
    for /f "delims=" %%x in ('dir /od /b %%y\build\libs\*.jar') do (
      start "%%y" java -jar %%y\build\libs\%%x
    )
)