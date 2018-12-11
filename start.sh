echo ###################################################
echo ##### Author:wenzhifeng
echo ##### Date:2017/09/15
echo ###################################################
echo #####Copy this file to linux and run command: dos2unix converting file "startAgent.sh" to Unix format
nohupFile="nohup.out"
if [ -f "$nohupFile" ]
then  
echo > $nohupFile
fi
nohup java -Dfile.encoding=UTF-8 -Xms256m -Xmx1024m -cp bin/Agent.jar cn.com.hnisi.agent.handler.ManageAgent start 2>&1 &
tail -f nohup.out
