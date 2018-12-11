package cn.com.hnisi.agent.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.com.hnisi.agent.handler.AgentHandler;
import cn.com.hnisi.agent.interfaces.IPlatform;
import cn.com.hnisi.agent.middleware.WebLogicTool;
import cn.com.hnisi.agent.middleware.WebSphereTool;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.ClusterServicesModel;
import cn.com.hnisi.model.ResultModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.type.PlatformType;
import cn.com.hnisi.type.Status;
import cn.com.hnisi.util.Tool;

/**
 * Windows系统
 * 
 * @author FengGeGe
 * 
 */
public class Windows implements IPlatform {
	static Logger log = Logger.getLogger(Windows.class);
	AgentHandler responseHandler = null;
	long startTime, endTime;

	public Windows(AgentHandler responseHandler) {
		this.responseHandler = responseHandler;
	}

	/**
	 * 备份应用
	 * 
	 * @param commandList
	 * @param server
	 * @param socketChannel
	 * @return
	 */
	public void backUp(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		try {
			startTime = System.currentTimeMillis();
			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setCodeMsg(Status.FAILED,"备份目标   “" + appPath.getPath() + "” 不存在，备份失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			result.setCode(0);
			result.setMsg("正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			String winrar = getWinRAR();
			if (winrar == null) {
				result.setCodeMsg(Status.FAILED,"服务器未安装WinRAR工具，无法进行压缩备份，请先下载安装.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			List<String> commandList = new ArrayList<String>();
			commandList.add(winrar);// WinRAR的安装目录
			commandList.add("a");
			commandList.add("-r");
			commandList.add("-m0");// 最快速度，0压缩
			// /----过滤文件----
			commandList.add("-x*.svn");
			commandList.add("-x*.svn-base");
			// -----过滤结束----
			commandList.add("-ibck");// 后台运行
			// 备份文件存放的路径

			String backUpFile = "";
			// 发果指定了备份目录
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				backUpFile = server.getBackUpPath().replace("\\", "/") + "/"
						+ appPath.getName() + Tool.getNowDateSSS() + "_"
						+ server.getDomainPort() + ".rar";
			} else {
				backUpFile = server.getAppPath() + Tool.getNowDateSSS() + "_"
						+ server.getDomainPort() + ".rar";
			}
			commandList.add(backUpFile);// 备份后存放的路径
			commandList.add(server.getAppPath());// 需要备份的文件夹

			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();

			String WinRAR_pid = Tool.getProcessIdByProcess(p);
			p.getOutputStream().close();
			int timer = 0;
			while (WinRAR_pid != null) {
				// 判断进程是否已经结束
				if (Tool.processIsRunning(PlatformType.WINDOWS, WinRAR_pid) == false) {
					break;
				}
				timer += 1000;
				result.setCodeMsg(Status.SUCCEED,"正在备份文件中，请稍等(" + timer / 1000 + "/"
						+ (AgentConfig.getTimeOut() / 1000) + "秒)");
				if (!responseHandler.sendMsgRun(result, socketChannel)) {
					break;
				}
				if (timer >= AgentConfig.getTimeOut()) {
					result.setCodeMsg(Status.FAILED,"备份文件“" + backUpFile + "”时，超过等待时间("
							+ (AgentConfig.getTimeOut() / 1000)
							+ "秒)程序已断开连接，请稍候查看");
					responseHandler.sendMsgCompleted(result, socketChannel);
					break;
				}
				Thread.sleep(1000);
			}
			// 获取程序“退出”结果，0为已正常执行。
			if (p.waitFor() == 0) {
				endTime = System.currentTimeMillis();
				result.setCodeMsg(Status.SUCCEED,"备份成功，备份文件存放在服务器:“" + backUpFile + "” 备份耗时:"
						+ Tool.millisecondFormat(endTime - startTime));

			} else {
				result.setCodeMsg(Status.FAILED,"备份失败，WinRAR已取消或发生异常退出");
			}
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		} catch (Exception ex) {
			ex.printStackTrace();
			result.setCodeMsg(Status.FAILED,"备份发生异常，原因: " + ex.getMessage());
			log.info(result.getMsg());
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	public void backUpByCopy(ServerModel server, SocketChannel socketChannel) {
		// TODO 备份
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in;
		try {
			startTime = System.currentTimeMillis();
			File appPath = new File(server.getAppPath());
			if (!appPath.exists()) {
				result.setCodeMsg(Status.FAILED,"备份目标  “" + appPath.getPath() + "” 不存在，备份失败");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			result.setCodeMsg(Status.SUCCEED,"正在对备份目标“" + appPath.getPath() + "” 进行备份，请稍候...");
			responseHandler.sendMsgRun(result, socketChannel);

			List<String> commandList = new ArrayList<String>();
			commandList.add("cmd");
			commandList.add("/c");
			commandList.add("xcopy");
			commandList.add("\"" + appPath.getPath() + "\"");//备份目录目录或文件

			// 备份文件存放的路径
			String backUpFile = "";
			// 发果指定了备份目录
			if (server.getBackUpPath() != null
					&& server.getBackUpPath().trim().length() > 0) {
				File backPath = new File(server.getBackUpPath());
				// 如果目录不存在则创建
				if (!backPath.exists()) {
					backPath.mkdirs();
				}
				backUpFile = server.getBackUpPath().replace("\\", "/") + "/"
						+ appPath.getName() + Tool.getNowDateSSS() + "_"
						+ server.getDomainPort();
			} else {
				backUpFile = server.getAppPath() + Tool.getNowDateSSS() + "_"
						+ server.getDomainPort();
			}
			commandList.add("\"" + backUpFile + "\"");// 备份后存放的路径
			commandList.add("/A");
			commandList.add("/Y");
			commandList.add("/E");
			commandList.add("/I");
			commandList.add("/C");
			commandList.add("/F");
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					"GBK"));
			String console = "";
			while ((console = in.readLine()) != null) {
				result.setMsg(new String(console.getBytes(AgentConfig
						.getEncoding()), AgentConfig.getEncoding()));
				responseHandler.sendMsgRun(result, socketChannel);
			}
			endTime = System.currentTimeMillis();
			result.setCodeMsg(Status.SUCCEED,"备份成功，备份文件存放在服务器:" + backUpFile + " 备份耗时:"
					+ Tool.millisecondFormat(endTime - startTime));
		} catch (Exception e) {
			result.setCodeMsg(Status.FAILED,"备份失败，原因：" + e.getMessage());
			log.info(result.getMsg());
		}
		responseHandler.sendMsgCompleted(result, socketChannel);
	}

	/**
	 * 获取Windows中的WinRAR程序EXE执行路径 例：C:\Program Files (x86)\WinRAR\WinRAR.exe
	 * 
	 * @return
	 */
	private String getWinRAR() {
		String winRAR_exe = null;
		// 优先查找Path环境变量
		if (winRAR_exe == null) {
			String[] paths = System.getenv("PATH").split(";");
			for (String path : paths) {
				if (path.toLowerCase().contains("winrar")) {
					File file = new File(winRAR_exe + "/WinRAR.exe");
					if (file.exists()) {
						winRAR_exe = file.getPath();
					}
				}
			}
		}

		if (winRAR_exe != null) {
			// 其次查找配置文件
			winRAR_exe = AgentConfig.getNodeTextContent("/root/agent/winrar");
			File file = new File(winRAR_exe + "/WinRAR.exe");
			if (file.exists()) {
				winRAR_exe = file.getPath();
			}
		}
		// win7_32和windows server 查C盘目录
		if (winRAR_exe == null) {
			File file = new File("C:/Program Files/WinRAR/WinRAR.exe");
			if (file.exists()) {
				winRAR_exe = file.getPath();
			}
		}
		// win7_64:查C盘目录
		if (winRAR_exe == null) {
			File file = new File("C:/Program Files (x86)/WinRAR/WinRAR.exe");
			if (file.exists()) {
				winRAR_exe = file.getPath();
			}
		}

		return winRAR_exe;
	}

	/**
	 * 启动WebSphere
	 * 
	 * @param server
	 * @param socketChannel
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void startWebSphere(ServerModel server, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in;
		try {
//			if (Tool.processIsRunning(PlatformType.WINDOWS,
//					WebSphereTool.getServerPid(server))) {
//				result.setCodeMsg(Status.FAILED,"经检测,WebSphere服务器进程已在运行中,进程ID="
//						+ WebSphereTool.getServerPid(server) + ",请确认.");
//				responseHandler.sendMsgCompleted(result, socketChannel);
//				return;
//			}

			File appSrvPath = new File(server.getDomainPath());
			if (!appSrvPath.exists()) {
				result.setCodeMsg(Status.FAILED,"服务器中 “" + appSrvPath.getPath()
						+ "”不是有效的概要文件(Profile)路径，请检查配置");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			File startServerFile = new File(appSrvPath.getPath()
					+ "\\bin\\startServer.bat");
			if (!startServerFile.exists()) {
				result.setCodeMsg(Status.FAILED,"未找到启动文件 “" + startServerFile.getPath()
						+ "” ，请检查WebSphere配置.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			List<String> commands = new ArrayList<String>();
			commands.add("cmd.exe");
			commands.add("/C");
			commands.add("\"" + startServerFile.getPath() + "\"");
			commands.add("server1");
			System.out.println("WebSphere start command==> "+commands.toString());
			pb = new ProcessBuilder(commands);
			pb.redirectErrorStream(true);
		    p = pb.start();
			startTime = System.currentTimeMillis();
			p.getOutputStream().close();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					"GBK"));
			String console = "";

			result.setCodeMsg(Status.SUCCEED,"WebSphere服务器启动中...");
			responseHandler.sendMsgRun(result, socketChannel);

			while ((console = in.readLine()) != null) {
				result.setMsg(console);
				if (!responseHandler.sendMsgRun(result, socketChannel)) {
					// 如果信息发送失败，则停止
					return;
				}

				if (console.toLowerCase().contains("进程标识为")
						|| console.toLowerCase().contains("is started")
						|| console.toLowerCase().contains("process id is")) {
					result.setCodeMsg(Status.SUCCEED,"WebSphere应用已启动成功.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				} else if (console.toLowerCase().contains("already be running")
						|| console.toLowerCase().contains("已经有一个服务器的实例在运行.")) {
					result.setCodeMsg(Status.FAILED,"WebSphere应用正在运行中，请先停止应用.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}

				if (AgentConfig.getTimeOut() - (endTime - startTime) <= 0) {
					result.setCodeMsg(Status.FAILED,"WebSphere加载应用时间超系统指定时间，请检查应用是否正常.");
					responseHandler.sendMsgCompleted(result, socketChannel);
					break;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			result.setCodeMsg(Status.FAILED,"SYS_启动WebSphere出错，原因：" + ex.getMessage());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	}

	/**
	 * 停止WebSphere
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void stopWebSphere(ServerModel server, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		try {
			log.info("ServerPid=" + WebSphereTool.getServerPid(server));
			if (!WebSphereTool.isServerPort(server)) {
				result.setCodeMsg(Status.FAILED,"停止失败，端口号 “" + server.getDomainPort()
						+ "” 并不是WebSphere应用的端口!");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			result.setCodeMsg(Status.SUCCEED,"正在检测WebSphere服务端口运行状态");
			responseHandler.sendMsgRun(result, socketChannel);
			if (Tool.findPidByPortWindows(server.getDomainPort()) != null) {
				result.setCodeMsg(Status.SUCCEED,"WebSphere服务运行中，正在尝试停止服务...");
				responseHandler.sendMsgRun(result, socketChannel);
				String pid = Tool.findPidByPortWindows(server.getDomainPort());
				if (pid != null) {
					String msg = Tool.killProcessByPidWindows(pid);
					result.setCodeMsg(Status.SUCCEED,"WebSphere应用服务停止成功. "
							+ (msg != null ? "处理信息:" + msg : ""));
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				} else {
					result.setCodeMsg(Status.FAILED,"根据应用端口号 “" + server.getDomainPort()
							+ "” 未找到WebSphere应用服务进程");
					responseHandler.sendMsgCompleted(result, socketChannel);
					return;
				}
			} else {
				result.setCodeMsg(Status.FAILED,"WebSphere服务未启动");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
		} catch (Exception ex) {
			result.setCodeMsg(Status.FAILED,"停止WebSphere服务出错，原因：" + ex.getMessage());
			responseHandler.sendMsgCompleted(result, socketChannel);
		}
	}

	/**
	 * 启动WebLogic应用
	 * 
	 * @param commandList
	 * @param server
	 * @param socketChannel
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void startWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 启动应用
		ResultModel result = new ResultModel();
		long start, end;
		start = System.currentTimeMillis();
		int timer = AgentConfig.getTimeOut();// 计时 单位:秒

		String serverPID = null;
		String processId = null;
		// 判断应用配置
		if (!WebLogicTool.isDomainPath(server.getDomainPath())) {
			result.setCodeMsg(Status.FAILED,"服务器中 “" + server.getDomainPath()
					+ "”不是有效的Domain路径，请检查配置");
			responseHandler.sendMsgCompleted(result, socketChannel);
			return;
		}
		// domain名称
		String domainName = WebLogicTool.getDomainName(server.getDomainPath());

		// 判断应用程序CMD窗口是否已打开
		processId = Tool.findProcessIdByCommandLineWindows(server
				.getDomainPath());
		//非集群机器需要检查应用是否已在运行中
		if (processId != null && 
				server.getServerType().equals("0")&&server.getServerType().equals("1")) {
			// 判断端口是否已绑定，因为窗口运行后，程序需要加载项目成功后，才会绑定到应用端口。
			if (Tool.findPidByPortWindows(server.getDomainPort()) == null) {
				result.setCodeMsg(Status.FAILED,"已检测到应用程序正在加载部署项目中，请稍候重试!");
			} else {
				result.setCodeMsg(Status.FAILED,"应用程序 “" + domainName + "” 已在启动运行中，不能同时打开多个.");
			}
			responseHandler.sendMsgCompleted(result, socketChannel);
			// =====结束--Windows检查程序是否已经运行，避免重复打开应用程序窗口，如果没有运行，则往下执行启动命令。
		} else {

			// 开始检查配置和启动应用
			Process p = null;
			// 检查启动文件
			String startFile = null;
			File startWebLogicFile = new File(server.getDomainPath()
					+ "/startWebLogic.cmd");
			if (startWebLogicFile.exists()) {
				startFile = startWebLogicFile.getPath();
			} else {
				startWebLogicFile = new File(server.getDomainPath()
						+ "/bin/startWebLogic.cmd");
				if (startWebLogicFile.exists()) {
					startFile = startWebLogicFile.getPath();
				}
			}
			if (startFile == null) {
				result.setCodeMsg(Status.FAILED,"启动失败，未找到启动文件“startWebLogic.cmd”");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}
			try {
				// 只有应用类型为“普通应用=0” 或“ 总控制端应用=1”时才启动主应用。“集群机器=2”不启动主应用，只启动集群服务。
				if (server.getServerType().equals("0") || server.getServerType().equals("1")) {
					// 启动前判断是否还存在后台进程，如果有则终止后再重新启动。
					serverPID = Tool.findPidByPortWindows(server
							.getDomainPort());
					if (serverPID != null) {
						
						Tool.killProcessByPidWindows(serverPID);
					}
					List<String> commandList = new ArrayList<String>();
					commandList.add("cmd.exe");
					commandList.add("/C");
					commandList.add("start");
					commandList.add("\"应用:" + domainName + "\"");
					commandList.add(startFile);
					ProcessBuilder pb = new ProcessBuilder(commandList);
					pb.redirectErrorStream(true);
					p = pb.start();
					File logFile = new File(
							WebLogicTool.getAdminServerLogFilePath(server
									.getDomainPath()));
					// 如果文件不存在
					if (!logFile.exists()) {
						result.setCodeMsg(Status.FAILED,"不存在日志文件：" + logFile.getPath()
								+ "，请检查WebLogic应用配置！");
						responseHandler.sendMsgCompleted(result, socketChannel);
						return;
					}
					while ((serverPID = Tool.findPidByPortWindows(server
							.getDomainPort())) == null) {
						// 根据命令行获取窗口进程ID
						processId = Tool.findProcessIdByCommandLineWindows(Tool
								.formatPath(server.getDomainPath()));
						// 判断进程是否还存在
						if (processId == null) {
							result.setCodeMsg(Status.FAILED,"WebLogic加载应用过程中，程序被终止.");
							responseHandler.sendMsgCompleted(result,
									socketChannel);
							break;
						} else {
							long filePointer = 0;
							RandomAccessFile tailfile = new RandomAccessFile(
									logFile, "r");
							// 用于判断读取日志是否已经结束
							boolean finish = false;
							while (!finish) {
								long fileLength = logFile.length();
								if (fileLength < filePointer) {
									tailfile = new RandomAccessFile(logFile,
											"r");
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
											result.setCodeMsg(Status.SUCCEED,"WebLogic应用已启动成功.");
											responseHandler.sendMsgRun(result,
													socketChannel);
											finish = true;
											break;
										} else if (console
												.toLowerCase()
												.contains(
														"unable to get file lock")) {
											result.setCodeMsg(Status.FAILED,"WebLogic应用已在运行状态中.");
											responseHandler.sendMsgRun(result,
													socketChannel);
											finish = true;
											return;
										} else if (console
												.toLowerCase()
												.contains("force_shutting_down")) {
											result.setCodeMsg(Status.FAILED,"WebLogic加载时发生异常，应用启动失败。 Agent自动关闭WebLogic窗口程序，请重新手工启动.");
											responseHandler.sendMsgRun(result,
													socketChannel);
											stopWebLogic(server, socketChannel);
											finish = true;
											return;
										} else if (console
												.toLowerCase()
												.contains(
														"could not create pool connection")) {
											result.setCodeMsg(Status.FAILED,"WebLogic创建数据库连接池失败， Agent将关闭WebLogic窗口程序，请检查网络和配置后再重新启动！");
											responseHandler.sendMsgRun(result,
													socketChannel);
											stopWebLogic(server, socketChannel);
											finish = true;
											return;
										} else {
											result.setCodeMsg(Status.SUCCEED,new String(console
													.getBytes("ISO-8859-1"),
													AgentConfig.getEncoding()));
											responseHandler.sendMsgRun(result,
													socketChannel);
										}
									}// 读取日志出办理 while循环
									filePointer = tailfile.getFilePointer();
								} else {
									end = System.currentTimeMillis();
									result.setCodeMsg(Status.SUCCEED,"WebLogic正在加载部署项目("
											+ (end - start) / 1000 + "/"
											+ (AgentConfig.getTimeOut() / 1000)
											+ "秒)");
									// 如果发送信息失败返回false
									if (!responseHandler.sendMsgRun(result,
											socketChannel)) {
										finish = true;
										return;// 不再执行后续判断
									}
									if (timer - (end - start) <= 0) {
										result.setCodeMsg(Status.FAILED,"应用加载时间过长或加载项目过程出错，请稍候检查程序是否启动成功.");
										responseHandler.sendMsgCompleted(
												result, socketChannel);
										finish = true;
										return;
									}
								}
								processId = Tool
										.findProcessIdByCommandLineWindows(Tool
												.formatPath(server
														.getDomainPath()));
								if (processId == null) {
									result.setCodeMsg(Status.FAILED,"应用在启动过程中，进程被关闭.");
									responseHandler.sendMsgCompleted(result,
											socketChannel);
									finish = true;
									return;// 不再执行后续判断
								}
								Thread.sleep(1000);
							}// 读取日志while循环
							tailfile.close();
						}// else
					}// 判断是否已启动 while循环
				}// if判断，如果是“集群机器”，只启动集群服务
				//启动集群服务
				WebLogicTool.startWebLogicClusterServices(responseHandler,server, socketChannel);		
				result.setCodeMsg(Status.SUCCEED,"启动完毕");
				responseHandler.sendMsgCompleted(result, socketChannel);
				
			} catch (Exception e) {
				result.setCodeMsg(Status.FAILED,"Agent发生异常，原因: " + e.getMessage());
				log.info(result.getMsg());
			} finally {
				if (p != null) {
					p.destroy();
				}
				try {
					if (socketChannel != null) {
						socketChannel.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * 停止WebLogic应用
	 * 
	 * @param server
	 * @param socketChannel
	 * @return
	 * @throws InterruptedException
	 */
	public void stopWebLogic(ServerModel server, SocketChannel socketChannel) {
		// TODO 停止应用
		ResultModel result = new ResultModel();
		try {

			// 检查端口,避免误杀其他程序
			if (!WebLogicTool.getListenPort(server.getDomainPath()).equals(
					server.getDomainPort())
					&& !WebLogicTool.getSSLListenPort(server.getDomainPath())
							.equals(server.getDomainPort())) {
				result.setCodeMsg(Status.FAILED,"校验失败，端口号 “" + server.getDomainPort() + "” 不是“"
						+ server.getDomainPath() + "” 应用的端口，停止失败.");
				responseHandler.sendMsgCompleted(result, socketChannel);
				return;
			}

			// 先停止集群服务
			List<ClusterServicesModel> cmsList=server.getClusterServers();
			if(cmsList!=null){
				for(ClusterServicesModel csm:cmsList){
					WebLogicTool.stopWebLogicClusterServices(responseHandler,server.getDomainPath(),csm,socketChannel);
					Thread.sleep(1000);
				}
			}
			//普通应用 或者 总控制端，才停止主应用。集群机器一开始不启动，所以不用停止。
			if(server.getServerType().equals("0") || server.getServerType().equals("1")){
				// 获取运行程序窗口的PID,如果Domain路径录错，则无法正常获取和停止
				// cmdID是CMD窗口的进程ID，程序未启动完成是不能通过端口关闭的。
				String cmdWindowId = Tool.findProcessIdByCommandLineWindows(Tool
						.formatPath(server.getDomainPath()));
	
				// 如果程序已启动完成情况下，才可以根据端口获取程序进程，否则获取不了进程ID
				String portId = Tool.findPidByPortWindows(server.getDomainPort());
				// 直接终止程序
				if (cmdWindowId != null || portId != null) {
					// 根据应用程序的启动文件，把所有打开的cmd.exe应用窗口关闭
					if (cmdWindowId != null) {
						Tool.killProcessByPidWindows(cmdWindowId);
					}
					if (portId != null) {
						Tool.killProcessByPidWindows(portId);
					}
					result.setCodeMsg(Status.FAILED,"已将应用 <" + server.getServerIp() + ":"
							+ server.getDomainPort() + "> 停止.");
					responseHandler.sendMsgRun(result, socketChannel);
				} else {
					// 没有找到应用程序的窗口proccessId为null
					result.setCodeMsg(Status.SUCCEED,"应用端口 <" + server.getServerIp() + ":"
							+ server.getDomainPort() + "> 已停止（或未启动）.");
					responseHandler.sendMsgRun(result, socketChannel);
				}
			}
			result.setCodeMsg(Status.SUCCEED,"停止完毕.");
			responseHandler.sendMsgCompleted(result, socketChannel);
		} catch (Exception e) {
			e.printStackTrace();
			result.setCodeMsg(Status.FAILED,"停止Weblogic应用发生异常，原因: " + e.getMessage());
			log.info(result.getMsg());
		}
	}
}
