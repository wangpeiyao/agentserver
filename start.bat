@echo off
TITLE 发布工具-Agent服务(请不要关闭)
java -Xms256m -Xmx1024m -cp bin/Agent.jar cn.com.hnisi.agent.handler.ManageAgent start
echo on