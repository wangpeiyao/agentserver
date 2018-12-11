@echo off
TITLE Stop AgentServer
java -Xms50m -Xmx512m -jar bin/Agent.jar stop
echo ==============Stopped==============
echo on