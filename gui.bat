@echo off
cls
setlocal ENABLEEXTENSIONS
set KEY_NAME="HKLM\SOFTWARE\JavaSoft\Java Runtime Environment"
set VALUE_NAME=CurrentVersion

::
:: get the current version of java
::
FOR /F "usebackq skip=4 tokens=3" %%A IN (`REG QUERY %KEY_NAME% /v
%VALUE_NAME% 2^>nul`) DO (
    set ValueValue=%%A
)

if defined ValueValue (
    
    @echo the current Java runtime is  %ValueValue%
) else (
    @echo %KEY_NAME%\%VALUE_NAME% not found.
    goto end
)

set JAVA_CURRENT="HKLM\SOFTWARE\JavaSoft\Java Runtime
Environment\%ValueValue%"
set JAVA_HOME=JavaHome

::
:: get the javahome
::
FOR /F "usebackq skip=4 tokens=3,4" %%A IN (`REG QUERY %JAVA_CURRENT% /v
%JAVA_HOME% 2^>nul`) DO (
    set JAVA_PATH=%%A %%B
)

"%JAVA_PATH%\bin\java.exe" -Dsun.java2d.opengl=true sigvis-1.0.0-BETA-20120611.jar 

:end
