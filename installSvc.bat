@echo off
set agent_path=%cd%

@echo ����AgentServices����
@echo --------------------------------------
sc create AgentServices binpath= %agent_path%\AgentServices.exe type= own start= auto displayname= AgentServices
@echo ����AgentServices����ɹ�
sc config AgentServices type= interact type= own
@echo --------------------------------------
@echo ��������AgentServices����...
sc start AgentServices
@echo ����AgentServices����ɹ�
@echo --------------------------------------
pause