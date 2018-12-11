package cn.com.hnisi.agent.handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import org.apache.log4j.Logger;

import cn.com.hnisi.agent.interfaces.IPlatform;
import cn.com.hnisi.agent.middleware.WebLogicTool;
import cn.com.hnisi.agent.platform.Aix;
import cn.com.hnisi.agent.platform.Linux;
import cn.com.hnisi.agent.platform.Windows;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.FileModel;
import cn.com.hnisi.model.ResultModel;
import cn.com.hnisi.model.ServerModel;
import cn.com.hnisi.security.AgentPassword;
import cn.com.hnisi.type.ProcessStatus;
import cn.com.hnisi.type.RequestType;
import cn.com.hnisi.type.Status;
import cn.com.hnisi.util.Tool;

/**
 * 处理请求操作
 * 
 * @author WENZHIFENG
 * 
 */
public class AgentHandler implements Runnable {
	static Logger log = Logger.getLogger(AgentHandler.class);
	SocketChannel socketChannel = null;
	ServerSocketChannel serverSocketChannel;
	// 从客户端发送，服务端反序列生成
	ServerModel server = null;
	IPlatform platform=null;
	long startTime, endTime;

	// 记录客户端信息
	String clientInfo = new String();

	public AgentHandler(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
		//判断当前操作系统
		if (Tool.isAix()) {
			platform = new Aix(this);
		} else if (Tool.isLinux()) {
			platform = new Linux(this);
		} else if (Tool.isWindows()) {
			platform = new Windows(this);
		}
	}

	public void run() {
		try {
			ResultModel result = new ResultModel();
			socketChannel = serverSocketChannel.accept();
			if (socketChannel != null) {
				clientInfo = socketChannel.socket().getInetAddress()
						.getHostAddress()
						+ ":" + socketChannel.socket().getPort();
				log.info("客户端 [" + clientInfo + "] 已连接.");
				startTime = System.currentTimeMillis();
				// (4个字节的请求类型)+(4个字节的ServerModel对象字节长度+ServerModel对象字节)+(4个字节的命令类型+命令行List<String>字节长度+命令行List<String>字节)
				int size = 0;
				int request_type = 0;
				// 判断请求类型
				ByteBuffer buf = ByteBuffer.allocate(4);
				size=socketChannel.read(buf);
				if(size > 0) {
					buf.flip();
					request_type = buf.getInt();//请求类型
					buf.clear();
					/* 执行命令、上传文件、删除文件、获取服务器目录、获取应用日志
					 * 需要校验密码（即：需要先获取ServerModel对象里面的账号信息）
					 */
					if (request_type == RequestType.COMMAND.getValue()
							|| request_type == RequestType.UPLOAD_FILE
									.getValue()
							|| request_type == RequestType.GET_PATH.getValue()
							|| request_type == RequestType.VIEW_LOGS.getValue()
							|| request_type == RequestType.DOWNLOAD_FILE.getValue()
							|| request_type == RequestType.DELETE_FILE.getValue()) {
						
						// 获取ServerModel对象字节长度
						int serverLenght = 0;
						buf = ByteBuffer.allocate(4);
						if ((size = socketChannel.read(buf)) >= 0) {
							buf.flip();
							serverLenght = buf.getInt();
							buf.clear();
						}
						// =========开始-先获取ServerModel对象=============
						buf = ByteBuffer.allocate(serverLenght);
						int reads = 0;
						byte[] serverByte = null;
						ByteArrayOutputStream outServer = new ByteArrayOutputStream();
						while ((size = socketChannel.read(buf)) >= 0) {
							buf.flip();
							serverByte = new byte[size];
							buf.get(serverByte);
							outServer.write(serverByte);
							buf.clear();
							reads += size;
							if (reads == serverLenght) {
								break;
							}
						}
						Object objServer = Tool.ByteToObject(outServer
								.toByteArray());
						if (objServer instanceof ServerModel) {
							this.server = (ServerModel) objServer;
						}
						// =======结束-获取ServerModel对象=============

						// 开始校验密码
						String agentPassword = AgentPassword.getPassword();
						if (agentPassword == null) {
							agentPassword = "";
						}

						if (!(server.getAgentPassword().equals(agentPassword))) {
							result.setCode(-1);
							result.setMsg("Agent密码错误，拒绝访问");
							sendMsgCompleted(result, socketChannel);
							return;
						}

						// ===================判断是"执行命令"还是“上传文件”===========
						if (request_type == RequestType.UPLOAD_FILE.getValue()) {
							// 接收客户端上传的文件
							receiveFile(server, socketChannel);
						} else if (request_type == RequestType.COMMAND
								.getValue()) {
							// 执行请求操作
							executeCommand(socketChannel);
						} else if (request_type == RequestType.GET_PATH
								.getValue()) {
							// 客户端请求获取服务端路径
							browserServerPath(socketChannel);
						} else if (request_type == RequestType.VIEW_LOGS
								.getValue()) {
							// 获取控制台
							printConsole(server, socketChannel);
						} else if (request_type == RequestType.DOWNLOAD_FILE
								.getValue()) {
							// 发送文件件
							sendFileToClient(server, socketChannel);
						}else if(request_type == RequestType.DELETE_FILE
								.getValue()){
							//删除服务器文件
							deleteServerFile(socketChannel);
						}
					} else if (request_type == RequestType.CHANGE_PASSWORD
							.getValue()) {
						// 设置Agent密码
						changePassword(socketChannel);
					} else if (request_type == RequestType.TEST.getValue()) {
						// 与客户端测试连接
						agentTestConnect(socketChannel);
					} else {
						result.setCode(-1);
						result.setMsg(inedxHtml());
						responseHtml(result, socketChannel);
						socketChannel.socket().getOutputStream().close();
						return;
					}
				}
			}
		} catch (ConnectException ce) {
			log.info("客户端连接已断开");
		} catch (ClosedChannelException cce) {
			log.info("客户端连接已断开");
		} catch (IOException e) {
			log.error("处理请求异常:" + e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 判断请求的Server中间件类型是否为WebLogic
	 */
	private boolean isWebLogic() {
		if (server != null
				&& server.getMiddlewareType()!=null && server.getMiddlewareType().toLowerCase()
						.contains("weblogic")) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * 判断请求的Server中间件类型是否为WebSphere
	 */
	private boolean isWebSphere() {
		if (server != null
				&& server.getMiddlewareType()!=null &&server.getMiddlewareType().toLowerCase()
						.contains("websphere")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 发送文件到客户端
	 * 
	 * @param server
	 * @param socketChannel
	 */
	public void sendFileToClient(ServerModel server, SocketChannel socketChannel) {
		ByteBuffer buf;
		FileChannel fileChannel = null;
		FileInputStream fileInputStream = null;
		File downloadFile = null;
		try {
			int size = 0;
			int downloadFilePathLength = 0;
			buf = ByteBuffer.allocate(4);
			size = socketChannel.read(buf);
			if (size > 0) {
				buf.flip();
				downloadFilePathLength = buf.getInt();
				buf.clear();
			}
			byte[] fileByte = null;
			// 获取需要下载的文件
			StringBuffer strPath = new StringBuffer();
			if (downloadFilePathLength > 0) {
				buf = ByteBuffer.allocate(downloadFilePathLength);
				int canReadSize = downloadFilePathLength;
				while (canReadSize > 0) {
					size = socketChannel.read(buf);
					fileByte = new byte[size];
					buf.flip();
					buf.get(fileByte);
					buf.clear();
					canReadSize -= size;
					strPath.append(new String(fileByte));
				}
			}

			downloadFile = new File(strPath.toString());
			log.info("下载文件:" + downloadFile.getPath());
			uploadFile(socketChannel, downloadFile.getPath());
		} catch (IOException e) {
			log.error(e.getMessage());
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
				if (fileChannel != null) {
					fileChannel.close();
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 发送文件--->客户端
	 * 
	 * @param uploadFileName
	 * @return
	 */
	private void uploadFile(SocketChannel channel, String uploadFileName) {
		// TODO 上传文件

		FileInputStream fis = null;
		FileChannel fileChannel = null;
		ByteBuffer fileHeadBuffer = null;

		try {
			File file = new File(uploadFileName);
			String msg = "";
			if (file.exists()) {
				msg = "正在下载中";
				Tool.sendStatusCodeMessage(channel, 0, msg);
				//文件名长度，文件名字节，文件长度
				fileHeadBuffer = ByteBuffer
						.allocate(
								+ 4
								+ Tool.utfToIso(file.getName()).getBytes(
										"ISO-8859-1").length + 8);
				fileHeadBuffer.putInt(Tool.utfToIso(file.getName()).getBytes(
						"ISO-8859-1").length);// “文件名”长度 4
				fileHeadBuffer.put(Tool.utfToIso(file.getName()).getBytes(
						"ISO-8859-1"));// “文件名”内容
				fileHeadBuffer.putLong(file.length());// “文件”长度 8
				fileHeadBuffer.flip();
				channel.write(fileHeadBuffer);
				fileHeadBuffer.clear();

				int bufSize = 1024000;
				ByteBuffer fileBuffer = null;
				int size = 0;
				long remainingSize = file.length();// 剩余可读长度
				fis = new FileInputStream(file);
				fileChannel = fis.getChannel();
				System.out.println("正在上传文件  --> " + file.getPath() + " 文件大小:"
						+ Tool.getFileSize(file.length()));
				while (remainingSize > 0) {
					if (remainingSize <= bufSize) {
						bufSize = (int) remainingSize;
					}
					fileBuffer = ByteBuffer.allocate(bufSize);
					size = fileChannel.read(fileBuffer);
					if (size > 0) {
						fileBuffer.rewind();
						fileBuffer.limit(size);
						remainingSize -= size;
						channel.write(fileBuffer);
						fileBuffer.clear();
					}
				}
				fileBuffer = null;
				file = null;
				fileHeadBuffer = null;
			} else {
				msg = "下载失败,服务器文件 "+file.getPath()+" 不存在，请检查配置!";
				Tool.sendStatusCodeMessage(channel, -1, msg);
			}
		} catch (ClosedChannelException ex) {
			log.info("网络连接异常，连接失败或已关闭");	
		} catch (IOException e) {
			log.info("发送文件出错，原因:" + e.getMessage());
			try {
				socketChannel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			System.gc();
			log.info("回收系统资源");
			fileHeadBuffer = null;
			try {
				// 关闭流，避免文件被占用
				if (fis != null) {
					fis.close();
					fis = null;
				}
				if (fileChannel != null) {
					fileChannel.close();
					fileChannel = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 获取控制台输出日志内容
	 * @param server
	 * @param socketChannel
	 */
	public void printConsole(ServerModel server, SocketChannel socketChannel) {
		ByteBuffer buf;
		String msg = "";
		FileChannel fileChannel = null;
		RandomAccessFile file = null;
		File logFile = null;
		if (server.getMiddlewareType().toLowerCase().equals("weblogic")) {			
			logFile = new File(server.getDomainPath()+"/syslog.log");
		} else if (server.getMiddlewareType().toLowerCase().equals("websphere")) {
			logFile = new File(server.getDomainPath() + "/logs/"
					+ server.getServerName() + "/SystemOut.log");
		} else {
			msg = "不支持的中间件";
		}
		if (logFile != null && logFile.exists()) {
			long filePointer = 0;
			try {
				file = new RandomAccessFile(logFile, "r");
				while (true) {
					long fileLength = file.length();
					if (fileLength < filePointer) {
						// 如果读取点大于文件长度，将始点设为文件最后
						file = new RandomAccessFile(logFile, "r");
						filePointer = fileLength;
					}
					// 如果是第一次读取，把位置设置到文件最后位置
					if (filePointer == 0) {
						filePointer = fileLength;
					}

					if (fileLength > filePointer) {
						// 设置开始读取文件的位置
						file.seek(filePointer);
						fileChannel = file.getChannel();
						long bufSize = 8192;
						long remainingLength = fileLength - filePointer;// 剩余(可读)长度

						if (filePointer > 0) {
							if ((fileLength - filePointer) < bufSize) {
								bufSize = fileLength - filePointer;// 少于缓存大小，直接读取完毕
							}
							remainingLength = fileLength - filePointer;
						}

						int size = 0;
						//发送响应长度
						buf = ByteBuffer.allocate((int) remainingLength);
						buf.putInt((int) remainingLength);
						buf.flip();
						socketChannel.write(buf);
						buf.clear();
						
						//发送响应内容
						while (remainingLength > 0) {
							if (remainingLength < bufSize) {
								bufSize = remainingLength;
							}
							buf = ByteBuffer.allocate((int) bufSize);
							size = fileChannel.read(buf);
							if (size != -1) {
								buf.rewind();
								buf.limit(size);
								socketChannel.write(buf);
								buf.clear();
								remainingLength -= size;
							} else {
								break;
							}
						}
						// 将读取始点设置为最后一次的读取位置
						filePointer = file.getFilePointer();
					}
					// 每200毫秒扫描一次
					Thread.sleep(200);
				}
			} catch (Exception ex) {
				log.info("读取异常, 原因: " + ex.getMessage());
				ex.printStackTrace();
			} finally {
				try {
					if (file != null) {
						file.close();
					}
					if (fileChannel != null) {
						fileChannel.close();
					}
					socketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return;
		} else {
			msg = "不存在日志文件:" + logFile.getPath();
		}
		try {
			buf = ByteBuffer.allocate(4 + msg.getBytes(AgentConfig
					.getEncoding()).length);
			buf.putInt(msg.getBytes(AgentConfig.getEncoding()).length);
			buf.put(ByteBuffer.wrap(msg.getBytes(AgentConfig.getEncoding())));
			buf.flip();
			while (buf.hasRemaining()) {
				socketChannel.write(buf);
			}
			buf.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 响应客户端，浏览服务器目录
	 * 
	 * @param socketChannel
	 */
	public void browserServerPath(SocketChannel socketChannel) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		int size = 0;
		int pathLength = 0;
		try {
			size = socketChannel.read(buf);
			if (size > 0) {
				buf.flip();
				pathLength = buf.getInt();
				buf.clear();
			}
			buf = ByteBuffer.allocate(pathLength);
			size = socketChannel.read(buf);
			byte[] bytePath = null;
			if (size > 0) {
				buf.flip();
				bytePath = new byte[pathLength];
				buf.get(bytePath);
				buf.clear();
			}
			String path = "";
			List<FileModel> fileList = new ArrayList<FileModel>();
			// 判断客户请求的路径是否为空
			if (bytePath != null) {
				path = new String(bytePath, AgentConfig.getEncoding());
			}

			File file = new File(path);
			FileModel fm = null;
			// 如果请求目录为空，则返回硬盘盘符
			if (file.exists() && file.isDirectory()) {
				// 返回请求目录的所有子目录
				for (File sonFolder : Tool.filesOrderByName(file.getPath())) {
					if (!sonFolder.isHidden()) {
						fm = new FileModel();
						if(Tool.isAix()){
							fm.setName(Tool.isoToGbk(sonFolder.getName()));// 文件名
						}else{
							fm.setName(sonFolder.getName());// 文件名
						}
						fm.setPath(sonFolder.getPath());// 文件路径
						fm.setParentPath(sonFolder.getParent());// 父目录
						fm.setDirectory(sonFolder.isDirectory());// 是否为目录
						fm.setFile(sonFolder.isFile());// 是否为文件
						fm.setLength(sonFolder.length());// 文件长度
						fm.setDrive(false);
						fileList.add(fm);
					}
				}
				// 如果目录下没有子文件夹
				if (fileList.size() == 0) {
					fm = new FileModel();
					fm.setName(null);// 文件名
					fm.setPath(file.getPath());// 文件路径
					fm.setParentPath(file.getPath());// 父目录
					fm.setDirectory(true);// 是否为目录
					fm.setFile(false);// 是否为文件
					fm.setLength(0);// 文件长度
					fm.setDrive(false);
					fileList.add(fm);
				}
			} else if (file.exists() && file.isFile()) {
				// 文件
				for (File sonFolder : Tool.filesOrderByName(file.getParent())) {
					if (!sonFolder.isHidden()) {
						fm = new FileModel();
						fm.setName(sonFolder.getName());// 文件名
						fm.setPath(sonFolder.getPath());// 文件路径
						fm.setParentPath(sonFolder.getParent());// 父目录
						fm.setDirectory(sonFolder.isDirectory());// 是否为目录
						fm.setFile(sonFolder.isFile());// 是否为文件
						fm.setLength(sonFolder.length());// 文件长度
						fm.setDrive(false);
						fileList.add(fm);
					}
				}
			} else {
				if (Tool.isWindows()) {
					FileSystemView sys = FileSystemView.getFileSystemView();
					File[] files = File.listRoots();
					for (File root : files) {

						fm = new FileModel();
						fm.setName(root + "  "
								+ sys.getSystemTypeDescription(root));// 文件名
						fm.setPath(root.getPath());// 文件路径
						fm.setParentPath(root.getPath());// 父目录
						fm.setDirectory(true);// 是否为目录
						fm.setFile(false);// 是否为文件
						fm.setLength(0);// 文件长度
						fm.setDrive(true);
						fileList.add(fm);
					}
				} else {
					fm = new FileModel();
					fm.setName("/");// 文件名
					fm.setPath("/");// 文件路径
					fm.setParentPath("/");// 父目录
					fm.setDirectory(true);// 是否为目录
					fm.setFile(false);// 是否为文件
					fm.setLength(0);// 文件长度
					fm.setDrive(true);
					fileList.add(fm);
				}
			}

			byte[] fileListByte = Tool.objectToByte(fileList);

			ByteBuffer pathBuf = ByteBuffer.allocate(4 + fileListByte.length);
			pathBuf.putInt(fileListByte.length);
			pathBuf.put(ByteBuffer.wrap(fileListByte));
			pathBuf.flip();
			while (pathBuf.hasRemaining()) {
				socketChannel.write(pathBuf);
			}
			pathBuf.clear();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 删除服务器文件
	 * @param socketChannel
	 */
	public void deleteServerFile(SocketChannel socketChannel){
		ResultModel result=new ResultModel();
		try{
			/*
			 * 4 请求类型 前面已经读取
			 * 4+ServerModel_byte.length 前面已经读取
			 * 4+List<FileModel>_byte.length 需要读取
			 */
			ByteBuffer buf=ByteBuffer.allocate(4);
			int size=0;
			int fileModelLength=0;
			
			size=socketChannel.read(buf);
			if(size>=0){
				buf.flip();
				fileModelLength=buf.getInt();
				buf.clear();
			}else{
				result.setCodeMsg(Status.FAILED, "获取文件列表长度数据异常!");
				sendMsgCompleted(result, socketChannel);
				return;
			}
				
			buf = ByteBuffer.allocate(fileModelLength);
			byte[] byteFileModel=null;
			int canReadSize = fileModelLength;
			ByteArrayOutputStream out_fileModel = new ByteArrayOutputStream();
			while (canReadSize > 0) {
				size = socketChannel.read(buf);
				byteFileModel = new byte[size];
				buf.flip();
				buf.get(byteFileModel);
				out_fileModel.write(byteFileModel);
				buf.clear();
				canReadSize -= size;
			}
			@SuppressWarnings("unchecked")
			List<FileModel> fileModelList=(List<FileModel>)Tool.ByteToObject(out_fileModel.toByteArray());
			if(fileModelList!=null && fileModelList.size()>0){
				for(FileModel fm:fileModelList){
					if(fm.isDirectory()){
						log.info("文件夹: "+fm.getPath()+" 已被<"+clientInfo+">删除");
					}else{
						log.info("文件: "+fm.getPath()+" 已被<"+clientInfo+">删除");
					}
					result = Tool.deleteFile(fm.getPath());
					System.out.println(result.getCode()+"-"+result.getMsg());
					//判断是否删除成功
					if(result.getCode()<0){
						result.setCodeMsg(Status.FAILED, result.getMsg());
						sendMsgCompleted(result, socketChannel);
						return;
					}
				}
				result.setCodeMsg(Status.SUCCEED, "指定文件已删除");
			}else{
				result.setCodeMsg(Status.FAILED, "文件列表为空");
			}			
			sendMsgCompleted(result, socketChannel);
		}catch(Exception e){
			e.printStackTrace();
			result.setCodeMsg(Status.FAILED, e.getMessage());
			sendMsgCompleted(result, socketChannel);
		}
	}
	/**
	 * 接收客户端发送的“测试连接”请求
	 * 
	 * @param socketChannel
	 */
	public void agentTestConnect(SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		ByteBuffer buf = null;
		int size;
		String pwd = null;
		String agentPwd = null;
		int pwdLength = 0;
		byte[] pwdByte = null;
		try {
			buf = ByteBuffer.allocate(4);
			size = socketChannel.read(buf);
			// 获取密码长度
			if (size > 0) {
				buf.flip();
				pwdLength = buf.getInt();
				buf.clear();
			}
			// 获取密码长度，一次读取完毕
			if (pwdLength > 0) {
				buf = ByteBuffer.allocate(pwdLength);
				size = socketChannel.read(buf);
				buf.flip();
				pwdByte = new byte[pwdLength];
				buf.get(pwdByte);
				buf.clear();
			}
			if (pwdByte != null) {
				pwd = new String(pwdByte);
				agentPwd = AgentPassword.getPassword();
				if (agentPwd == null || agentPwd.equals("")) {
					agentPwd = Tool.EncoderByMd5("");
				}
				if (pwd.equals(agentPwd)) {
					result.setCode(0);
					result.setMsg("Agent校验通过，连接成功!");
					sendMsgCompleted(result, socketChannel);
					return;
				} else {
					result.setCode(-1);
					result.setMsg("校验失败，Agent密码错误，被拒绝访问!");
					sendMsgCompleted(result, socketChannel);
					return;
				}
			} else {
				result.setCode(-1);
				result.setMsg("Agent密码错误，被拒绝访问!");
				sendMsgCompleted(result, socketChannel);
				return;
			}
		} catch (IOException e) {
			result.setCode(-1);
			result.setMsg("Agent连接异常!");
			sendMsgCompleted(result, socketChannel);
			return;
		}finally{
			try {
				log.info("释放连接");
				socketChannel.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 设置Agent密码
	 * 
	 * @param socketChannel
	 */
	public void changePassword(SocketChannel socketChannel) {
		// TODO 修改密码
		ResultModel result = new ResultModel();
		ByteBuffer buf = null;
		int size;
		buf = ByteBuffer.allocate(4);
		String oldPassword = null;
		String newPassword = null;
		int oldPwdLength = 0;
		int newPwdLength = 0;
		try {
			size = socketChannel.read(buf);
			if (size >= 0) {
				buf.flip();
				oldPwdLength = buf.getInt();
				buf.clear();
			}
			buf = ByteBuffer.allocate(oldPwdLength);
			byte[] oldPwdByte = null;
			if ((size = socketChannel.read(buf)) >= 0) {
				buf.flip();
				oldPwdByte = new byte[oldPwdLength];
				buf.get(oldPwdByte);
				buf.clear();
			}
			buf = ByteBuffer.allocate(4);
			if ((size = socketChannel.read(buf)) >= 0) {
				buf.flip();
				newPwdLength = buf.getInt();
				buf.clear();
			}
			buf = ByteBuffer.allocate(newPwdLength);
			byte[] newPwdByte = null;
			if ((size = socketChannel.read(buf)) >= 0) {
				buf.flip();
				newPwdByte = new byte[newPwdLength];
				buf.get(newPwdByte);
				buf.clear();
			}
			if (oldPwdByte != null && newPwdByte != null) {
				oldPassword = new String(oldPwdByte);
				newPassword = new String(newPwdByte);
				String agentPwd = AgentPassword.getPassword();
				// 如果密码不为空时与“原密码”进行判断
				if (agentPwd != null && !agentPwd.equals("")) {
					if (!oldPassword.equals(agentPwd)) {
						result.setCode(-1);
						result.setMsg("录入的“原密码”与服务器Agent密码不一致!");
						sendMsgCompleted(result, socketChannel);
						return;
					}
				}
				if (AgentPassword.changePassword(newPassword, socketChannel
						.socket().getInetAddress().toString())) {
					result.setCode(0);
					result.setMsg("修改成功，服务器Agent密码已更改");
					sendMsgCompleted(result, socketChannel);
				}
			} else {
				result.setCode(-1);
				result.setMsg("设置服务器Agent密码失败");
				sendMsgCompleted(result, socketChannel);
			}
		} catch (IOException e) {
			result.setCode(-1);
			result.setMsg("设置服务器Agent密码失败");
			sendMsgCompleted(result, socketChannel);
		}
	}

	/**
	 * 执行命令
	 * 
	 * @param socketChannel
	 *            SocketChannel通道
	 * @param command
	 *            命令行
	 * @param operationType
	 *            操作类型
	 */
	public void executeCommand(SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		RequestType operationType;
		int operationValue = 0;
		ByteBuffer buf = null;
		// =====开始-获取命令行类型(备份、启动、停止)=====
		try {
			buf = ByteBuffer.allocate(4);
			if (socketChannel.read(buf) >= 0) {
				buf.flip();
				operationValue = buf.getInt();
				buf.clear();
			}
			if (operationValue == 101) {
				// 备份
				operationType = RequestType.COMMAND_BACKUP;
			} else if (operationValue == 102) {
				// 启动
				operationType = RequestType.COMMAND_START;
			} else if (operationValue == 103) {
				// 停止
				operationType = RequestType.COMMAND_STOP;
			} else {
				result.setCode(-1);
				result.setMsg("无法识别的命令代码: " + operationValue);
				return;
			}

			String systemInfo = "服务器操作系统 : " + Tool.getSystemVersion();
			result.setMsg(systemInfo);
			sendMsgRun(result, socketChannel);

			String serverInfo = server.getServerIp() + ":"
					+ server.getDomainPort();
			if (operationType == RequestType.COMMAND_START) {
				log.info("客户端 [" + clientInfo + "]对应用进行了启动操作");
				// 启动应用
				result.setMsg("正在启动应用<" + serverInfo + ">，请稍候...");
				sendMsgRun(result, socketChannel);
				if(isWebLogic()){
					platform.startWebLogic(server, socketChannel);
				}else if(isWebSphere()){
					platform.startWebSphere(server, socketChannel);
				}else {
					result.setMsg("不支持的中间件!");
					sendMsgCompleted(result, socketChannel);
					return;
				}
			} else if (operationType == RequestType.COMMAND_STOP) {
				log.info("客户端 [" + clientInfo + "]对应用进行了停止操作");
				// 停止应用
				result.setMsg("正在关闭应用<" + serverInfo + ">，请稍候...");
				sendMsgRun(result, socketChannel);
				if (isWebLogic()) {
					platform.stopWebLogic(server, socketChannel);
				} else if (isWebSphere()) {
					platform.stopWebSphere(server, socketChannel);
				} else {
					result.setMsg("不支持的中间件!");
					sendMsgCompleted(result, socketChannel);
					return;
				}

			} else if (operationType == RequestType.COMMAND_BACKUP) {
				log.info("客户端 [" + clientInfo + "]对应用进行了备份操作");
				// 备份应用(备份不用区分中间件类型)
				result.setMsg("正在备份应用<" + serverInfo + ">，请稍候...");
				sendMsgRun(result, socketChannel);
				// 备份方式：0-复制文件夹;1-压缩
				if (server.getBackupType() != null
						&& server.getBackupType().equals("1")) {
					if(platform!=null){
						platform.backUp(server, socketChannel);
					}else{
						result.setMsg("获取操作对象失败！");
						sendMsgCompleted(result, socketChannel);
						return;
					}
				} else {
					if(platform!=null){
						platform.backUpByCopy(server, socketChannel);
					}else{
						result.setMsg("不支持系统:" + Tool.getSystemVersion());
						sendMsgCompleted(result, socketChannel);
						return;
					}
				}

			} else {
				result.setMsg("无法识别的请求命令" + operationType);
				sendMsgCompleted(result, socketChannel);
			}
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(-1);
			result.setMsg("网络异常: " + e.getMessage());
		} finally {
			if (socketChannel != null) {
				try {
					socketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 接收客户端上传的文件
	 * 
	 * @param requestFile
	 * @param socketChannel
	 * @throws IOException
	 */
	public void receiveFile(ServerModel server, SocketChannel socketChannel) {
		ResultModel result = new ResultModel();
		// 下面来获取待接收文件数
		ByteBuffer buf = null;
		FileOutputStream out = null;
		int fileCount = 0;//客户端上传文件数
		int size = 0;
		try {
			buf = ByteBuffer.allocate(4);
			size = socketChannel.read(buf);
			if (size >= 0) {
				buf.flip();
				fileCount = buf.getInt();
				log.info("客户端 [" + clientInfo + "] 正在上传 " + fileCount + "个文件");
				buf.clear();
			}

			// 循环获取文件(文件名，文件内容，文件路径、文件路径内容)
			for (int i = 0; i < fileCount; i++) {
				// 获取文件名长度
				buf = ByteBuffer.allocate(4);
				int fileNameLength = 0;
				size = socketChannel.read(buf);
				if (size > 0) {
					buf.flip();
					fileNameLength = buf.getInt();
					buf.clear();
				}
				// 获取文件名长度，一般不超过1K
				if (fileNameLength > 1024) {
					result.setCodeMsg(-1,"接收文件出错，文件名错误");
					sendMsgCompleted(result, socketChannel);
					return;
				}
				// 获取文件名内容，，写入数组 fileNameByte
				byte[] fileNameByte = null;
				buf = ByteBuffer.allocate(fileNameLength);
				size = socketChannel.read(buf);
				if (size > 0) {
					buf.flip();
					fileNameByte = new byte[size];
					buf.get(fileNameByte);
					buf.clear();
				}
				// 获取文件路径长度
				buf = ByteBuffer.allocate(4);
				size = socketChannel.read(buf);
				int filePathLength = 0;
				if (size > 0) {
					buf.flip();
					filePathLength = buf.getInt();
					buf.clear();
				}

				// 获取文件路径，一般不超过1K
				if (filePathLength > 1024) {
					result.setCodeMsg(-1,"接收文件出错,文件路径错误");
					sendMsgCompleted(result, socketChannel);
					return;
				}
				//获取文件路径内容，写入数组 filePathByte
				buf = ByteBuffer.allocate(filePathLength);
				byte[] filePathByte = null;
				size = socketChannel.read(buf);
				if (size >= 0) {
					buf.flip();
					filePathByte = new byte[size];
					buf.get(filePathByte);
					buf.clear();
				}
				// 文件路径，例：\web\testFile\V5.2.0_Release(ybdy)_KSYDJYJK_WH.txt
				// 原始编码状态，用来响应客户端，保持编码一致，否则会出现乱码。
				String sourceEncodeFile = new String(filePathByte, "UTF-8");

				String filePath = "";// 需要根据操作系统转换编码
				if (Tool.isAix()) {
					// 如果是AIX操作系统需要转换编码，否则文件名会乱码
					filePath = Tool.gbkToISO88591(new String(filePathByte));
				} else {
					filePath = new String(filePathByte, "UTF-8");
				}

				// 获取文件长度
				buf = ByteBuffer.allocate(8);
				long fileLength = 0;
				size = socketChannel.read(buf);
				if (size >= 0) {
					buf.flip();
					fileLength = buf.getLong();
					buf.clear();
				}
				// 应用路径
				File serverPath = new File(server.getAppPath());
				// 构建文件目录路径
				filePath = filePath.replace("\\", "/");
				if (filePath.indexOf("/" + serverPath.getName() + "/") == 0) {
					filePath = serverPath.getParent() + File.separator
							+ filePath;
					sourceEncodeFile = serverPath.getParent() + File.separator
							+ sourceEncodeFile;
					
				} else {
					filePath = serverPath.getPath() + File.separator + filePath;
					sourceEncodeFile = serverPath.getPath() + File.separator
							+ sourceEncodeFile;
				}
				
				filePath = filePath.replace("\\", "/").replace("//", "/");
				//格式化返回路径格式
				if(Tool.isAix()||Tool.isLinux()){
					sourceEncodeFile = sourceEncodeFile.replace("\\", "/").replace("//", "/");
				}else{
					sourceEncodeFile = sourceEncodeFile.replace("/", "\\").replace("\\\\", "\\");
				}
				//部署文件
				File deployfile = new File(filePath);
				// 保证读取的数据长度与文件长度一致，避免文件数据丢失
				log.info("正在接收文件 " + deployfile.getPath() + " 文件大小: "
						+ Tool.getFileSize(deployfile.length()));

				if (!deployfile.getParentFile().exists()) {
					deployfile.getParentFile().mkdirs();
				}
				if (!deployfile.exists()) {
					try {
						deployfile.createNewFile();
					} catch (Exception ec) {
						result.setCode(-1);
						result.setMsg("创建文件出错，原因:" + ec.getMessage());
						log.info(result.getMsg());
						sendMsgCompleted(result, socketChannel);
						return;
					}
				}
				try {
					out = new FileOutputStream(deployfile);
				} catch (IOException ioe) {
					result.setCode(-1);
					result.setMsg("接收文件 “" + deployfile.getPath() + "” 出错，原因:"
							+ ioe.getMessage()+"，请检查服务器文件或磁盘是否有足够的存储空间!");
					log.info(result.getMsg());
					sendMsgCompleted(result, socketChannel);
					return;
				}
				long writedSize = 0;
				// 获取文件内容
				byte[] fileByte = null;
				// 剩余可读取的长度
				long remainingSize = fileLength;

				int bufSize = 102400;
				while (fileLength != -1) {
					bufSize = 1024 * 1024;
					if (remainingSize < bufSize) {
						bufSize = (int) remainingSize;
					}
					buf = ByteBuffer.allocate(bufSize);
					size = socketChannel.read(buf);
					if (size >= 0) {
						buf.flip();
						fileByte = new byte[size];
						buf.get(fileByte);
						writedSize += size;
						//写到本地文件
						Tool.writeFile(out, fileByte);
						buf.clear();
						remainingSize = fileLength - writedSize;// 计算剩余可读取的字节数
						if (remainingSize == 0) {
							result.setCodeMsg(Status.SUCCEED,"服务器正在接收文件: " + sourceEncodeFile);
							sendMsgRun(result, socketChannel);
							out.flush();
							out.close();
							//如果是集群服务则同步部署文件到集服服务	
							WebLogicTool.syncDeployFile(this,socketChannel,server,deployfile);
							
							result.setCodeMsg(Status.SUCCEED,"文件 “" + sourceEncodeFile+"” 已接收完毕.");
							sendMsgCompleted(result, socketChannel);
							break;
						}
					}
				}//while读写文件
				sendMsgCompleted(result, socketChannel);
			}//end-for循环
			result.setMsg("全部接收完毕，共  " + fileCount + " 个文件.");
			log.info(result.getMsg());
			sendMsgCompleted(result, socketChannel);
		} catch (Exception e) {
			e.printStackTrace();
			result.setMsg("接收文件出错，原因: " + e.getMessage());
			log.info(result.getMsg());
			sendMsgCompleted(result, socketChannel);
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
				if (socketChannel != null) {
					socketChannel.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送信息，任务、循环运行中，未完成。
	 * 发送成功返回True，发送失败False
	 * 
	 * @param result
	 * @param socketChannel
	 * @return
	 */
	public boolean sendMsgRun(ResultModel result, SocketChannel socketChannel) {
		return sendMsg(result, socketChannel, ProcessStatus.Run);
	}

	/**
	 * 发送信息，操作已完成，调整此方法将通知客户端断开Socket
	 * 发送成功返回True，发送失败False
	 * 
	 * @param result
	 * @param socketChannel
	 * @return
	 */
	public boolean sendMsgCompleted(ResultModel result,
			SocketChannel socketChannel) {
		return sendMsg(result, socketChannel, ProcessStatus.Completed);
	}

	/**
	 * 响应请求-发送信息给请求端 -1退出，0进行中
	 * 
	 * @param result
	 * @param socketChannel
	 * @param status
	 *            0连接中，-1断开连接
	 */
	private boolean sendMsg(ResultModel result, SocketChannel socketChannel,
			ProcessStatus ps) {
		byte[] byteObject;
		try {
			byteObject = result.getMsg().getBytes("UTF-8");
			ByteBuffer responseBuf = ByteBuffer
					.allocate(12 + byteObject.length);
			responseBuf.putInt(ps.getValue());// 0-已完成,1-进行中
			responseBuf.putInt(result.getCode());// 4:处理结果0-正常，非0为异常
			responseBuf.putInt(byteObject.length);// 4:返回结果字符串的长度
			responseBuf.put(ByteBuffer.wrap(byteObject));// 返回结果字符串
			responseBuf.flip();
			while (responseBuf.hasRemaining()) {
				if (socketChannel != null) {
					socketChannel.write(responseBuf);
				} else {
					log.info("发送信息失败，连接已断开");
					break;
				}
			}
			responseBuf.clear();
			return true;
		}catch(ClosedChannelException cce){
			cce.printStackTrace();
			log.error("已关闭连接");
			return false;
		} catch (IOException e) {
			log.error("连接中断，发送信息失败: " + e.getMessage());
			return false;
		}
	}

	/**
	 * 响应HTML内容
	 * 
	 * @param result
	 * @param socketChannel
	 */
	private void responseHtml(ResultModel result, SocketChannel socketChannel) {
		byte[] byteObject;
		try {
			byteObject = result.getMsg().getBytes(AgentConfig.getEncoding());
			ByteBuffer responseBuf = ByteBuffer.allocate(byteObject.length);
			responseBuf.put(ByteBuffer.wrap(byteObject));
			responseBuf.flip();
			while (responseBuf.hasRemaining()) {
				if (socketChannel != null) {
					socketChannel.write(responseBuf);
				} else {
					log.info("发送信息失败，连接已断开");
					break;
				}
			}
			responseBuf.clear();
		} catch (IOException e) {
			e.printStackTrace();
			log.error("发送信息失败: " + e.getMessage());
		}
	}

	public String inedxHtml() {
		StringBuffer str = new StringBuffer();
		str.append("<!DOCTYPE html>");
		str.append("<html>");
		str.append("<head>");
		str.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>");
		str.append(" <title>程序发布管理工具-Agent代理服务器@广州华资软件有限公司-作者：温志锋</title>");
		// 样式
		str.append("<style type=\"text/css\">");
		str.append("body { background-color:#f7f8f8;font-family:Tahoma }");
		str.append(" .main{ box-shadow: 10px 10px 5px #888888;text-align:center;background-color:#6fb1e2;border-radius: 20px;width: 600px;height: 350px;margin: auto;position: absolute;top: 0;left: 0;right: 0;bottom: 0;}");
		str.append(" H1{color:#ffffff;font-size:30px;margin-top:50px;margin-bottom:50px;}");
		str.append(" H3{color:#ffffff;margin-top:40px;}");
		str.append(" p {font-size:20px;color:#ffffff;font-weight:bold;}");
		str.append("a{ text-decoration:none;}");
		str.append("</style>");

		str.append("</head>");
		str.append("<body>");
		// 主体内容
		str.append("<div class=\"main\" >");
		str.append("<H1>程序发布管理工具-Agent代理服务器</H1>");
		str.append("<p>广州华资软件有限公司</p>");
		str.append("<p><a href=\"http://www.sinobest.cn\">www.sinobest.cn</a></p>");
		str.append("<p>Version 2017</p>");
		str.append("<h3>Copyright (C) 2017 All Rights Reserved.</h3>");
		str.append("</div>");

		str.append("</body>");
		str.append("</html>");
		return str.toString();
	}

}
