@echo off
@echo 删除AgentServices服务
@echo --------------------------------------
@echo 停止服务
sc stop AgentServices
@echo 服务已停止
@echo --------------------------------------
sc delete AgentServices
@echo 服务删除成功
@echo --------------------------------------
pause