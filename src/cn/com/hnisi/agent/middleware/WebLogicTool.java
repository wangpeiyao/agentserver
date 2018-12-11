package cn.com.hnisi.agent.middleware;

import java.io.File;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import cn.com.hnisi.agent.handler.AgentHandler;
import cn.com.hnisi.agent.middleware.cluster.WebLogicCluster;
import cn.com.hnisi.agent.middleware.cluster.WebLogicClusterForAixThread;
import cn.com.hnisi.agent.middleware.cluster.WebLogicClusterForLinuxThread;
import cn.com.hnisi.agent.middleware.cluster.WebLogicClusterForWindowsThread;
import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ClusterServicesStatus;
import cn.com.hnisi.model.ResultModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.type.Status;
import cn.com.hnisi.util.AixCheck;
import cn.com.hnisi.util.Tool;
import cn.com.hnisi.util.XmlUtil;

public class WebLogicTool {
	static Logger log = Logger.getLogger(WebLogicTool.class);
	public WebLogicTool() {
	}

	/**
	 * 获取weblogic应用的Config.xml文件
	 * 
	 * @param domainPath
	 * @return
	 */
	public static File getConfigXmlFile(String domainPath) {
		File configFile = new File(domainPath + "/config/config.xml");
		if (configFile.exists()) {
			return configFile;
		} else {
			return null;
		}
	}

	/**
	 * 判断路径是否为domain目录
	 * 
	 * @param path
	 * @return
	 */
	public static boolean isDomainPath(String domainPath) {
		File file = getConfigXmlFile(domainPath);
		if (file != null) {
			return file.exists();
		} else {
			return false;
		}
	}

	/**
	 * 获取weblogic应用的名称
	 * 
	 * @param domainPath
	 * @return
	 */
	public static String getDomainName(String domainPath) {
		return XmlUtil.getNodeText(getConfigXmlFile(domainPath), "domain/name");
	}

	/**
	 * 获取weblogic应用“管理应用名称”
	 * 
	 * @param domainPath
	 * @return
	 */
	public static String getAdminServerName(String domainPath) {
		return XmlUtil.getNodeText(getConfigXmlFile(domainPath),
				"domain/admin-server-name");
	}

	/**
	 * 获取weblogic应用的监听端口，默认是7001
	 * 
	 * @param domainPath
	 * @return
	 */
	public static String getListenPort(String domainPath) {
		String port=XmlUtil.getNodeText(getConfigXmlFile(domainPath),
				"domain/server/listen-port");
		if(port==null){
			port="7001";
		}
		return port;
	}

	/**
	 * 获取weblogic应用的SSL监听端口
	 * 
	 * @param domainPath
	 * @return
	 */
	public static String getSSLListenPort(String domainPath) {
		String port=XmlUtil.getNodeText(getConfigXmlFile(domainPath),
				"domain/server/ssl/listen-port");
		if(port==null){
			port="";
		}
		return port;
	}

	/**
	 * 获取domain管理日志文件
	 * 
	 * @param domainPath
	 * @return
	 */
	public static String getAdminServerLogFilePath(String domainPath) {
		String adminServerName = getAdminServerName(domainPath);
		File logFile = new File(domainPath
				+ "/servers/"
				+ adminServerName
				+ "/"
				+ XmlUtil.getNodeText(getConfigXmlFile(domainPath),
						"domain/server[name='" + adminServerName
								+ "']/log/file-name"));
		if (logFile.exists()) {
			return logFile.getPath();
		} else {
			logFile = new File(domainPath + "/servers/" + adminServerName + "/logs/"
					+ adminServerName + ".log");
			if (logFile.exists()) {
				return logFile.getPath();
			}
		}
		return null;
	}

	/**
	 * 判断服务名是否为集群服务
	 * 
	 * @param domainPath
	 * @param servicesName
	 * @return
	 */
	public static boolean isClusterServices(String domainPath,
			String servicesName) {
		String adminServerName = getAdminServerName(domainPath);
		if (servicesName.equals(adminServerName)) {
			// 如果服务名等与管理服务名，则返回false
			return false;
		} else {
			String cluster = XmlUtil.getNodeText(getConfigXmlFile(domainPath),
					"domain/server[name='" + servicesName + "']/cluster");
			// 判断是否为集群-服务
			if (cluster != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取集群服务监听端口
	 * 
	 * @param domainPath
	 *            domain路径
	 * @param servicesName
	 *            服务名
	 * @return
	 * @throws Exception
	 */
	public static String getClusterServicesPort(String domainPath,
			String servicesName) throws Exception {
		if (isClusterServices(domainPath, servicesName)) {
			String port = XmlUtil.getNodeText(getConfigXmlFile(domainPath),
					"domain/server[name='" + servicesName + "']/listen-port");
			return port;
		}
		return null;
	}
	
	/**
	 * 获取服务的监听地址，如果listen-address为空，则为本机地址
	 * @param domainPath
	 * @param servicesName
	 * @return
	 */
	public static String getClusterServicesListenAddress(String domainPath,
			String servicesName){
		String listenAddress="";
		if (isClusterServices(domainPath, servicesName)) {
			listenAddress = XmlUtil.getNodeText(getConfigXmlFile(domainPath),
					"domain/server[name='" + servicesName + "']/listen-address");
		}
		if(listenAddress==null || listenAddress.equals("")){
			return Tool.getIp();
		}else{
			return listenAddress;
		}
	}
	
	/**
	 * 同步部署文件到集群服务的应用存放路径
	 * @param sourceFile
	 * @param server
	 * @param handler
	 * @param socketChannel
	 * @return 同步成功返回True，失败返回False
	 */
	public static boolean syncDeployFile(AgentHandler handler,SocketChannel socketChannel,ServerModel server,File sourceFile){
		Process p = null;
		ProcessBuilder pb = null;
		ResultModel result=new ResultModel();
		String copyFile="";
		String newFile="";
		try{
			if(server.getServerType().equals("1")||server.getServerType().equals("2")){
				if(!sourceFile.exists()){
					result.setCodeMsg(Status.FAILED, sourceFile.getPath() +"需要复制的文件不存在!");
					return false;
				}
				List<ClusterServicesModel> clusterServersList = server
						.getClusterServers();
				//判断是否有配置集群服务;
				if (clusterServersList != null && clusterServersList.size() > 0) {
					for (ClusterServicesModel csm : clusterServersList) {
						//判断是否需要同步部署文件
						if(csm.getIsUploadFile()==1){
							log.info("开始同步文件: "+sourceFile.getPath()+" -> "+csm.getStagePath());
							copyFile=sourceFile.getPath();//需要拷贝的文件
							newFile=csm.getStagePath()+File.separator+copyFile.replace(server.getAppPath(), "");//新文件位置
							File newFilePath=new File(newFile);
							if(Tool.isAix()){
								String[] commandList = { "/bin/sh", "-c",
										"cp -rf " + copyFile + " " + newFilePath.getParent()  };
								pb = new ProcessBuilder(commandList);
							}else if(Tool.isLinux()){
								String[] commandList = { "/bin/sh", "-c",
										"cp -r -v " + copyFile + " " + newFilePath.getParent()};
								pb = new ProcessBuilder(commandList);
							}else if(Tool.isWindows()){
								List<String> commandList = new ArrayList<String>();
								commandList.add("cmd");
								commandList.add("/c");
								commandList.add("xcopy");
								commandList.add("\"" + copyFile + "\"");//指定要复制的文件。
								// 指定新文件的位置和/或名称,需要在目录末端增加“\”，否则会提示：.... 是文件名还是目录名(F = 文件，D = 目录)?
								commandList.add("\"" + newFilePath.getParent()+File.separator + "\"");
								commandList.add("/A");//只复制有存档属性集的文件， 但不改变属性
								commandList.add("/E");//复制目录和子目录，包括空的
								commandList.add("/I");//如果目标不存在，又在复制一个以上的文件， 则假定目标一定是一个目录
								commandList.add("/C");//即使有错误，也继续复制
								commandList.add("/F");//复制时显示完整的源和目标文件名
								commandList.add("/Y");//禁止提示以确认改写一个现存目标文件
								/*
								 * /D 只复制那些在指定日期或指定日期之后更改过的源文件。如果不包括“MM-DD-YYYY”值，
								 * “xcopy”会复制比现有“Destination”文件新的所有“Source”文件。该命令行选项
								 * 使您可以更新更改过的文件。
								 */
								commandList.add("/D");
								pb = new ProcessBuilder(commandList);
							}else{
								return false;
							}
							if(pb!=null){								
								pb.redirectErrorStream(true);
								p = pb.start();
								p.getOutputStream().close();
								int code=p.waitFor();
								if(code==0){
									result.setCodeMsg(Status.SUCCEED, "###集群同步文件成功<√>: "+ newFilePath.getPath());
								}else{
									result.setCodeMsg(Status.FAILED,  "###集群同步文件失败<×>: "+ newFilePath.getPath());								
								}
								handler.sendMsgRun(result, socketChannel);
							}
						}//end-if 判断是否设置了“同步文件”标志
					}// for循环，把文件同步到所有集群服务下面
				}//end-if 判断是否为集群服务
			}//end-if 判断是否为集群应用
			return true;
		}catch(Exception ex){
			result.setCodeMsg(Status.FAILED, "复制文件:"+copyFile+" 到 "+ newFile +" 失败，原因: "+ex.getMessage());
			handler.sendMsgRun(result, socketChannel);
			return false;
		}		
	}

	/**
	 * 启动WebLogic集群服务(需要先启动AdminServer(管理服务))
	 * @param responseHandler
	 * @param server
	 * @param socketChannel
	 */
	public static void startWebLogicClusterServices(AgentHandler responseHandler,ServerModel server,SocketChannel socketChannel) {
		ResultModel result=new ResultModel();
		try {
			// 判断应用类型是否为集群服务
			if (server.getServerType().equals("1") || server.getServerType().equals("2")) {
				//集群服务
				List<ClusterServicesModel> clusterServersList = server.getClusterServers();
				//检查集群服务
				if(clusterServersList==null){
					return;
				}			
				// 集群服务数
				int servicesCount=clusterServersList.size();
				if (servicesCount== 0) {
					return;
				}
				// 集群服务启动线程队列
				BlockingQueue<WebLogicCluster> clusterThreadQueue = new ArrayBlockingQueue<WebLogicCluster>(servicesCount);
				// 集群服务启动状态完成列表
				List<ClusterServicesStatus> clusterServicesStatusList = new ArrayList<ClusterServicesStatus>(servicesCount);
				
				//WebLogic集群启动工作线程
				WebLogicCluster clusterManageThread=null;
	
				result.setCodeMsg(Status.SUCCEED,"正在启动集群服务，总共< "+servicesCount+" >个");
				responseHandler.sendMsgRun(result, socketChannel);
				
				//启动集群服务;
				for (ClusterServicesModel csm : clusterServersList) {
					//根据不同的操作系统调用对应的处理类
					if(Tool.isAix()){
						//AIX
						clusterManageThread = new WebLogicClusterForAixThread(server, csm);
					}else if(Tool.isLinux()){
						//LINUX
						clusterManageThread = new WebLogicClusterForLinuxThread(server, csm);
					}else if(Tool.isWindows()){
						//WINDOWS
						clusterManageThread = new WebLogicClusterForWindowsThread(server, csm);
					}else{
						result.setCodeMsg(Status.FAILED,"不支持的操作系统类型!!!");
						responseHandler.sendMsgRun(result, socketChannel);
						return;
					}
					// 设置线程名称，与集群服务名称一样
					clusterManageThread.setName(csm.getName());
					clusterManageThread.start();
					//把启动服务添加到队列
					clusterThreadQueue.put(clusterManageThread);
				}
				
						
				//While循环监控集群服务启动的状态
				while (clusterThreadQueue.size() > 0) {
					//移除并返问队列头部的元素 如果队列为空，则返回null.
					WebLogicCluster cmt = clusterThreadQueue.poll();
					if (cmt != null) {
						Thread.sleep(1000);
						ClusterServicesStatus csmStatus = cmt.getStatus();
						if(csmStatus.isFinish()){
							if(csmStatus.getCode()!=0){
								result.setCodeMsg(Status.SUCCEED,"集群服务 ["+csmStatus.getName()+"]启动失败, 原因: "+csmStatus.getMsg());
								responseHandler.sendMsgRun(result,socketChannel);
							}
							clusterServicesStatusList.add(csmStatus);	
							cmt.interrupt();
						}else{
							if(csmStatus.getMsg()!=null){
								result.setCodeMsg(Status.SUCCEED,"集群服务 ["+csmStatus.getName()+"] "+csmStatus.getMsg());
								responseHandler.sendMsgRun(result,socketChannel);
							}
							//未完成，重新放回到队列，继续监控状态
							clusterThreadQueue.put(cmt);
						}								
					}
				}//End-While 循环监控服务启动状态
						
					
				//--------开始检查集群服务的启动情况--------
				int succeedCount=0;//成功数
				String succeedStr="";
				
				int failedCount=0;//失败数
				String failedStr="";
				
				for(ClusterServicesStatus css:clusterServicesStatusList){
					if(css.getCode()==0){
						succeedCount++;
						succeedStr+=" <"+css.getName()+"(启动成功)> ";
					}else{
						failedCount++;
						failedStr+=" <"+css.getName()+"(启动失败，原因:"+css.getMsg()+")> ";
					}
				}
				result.setCodeMsg(Status.SUCCEED,"集群服务数量: "+servicesCount+" 个");
				responseHandler.sendMsgRun(result, socketChannel);
				
				if(succeedCount>0){
					result.setCodeMsg(Status.SUCCEED,"成功启动 "+succeedCount+" 个 (" + succeedStr+")");
				}else{
					result.setCodeMsg(Status.SUCCEED,"成功启动 0 个 ");
				}
				responseHandler.sendMsgRun(result, socketChannel);
				
				if(failedCount>0){
					result.setCodeMsg(Status.SUCCEED,"启动失败 "+failedCount+" 个 (" + failedStr+")");
				}else{
					result.setCodeMsg(Status.SUCCEED,"启动失败 0 个 ");
				}
				responseHandler.sendMsgRun(result, socketChannel);
				//--------检查集群服务的启动情况--结束------
				
			}//End-If  判断应用类型是否为集群服务
		} catch (Exception ex) {			
			ex.printStackTrace();
			result.setCodeMsg(Status.FAILED,"启动集群服务时，发生异常: "+ex.getMessage());
			responseHandler.sendMsgRun(result, socketChannel);
		}
	}
	
	/**
	 * 停止WebLogic集群服务
	 * 
	 * @param clusterServersList
	 * @param socketChannel
	 */
	public static void stopWebLogicClusterServices(AgentHandler responseHandler,String domainPath,
			ClusterServicesModel csm,SocketChannel socketChannel){
		if(Tool.isWindows()){
			stopWebLogicClusterServicesForWindows(responseHandler,domainPath, csm, socketChannel);
		}else if(Tool.isLinux()){
			stopWebLogicClusterServicesForLinux(responseHandler, domainPath, csm, socketChannel);
		}else if(Tool.isAix()){
			stopWebLogicClusterServicesForAix(responseHandler, domainPath, csm, socketChannel);
		}
	}
	
	/**
	 * 停止WebLogic集群服务 for Windows
	 * 
	 * @param clusterServersList
	 * @param socketChannel
	 */
	private static void stopWebLogicClusterServicesForWindows(AgentHandler responseHandler,String domainPath,
			ClusterServicesModel csm,SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		try {
			log.info("停止服务" + csm.getName());
			// 获取服务运行程序窗口的PID,cmdID是CMD窗口的进程ID，程序未启动完成是不能通过端口关闭的。
			String cmdWindowId = Tool.findProcessIdByCommandLineWindows(csm
					.getName() + " " + csm.getAdminUrl());
			// 如果服务已启动情况下，可以根据端口获取程序进程，否则获取不了进程
			String portId = Tool.findPidByPortWindows(WebLogicTool
					.getClusterServicesPort(domainPath,
							csm.getName()));
			// 终止程序
			if (cmdWindowId != null || portId != null) {
				// 根据应用程序的启动文件，把所有打开的cmd.exe应用窗口关闭
				if (cmdWindowId != null) {
					Tool.killProcessByPidWindows(cmdWindowId);
				}
				if (portId != null) {
					Tool.killProcessByPidWindows(portId);
				}
				result.setCodeMsg(Status.SUCCEED,"已将集群服务 <" + csm.getName() + "> 停止.");
			} else {
				// 没有找到应用程序的窗口proccessId为null
				result.setCodeMsg(Status.SUCCEED,"集群服务端口 <" + csm.getName() + "> 已停止（或未启动）.");
			}
			responseHandler.sendMsgRun(result, socketChannel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * 停止WebLogic集群服务 for Linux
	 * 
	 * @param clusterServersList
	 * @param socketChannel
	 */
	private static void stopWebLogicClusterServicesForLinux(AgentHandler responseHandler,String domainPath,
			ClusterServicesModel csm, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		try {
			log.info("停止服务" + csm.getName());
			//获取集群服务端口
			String server_port = WebLogicTool.getClusterServicesPort(domainPath,
					csm.getName());
			// 先根据端口号获取进程ID，（并非所有进程都能被检测到，所有非本用户的进程信息将不会显示，如果想看到所有信息，则必须切换到 进程所属用户）
			String pid = Tool.findPidByPortLinux(server_port);
			if (pid == null) {//为NULl表示端口未被使用
				result.setCodeMsg(Status.SUCCEED,"集群服务 <" + csm.getName() 
						+ "> 端口 <"+ server_port + "> 已停止（或未启动）.");
			} else {
				String s = Tool.KillProcessByPidLinux(pid);
				if (s == null || s.equals("")) {
					result.setCodeMsg(Status.FAILED,"已将集群服务 <" + csm.getName() + "> 已停止.");
				} else {
					result.setCodeMsg(Status.FAILED,"停止应用失败，原因:" + s);
				}
			}
			responseHandler.sendMsgRun(result, socketChannel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	/**
	 * 停止WebLogic集群服务 for Aix
	 * 
	 * @param clusterServersList
	 * @param socketChannel
	 */
	private static void stopWebLogicClusterServicesForAix(AgentHandler responseHandler,String domainPath,
			ClusterServicesModel csm, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		try {
			log.info("停止服务" + csm.getName());
			//获取集群服务端口
			String server_port = WebLogicTool.getClusterServicesPort(domainPath,
					csm.getName());
			//先判断是否已启动
			if (!AixCheck.checkPortStatus(server_port)) {
				result.setCodeMsg(Status.SUCCEED,"集群服务 <"+csm.getName()+"> 端口:"+server_port+", 已停止或未启动.");
				responseHandler.sendMsgRun(result, socketChannel);
				return;
			}else{			
				// 判断能否使用RMSOCK命令
				if (AixCheck.canUseRmsock()) {
					log.info("Using rmsock command");
					// 先根据端口号获取进程ID
					String pid = Tool.findPidByPortAix(server_port);
					if (pid != null) {
						Tool.KillProcessByPidAix(pid);
						result.setCodeMsg(Status.FAILED,"已将集群服务 <" + csm.getName() + "> 已停止.");
					}else {
						result.setCodeMsg(Status.FAILED,"停止应用失败，未查找到进程PID.");
					}
				}else{
					//非管理员身份时，使用grep查找启动命令行 （模糊查询） 例： 启动命令 ../startManagedWebLogic.sh {SERVER_NAME} {ADMIN_URL}
					List<String> ppids = Tool.findPidByCommandAix("startManagedWebLogic.sh "+csm.getName());
					if (ppids != null && ppids.size() > 0) {					
						for (String ppid : ppids) {
							// 根据PPID获取归属于它的所有子进程
							List<String> pids = Tool.findPidByPPIDAix(ppid);
							for (String pid : pids) {
								if (pid != null) {
									// 终止子进程
									Tool.KillProcessByPidAix(pid);
								}
							}
						}
						result.setCodeMsg(Status.SUCCEED,"集群服务 <" + csm.getName() 
								+ "> 端口 <"+ server_port + "> 已停止（或未启动）.");
					}else{
						result.setCodeMsg(Status.FAILED,"停止应用失败，未查找到进程PID.");
					}
				}
				responseHandler.sendMsgRun(result, socketChannel);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
