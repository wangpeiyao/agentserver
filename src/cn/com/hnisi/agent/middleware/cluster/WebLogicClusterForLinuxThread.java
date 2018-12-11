package cn.com.hnisi.agent.middleware.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import cn.com.hnisi.agent.middleware.WebLogicTool;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.util.NetWorkUtil;
import cn.com.hnisi.util.Tool;

/**
 * Linux集群服务启动线程类
 * 
 * @author WenZhiFeng 2018年1月30日
 */
public class WebLogicClusterForLinuxThread extends WebLogicCluster {

	public WebLogicClusterForLinuxThread(ServerModel server,
			ClusterServicesModel csm) {
		super(server, csm);
	}

	public void run() {
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		long start, end = 0;
		int timer = AgentConfig.getTimeOut();
		try {
			// 获取服务名
			String servicesName = csm.getName();
			csmStatus.setName(servicesName);
			if (!WebLogicTool.isClusterServices(server.getDomainPath(), servicesName)) {
				csmStatus.setCode(-1);
				csmStatus.setMsg("启动失败，无效的集群服务");
				csmStatus.setFinish(true);
				return;
			}
			// 管理服务的启动文件: .../bin/startmanagedWebLogic.sh
			// <servericsName> <adminUrl>
			File startManagedWebLogicFile = new File(server.getDomainPath()
					+ "/bin/startManagedWebLogic.sh");
			if (!startManagedWebLogicFile.exists()) {
				csmStatus.setCode(-1);
				csmStatus.setMsg("启动失败，未找到启动文件: " + startManagedWebLogicFile.getPath());
				csmStatus.setFinish(true);
				return;
			}
	
			//根据端口，判断是否已启动（未关闭）
			String server_port=WebLogicTool.getClusterServicesPort(server.getDomainPath(), servicesName);//服务端口号
			String processId=Tool.findPidByPortLinux(server_port);
			if(processId!=null){
				csmStatus.setCode(-1);
				csmStatus.setMsg("检查到集群服务 <"+servicesName+"> 的端口: "+server_port+" 已被占用!");
				csmStatus.setFinish(true);
				return;
			}
		
			start = System.currentTimeMillis();// 记录开始时间
			boolean checkAdminStatus = false;//检查管理服务状态
			while (!checkAdminStatus) {
				// 等待控制端启动后，再启动服务(每隔1秒检查一次)
				if (NetWorkUtil.TestConnectByUrl(csm.getAdminUrl()
						+ "/console", 1000)) {
					// 已启动
					checkAdminStatus = true;
					break;
				} else {
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
				}
			}
			//启动命令 ../startManagedWebLogic.sh SERVER_NAME {ADMIN_URL}
			String commandLine = startManagedWebLogicFile + " " + servicesName + " " + csm.getAdminUrl();
			//日志文件名  例：$WLS_HOME/domain/domain_name/nohup.services7001
			File nohupLogFile =new File(server.getDomainPath() + "/nohup." + servicesName);
			// 如果管理服务已经启动，则启动集群服务
			String[] startCommandLine = { "/bin/sh", "-c",
					"nohup " + commandLine + " > " + nohupLogFile.getPath() + " &" };
			pb = new ProcessBuilder(startCommandLine);
			pb.redirectErrorStream(true);
			p=pb.start();
			//等待服务启动命令
			if (p.waitFor() == 0) {
				String[] tailCommandList = { "/bin/sh", "-c",
						"tail -f " + nohupLogFile.getPath() };
				pb = new ProcessBuilder(tailCommandList);
				pb.redirectErrorStream(true);
				p = pb.start();
			}
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));
			String console = "";
			while ((console = in.readLine()) != null) {
				if (console.toLowerCase().contains("server started in running")) {
					csmStatus.setCode(0);
					csmStatus.setMsg("应用已启动成功.");
					csmStatus.setFinish(true);
					return;//集群启动完成，返回到主业务
				} else if (console.toLowerCase().contains(
						"unable to get file lock")) {
					csmStatus.setCode(-1);
					csmStatus.setMsg("文件被锁，应用可能已在运行状态中.");
					csmStatus.setFinish(true);
					return;
				} else if (console.toLowerCase()
						.contains("force_shutting_down")) {
					csmStatus.setCode(-1);
					csmStatus.setMsg("WebLogic加载项目异常.");
					csmStatus.setFinish(true);
					return;
				} else if (console.toLowerCase().contains(
						"could not create pool connection")) {
					csmStatus.setCode(-1);
					csmStatus.setMsg("创建数据库连接池失败.");
					csmStatus.setFinish(true);
					return;
				} else {
						// 打印日志
						/*
						 csmStatus.setCode(0);
						 csmStatus.setMsg(new String(console.getBytes("ISO-8859-1"), AgentConfig
						 .getEncoding()));
						 csmStatus.setFinish(false);
						 */
						//判断控制端是否有响应
						end = System.currentTimeMillis();
						csmStatus.setCode(0);
						csmStatus.setMsg("正在加载部署项目(" + (end - start) / 1000
								+ "/" + (AgentConfig.getTimeOut() / 1000)
								+ "秒)");
						csmStatus.setFinish(false);
						if (timer - (end - start) <= 0) {
							csmStatus.setCode(-1);
							csmStatus.setMsg("加载时间超时，请稍候检查程序是否启动成功.");
							csmStatus.setFinish(true);
							return;
						}else{
							continue;
						}
				}
			}// 读取日志出办理 while循环
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			csmStatus.setFinish(true);
		}
	}
}
