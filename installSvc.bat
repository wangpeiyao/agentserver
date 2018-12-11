@echo off
set agent_path=%cd%

@echo 创建AgentServices服务
@echo --------------------------------------
sc create AgentServices binpath= %agent_path%\AgentServices.exe type= own start= auto displayname= AgentServices
@echo 创建AgentServices服务成功
sc config AgentServices type= interact type= own
@echo --------------------------------------
@echo 正在启动AgentServices服务...
sc start AgentServices
@echo 启动AgentServices服务成功
@echo --------------------------------------
pause