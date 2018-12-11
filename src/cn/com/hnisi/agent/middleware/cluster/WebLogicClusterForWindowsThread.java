package cn.com.hnisi.agent.middleware.cluster;

import java.io.File;
import java.io.RandomAccessFile;

import cn.com.hnisi.agent.middleware.WebLogicTool;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.util.NetWorkUtil;
import cn.com.hnisi.util.Tool;

/**
 * Windows集群服务启动线程类
 * 
 * @author WenZhiFeng 2018年1月30日
 */
public class WebLogicClusterForWindowsThread extends WebLogicCluster {

	public WebLogicClusterForWindowsThread(ServerModel server,
			ClusterServicesModel csm) {
		super(server, csm);
	}

	public void run() {
		ProcessBuilder pb = null;
		long start, end = 0;
		int timer = AgentConfig.getTimeOut();
		
		try {
			String processId=null;//集群服务进程ID 变量
			// 服务名
			String servicesName = csm.getName();
			csmStatus.setName(servicesName);
			if (!WebLogicTool.isClusterServices(server.getDomainPath(), servicesName)) {
				csmStatus.setCode(-1);
				csmStatus.setMsg("启动失败，无效的集群服务");
				csmStatus.setFinish(true);
				return;
			}
			// 管理服务的启动文件: .../bin/startmanagedWebLogic.cmd
			// <servericsName> <adminUrl>
			File startManagedWebLogicFile = new File(server.getDomainPath()
					+ "/bin/startManagedWebLogic.cmd");
			if (!startManagedWebLogicFile.exists()) {
				csmStatus.setCode(-1);
				csmStatus.setMsg("启动失败，未找到启动文件: "
						+ startManagedWebLogicFile.getPath());
				csmStatus.setFinish(true);
				return;
			}
	
			//根据端口，判断是否已启动（未关闭）
			String server_port=WebLogicTool.getClusterServicesPort(server.getDomainPath(), servicesName);//服务端口号
			processId=Tool.findPidByPortWindows(server_port);
			if(processId!=null){
				csmStatus.setCode(-1);
				csmStatus.setMsg("检查到集群服务 <"+servicesName+"> 的端口: "+server_port+" 已被占用!");
				csmStatus.setFinish(true);
				return;
			}
		
			start = System.currentTimeMillis();// 记录开始时间
			boolean checkAdmin = false;
			while (!checkAdmin) {
				// 等待控制端启动后，再启动服务
				if (!NetWorkUtil.TestConnectByUrl(csm.getAdminUrl()
						+ "/console", 1000)) {
					end = System.currentTimeMillis();
					csmStatus.setCode(0);
					csmStatus.setMsg("检测到集群管理控制端  " + csm.getAdminUrl()
							+ " 服务未有响应，等待中(" + (end - start) / 1000 + "/"
							+ (AgentConfig.getTimeOut() / 1000) + "秒)");
					
					if (timer - (end - start) <= 0) {
						csmStatus.setCode(-1);
						csmStatus.setMsg("加载时间超时，请先启动集群总控制端应用.");
						csmStatus.setFinish(true);
						return;
					} else {
						continue;
					}
				} else {
					// 已启动
					checkAdmin = true;
					break;
				}
			}
			String commandLine = startManagedWebLogicFile + " " + servicesName
					+ " " + csm.getAdminUrl();
			// 如果管理服务已经启动，则启动集群服务
			String[] startCommandLine = { "cmd.exe", "/c",
					"start " + " \"集群服务:" + servicesName + "\" " + commandLine };
			pb = new ProcessBuilder(startCommandLine);
			pb.redirectErrorStream(true);
			pb.start();
			File logFile = new File(server.getDomainPath() + "/servers/"
					+ servicesName + "/logs/" + servicesName + ".log");
			// 如果文件不存在
			if (!logFile.exists()) {
				csmStatus.setCode(-1);
				csmStatus.setMsg("不存在日志文件：" + logFile.getPath()
						+ "，请检查WebLogic应用配置！");
				return;
			}

			long filePointer = 0;
			RandomAccessFile tailfile = new RandomAccessFile(logFile, "r");
			// 用于判断读取日志是否已经结束
			while (!csmStatus.isFinish()) {
				long fileLength = logFile.length();
				if (fileLength < filePointer) {
					tailfile = new RandomAccessFile(logFile, "r");
					filePointer = 0;
				}
				if (filePointer == 0) {
					filePointer = fileLength;
				}
				// 如果有新的内容，则从最后一次位置开始读取新内容
				if (fileLength > filePointer) {
					tailfile.seek(filePointer);
					String console = "";
					while ((console = tailfile.readLine()) != null) {
						if (console.toLowerCase().contains(
								"server started in running")) {
							csmStatus.setCode(0);
							csmStatus.setMsg("应用已启动成功.");
							csmStatus.setFinish(true);
							return;
						} else if (console.toLowerCase().contains(
								"unable to get file lock")) {
							csmStatus.setCode(-1);
							csmStatus.setMsg("已在运行状态中.");
							csmStatus.setFinish(true);
							return;
						} else if (console.toLowerCase().contains(
								"force_shutting_down")) {
							csmStatus.setCode(-1);
							csmStatus.setMsg("WebLogic加载项目异常.");
							csmStatus.setFinish(true);
							return;
						} else if (console.toLowerCase().contains(
								"could not create pool connection")) {
							csmStatus.setCode(-1);
							csmStatus.setMsg("创建数据库连接池失败");
							csmStatus.setFinish(true);
							return;
						} else {
							// 打印日志
							// csmStatus.setCode(0);
							// csmStatus.setMsg(new String(console
							// .getBytes("ISO-8859-1"), AgentConfig
							// .getEncoding()));
							// csmStatus.setFinish(false);
							continue;
						}
					}// 读取日志出办理 while循环
					filePointer = tailfile.getFilePointer();
				} else {// 等待启动日志更新，超过指定时间后退出while循环
					end = System.currentTimeMillis();
					csmStatus.setCode(0);
					csmStatus.setMsg("正在加载部署项目(" + (end - start) / 1000 + "/"
							+ (AgentConfig.getTimeOut() / 1000) + "秒)");
					csmStatus.setFinish(false);
					if (timer - (end - start) <= 0) {
						csmStatus.setCode(-1);
						csmStatus.setMsg("加载时间超时，请稍候检查程序是否启动成功.");
						csmStatus.setFinish(true);
						return;
					}
				}
				Thread.sleep(1000);// 避免 findProcessIdByCommandLineWindows 多线程冲突
				// 判断窗口是否正常
				processId = Tool
						.findProcessIdByCommandLineWindows(servicesName
								+ csm.getAdminUrl());
				if (processId == null) {
					csmStatus.setCode(-1);
					csmStatus.setMsg("在启动过程中，进程被关闭");
					csmStatus.setFinish(true);
					break;
				}
			}// 读取日志while循环
			tailfile.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			csmStatus.setFinish(true);
		}
	}
}
