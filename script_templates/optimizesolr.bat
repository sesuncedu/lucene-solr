@echo off
:: optimizesolr.bat
:: Run an optimize process on the solr index
:: $Id: optimizesolr.bat

::Get the current batch file's short path
for %%x in (%0) do set scriptdir=%%~dpsx
for %%x in (%scriptdir%) do set scriptdir=%%~dpsx
::echo BatchPath = %scriptdir%

java @MEM_ARGS@ -Dmarc.source="NONE" -Dsolr.optimize_at_end="true" -jar %scriptdir%@CUSTOM_JAR_NAME@ %1 

