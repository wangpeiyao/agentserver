package cn.com.hnisi.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;

import sun.misc.BASE64Encoder;
import cn.com.hnisi.config.AgentConfig;
import cn.com.hnisi.model.ResultModel;
import cn.com.hnisi.type.PlatformType;
import cn.com.hnisi.type.Status;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

public class Tool {
	static Logger log = Logger.getLogger(Tool.class);

	/**
	 * 利用MD5进行加密
	 * 
	 * @param str
	 *            待加密的字符串
	 * @return 加密后的字符串
	 * @throws NoSuchAlgorithmException
	 *             没有这种产生消息摘要的算法
	 * @throws UnsupportedEncodingException
	 */
	public static String EncoderByMd5(String str) {
		// 确定计算方法
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			BASE64Encoder base64en = new BASE64Encoder();
			String newstr = base64en.encode(md5.digest(str.getBytes(AgentConfig
					.getEncoding())));
			return newstr;
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 将对象转化为二进制
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	public static byte[] objectToByte(Object obj) throws IOException {
		byte[] bytes = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(os);
		out.writeObject(obj);
		bytes = os.toByteArray();
		os.close();
		out.close();
		return bytes;
	}

	/**
	 * 将二进制转化为Object对象
	 * 
	 * @param bt
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object ByteToObject(byte[] bt) throws IOException,
			ClassNotFoundException {
		Object obj = null;
		ByteArrayInputStream is = new ByteArrayInputStream(bt);
		ObjectInputStream in = new ObjectInputStream(is);
		obj = in.readObject();
		is.close();
		in.close();
		return obj;
	}

	/**
	 * 将毫秒转化[h小时 m分 s秒 ms毫秒]
	 * 
	 * @param time
	 * @return h小时 m分 s秒 ms毫秒
	 */
	public static String millisecondFormat(long time) {
		long t = time;
		long h = 0;
		long m = 0;
		long s = 0;
		long ms = 0;
		h = t / 3600000;// 取小时
		t = t % 3600000;
		m = t / 60000;// 取分钟
		t = t % 60000;
		s = t / 1000;// 取秒
		t = t % 1000;
		ms = t;// 取毫秒

		return h + "小时 " + m + "分 " + s + "秒 " + ms + "毫秒";
	}

	/**
	 * 获取现在时间
	 * 
	 * @return 返回时间类型 yyyy-MM-dd HH:mm:ss
	 */
	public static String getNowDate() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss:SSS");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/**
	 * 获取现在时间
	 * 
	 * @return 返回时间类型 yyyyMMddHHmmssSSS
	 */
	public static String getNowDateSSS() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/**
	 * 把接收的数据写到本地文件里
	 * 
	 * @param drc
	 *            本地目录
	 * @param fileName
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static void writeFile(String filePath, byte[] fileByte)
			throws UnsupportedEncodingException {
		FileOutputStream os = null;
		try {
			File file = new File(filePath);
			if (!file.getParentFile().exists()) {
				log.info("创建目录: " + file.getParentFile());
				file.getParentFile().mkdirs();
			}
			if (!file.exists()) {
				log.info("新建文件: " + file.getPath());
				file.createNewFile();
			}
			file.setWritable(true);
			log.info("写入文件(字节数:" + fileByte.length + "): " + file.getPath());

			os = new FileOutputStream(file);
			os.write(fileByte);
			os.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeFile(FileOutputStream out, byte[] fileByte)
			throws IOException {
		out.write(fileByte);
		out.flush();
	}

	/**
	 * 计算百分比
	 * 
	 * @param member
	 *            分子
	 * @param denominator
	 *            分母(不能为0)
	 * @return
	 */
	public static String getPercent(double member, double denominator) {
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);
		return numberFormat.format(member / denominator * 100) + "%";
	}

	/**
	 * 获取所有文件
	 * 
	 * @param fileList
	 * @param dirPath
	 */
	public static void getFiles(List<String> fileList, String dirPath) {
		File file = new File(dirPath);
		if (file.isDirectory() && !file.isHidden()) {
			for (String f : file.list()) {
				getFiles(fileList, dirPath + "\\" + f);
			}
		} else {
			if (fileList != null) {
				fileList.add(file.getPath());
			}
		}
	}

	/**
	 * 获取文件大小B\KB\MB\GB
	 * 
	 * @param size
	 * @return
	 */
	public static String getFileSize(long size) {
		// 如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
		if (size < 1024) {
			return String.valueOf(size) + " B";
		} else {
			size = size / 1024;
		}
		// 如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
		// 因为还没有到达要使用另一个单位的时候
		// 接下去以此类推
		if (size < 1024) {
			return String.valueOf(size) + " KB";
		} else {
			size = size / 1024;
		}
		if (size < 1024) {
			// 因为如果以MB为单位的话，要保留最后1位小数，
			// 因此，把此数乘以100之后再取余
			size = size * 100;
			return String.valueOf((size / 100)) + "."
					+ String.valueOf((size % 100)) + " MB";
		} else {
			// 否则如果要以GB为单位的，先除于1024再作同样的处理
			size = size * 100 / 1024;
			return String.valueOf((size / 100)) + "."
					+ String.valueOf((size % 100)) + " GB";
		}
	}

	/**
	 * 检查端口是否已经在使用中 如果端口已在监听中则返回treu，未使用则返回false
	 * 
	 * @param port
	 * @return
	 */
	public static boolean checkPortWindows(String port) {
		int count = 0;
		List<String> commands = new ArrayList<String>();
		commands.add("cmd.exe");
		commands.add("/C");
		commands.add("netstat");
		commands.add("-ano|findstr");
		commands.add(getIp() + ":" + port);
		debugShowCommand(commands);
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = null;
		pb.redirectErrorStream(true);
		BufferedReader in = null;
		try {
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));

			@SuppressWarnings("unused")
			String line = "";
			while ((line = in.readLine()) != null) {
				count++;
			}
			// 如果count>0表示端口正在使用中
			if (count > 0) {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			pb.directory();
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 * 根据端口获取应用程序的PID
	 * 
	 * @param port
	 * @return
	 */
	public static String findPidByPortWindows(String port) {
		List<String> commands = new ArrayList<String>();
		commands.add("cmd.exe");
		commands.add("/C");
		commands.add("netstat");
		commands.add("-ano|findstr");
		commands.add(":" + port);
		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p = null;
		pb.redirectErrorStream(true);
		BufferedReader in = null;
		try {
			p = pb.start();
			p.getOutputStream().close();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));
			String pid = null;
			while ((pid = in.readLine()) != null) {
				// 输出格式： TCP 192.168.56.1:7788 0.0.0.0:0 LISTENING 7052
				if (pid.toUpperCase().contains("LISTENING")) {
					pid = pid.substring(pid.lastIndexOf(" ") + 1);
					return pid;
				}
			}
			return pid;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			pb.directory();
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 根据PID终止进程
	 * 
	 * @param pid
	 * @return
	 */
	public static String killProcessByPidWindows(String pid) {
		List<String> commands = new ArrayList<String>();
		commands.add("cmd.exe");
		commands.add("/C");
		commands.add("taskkill");
		commands.add("/T");// /T 终止指定的进程和由它启用的子进程
		commands.add("/F");// /F 指定强制终止进程
		commands.add("/PID");
		commands.add(pid);
		log.info("Kill process " + pid);
		Process p = null;
		BufferedReader in =null;
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.redirectErrorStream(true);
		try {
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			String console = "";
			while ((console = in.readLine()) != null) {
				console += console;
			}
			return console;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 查找命令行包含了commandLine(自动去掉命令行中的空格再进行查找)的进程，并获取它的进程PID。
	 * 
	 * @(PID只是窗口进程的PID，非应用程序的PID) 如果没有找到相应进程，返回Null。
	 * @param commandLine
	 * @return
	 */
	public static synchronized String findProcessIdByCommandLineWindows(String commandLine) {
		/**
		 * 不能使用Windows自带的WMIC命令工具，因为WMIC在Windows Services 2003系统下，不能兼容多线程操作。
		 */
		WinDll.ProcessInfo process = WinDll.GetCmdLine(commandLine);
		
		if (process != null) {
			return String.valueOf(process.getProcessId());
		}
		return null;
	}

	/**
	 * 终止进程
	 * 
	 * @param process
	 */
	public static void killProcess(Process process) {
		String pid = getProcessIdByProcess(process);
		if (pid != null) {
			if (isWindows()) {
				killProcessByPidWindows(pid);
			} else if (isAix()) {
				KillProcessByPidAix(pid);
			} else if (isLinux()) {
				KillProcessByPidLinux(pid);
			}
		}
	}

	/**
	 * 获取Process进程的PID
	 * 
	 * @param process
	 * @return
	 */

	public static String getProcessIdByProcess(Process process) {
		String pid = null;
		try {
			if (Tool.isWindows()) {
				if (process.getClass().getName()
						.equals("java.lang.Win32Process")
						|| process.getClass().getName()
								.equals("java.lang.ProcessImpl")) {

					Field f = process.getClass().getDeclaredField("handle");
					f.setAccessible(true);
					long handl = f.getLong(process);
					Kernel32 kernel = Kernel32.INSTANCE;
					WinNT.HANDLE handle = new WinNT.HANDLE();
					handle.setPointer(Pointer.createConstant(handl));
					int ret = kernel.GetProcessId(handle);
					return String.valueOf(ret);

				}
			} else if (Tool.isLinux() || Tool.isAix()) {

				Field field = null;
				Class<?> clazz = Class.forName("java.lang.UNIXProcess");
				field = clazz.getDeclaredField("pid");
				field.setAccessible(true);
				pid = field.get(process) == null ? "-1" : field.get(process)
						.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pid;
	}

	/**
	 * 判断程序是否正在运行，运行中返回：true
	 * 
	 * @param platform
	 * @param pid
	 * @return
	 */
	public static boolean processIsRunning(PlatformType platform, String pid) {
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		try {
			final Process pidof;
			if (platform == PlatformType.LINUX || platform == PlatformType.AIX) {
				// kill -0 pid 不发送任何信号，但是系统会进行错误检查。
				// 所以经常用来检查一个进程是否存在，存在返回0；不存在返回1
				pidof = Runtime.getRuntime().exec(
						new String[] { "kill", "-0", pid });
				return pidof.waitFor() == 0;
			} else {
				final String[] cmd = { "tasklist.exe", "/FI", "PID eq " + pid,
						"/FO", "CSV" };
				pb = new ProcessBuilder(Arrays.asList(cmd));
				pb.redirectErrorStream(true);
				p= pb.start();
				p.getOutputStream().close();
				in = new BufferedReader(new InputStreamReader(
						p.getInputStream(), AgentConfig.getEncoding()));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.contains("\"" + pid + "\"")) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 * 根据pid获取PPID
	 * 
	 * @param pid
	 * @return
	 */
	public static String getPPidByPidWindows(String pid) {
		return WinDll.getParentProcessId(Integer.parseInt(pid)) + "";
	}

	/**
	 * 需要Root管理员身份
	 * 
	 * @param port
	 * @return
	 */
	public static String findPidByPortAix(String port) {
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		try {

			String[] commandList = {
					"/bin/sh",
					"-c",
					"netstat -An|grep LISTEN|grep -e '\\."
							+ port
							+ " '|cut -d' ' -f1|xargs -i{} rmsock {} tcpcb|awk '{print $9}' " };
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));
			String pid = null;
			/*
			 * 执行命令行： netstat -An|grep LISTEN|grep -e '\.6666'|cut -d' ' -f1|xargs -i{} rmsock {} tcpcb|awk '{print $9}'
			 * 查询结果输出：
			 * 6422994
			 * 6422994
			 * 6422994
			 */
			while ((pid = in.readLine()) != null) {
				try{
					Integer.parseInt(pid);//检查一下是否为进程PID数字
					return pid;
				}catch(Exception e){
					return null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 获取Linux进程 PID
	 * 
	 * @param port
	 * @return
	 */
	public static String findPidByPortLinux(String port) {
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in = null;
		String console = "";
		try {
			// （并非所有进程都能被检测到，所有非本用户的进程信息将不会显示，如果想看到所有信息，则必须切换到 root 用户）
			String[] commandList = {
					"/bin/sh",
					"-c",
					"netstat -anp|grep :"
							+ port
							+ "|grep -v 'grep'|awk '{if ($6==\"LISTEN\") print $7}'" };
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(p.getInputStream(),
					AgentConfig.getEncoding()));
			String tcpId = null;
			while ((console = in.readLine()) != null) {
				if (console.contains("/")) {
					tcpId = console.substring(0, console.indexOf("/"));
					break;
				}
			}
			log.info("Find tcpId = " + tcpId);
			return tcpId;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 根据CMD命令行（不区分大小写）查找PID
	 * 
	 * @param commandLine
	 * @return
	 */
	public static List<String> findPidByCommandAix(String commandLine) {
		List<String> result = new ArrayList<String>();
		// =====开始--Windows检查程序是否已经运行，避免重复打开应用程序窗口
		if (isAix()) {
			Process p = null;
			ProcessBuilder pb = null;
			BufferedReader in=null;
			try {
				//“-i”表示不区分大小写
				String[] commandList = {
						"/bin/sh",
						"-c",
						"ps -ef|grep -i '" + commandLine
								+ "'|grep -v 'grep'|awk '{print $2}'" };
				pb = new ProcessBuilder(commandList);
				pb.redirectErrorStream(true);
				p = pb.start();
				p.getOutputStream().close();
				String pid = "";
				in = new BufferedReader(new InputStreamReader(
						p.getInputStream(), AgentConfig.getEncoding()));
				/*
				 * 输出格式，例： [root@jmsbkapp1]#ps -ef|grep -i 'startWebLogic'|grep -v 'grep'|awk '{print $2}' 
				 * 5701820 
				 * 5898412 
				 * 7077986 
				 * 6685074
				 * 7471412 
				 * 7799040
				 */
				while ((pid = in.readLine()) != null) {
					try{
						Integer.parseInt(pid);
						result.add(pid);
					}catch(Exception e){
						result=null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null) {
					p.destroy();
				}
				if(in!=null){
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}

	/**
	 * 根据父PID查找 子PID
	 * 
	 * @param ppid
	 * @return
	 */
	public static List<String> findPidByPPIDAix(String ppid) {
		List<String> result = new ArrayList<String>();
		// =====开始--Windows检查程序是否已经运行，避免重复打开应用程序窗口
		if (isAix()) {
			Process p = null;
			ProcessBuilder pb = null;
			BufferedReader in=null;
			try {
				// 根据PPID获取包含本身和它的子进程列表
				String[] commandList = { "/bin/sh", "-c",
						"ps -fT " + ppid + "|awk 'NR!=1 {print $2}'" };
				pb = new ProcessBuilder(commandList);
				pb.redirectErrorStream(true);
				p = pb.start();
				String pid = "";
				in = new BufferedReader(new InputStreamReader(
						p.getInputStream(), AgentConfig.getEncoding()));
				/*
				 * 传入PPID=5701820，得到PPID(5701820)和他的子进程PID(7077986、6422994)
				 * [root@jmsbkapp1]#ps -fT 5701820|awk 'NR!=1 {print $2}'
				 * 5701820 7077986 6422994
				 */
				while ((pid = in.readLine()) != null) {
					result.add(pid);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (p != null) {
					p.destroy();
				}
				if(in!=null){
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}

	/**
	 * 利用grep weblogic 查找weblogic进程
	 * 
	 * @param commandLine
	 * @return
	 */
	public static List<String> findPidByCommandLinux(String commandLine) {
		List<String> result = new ArrayList<String>();
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in=null;
		try {
			//“-i”表示不区分大小写
			String[] cmd = {
					"/bin/sh",
					"-c",
					"ps -ef|grep -i '" + commandLine
							+ "'|grep -v 'grep'|awk '{print $2}'" };
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			p = pb.start();
			p.getOutputStream().close();
			String console = "";
			in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));

			while ((console = in.readLine()) != null) {
				result.add(console);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public static String KillProcessByPidAix(String pid) {
		// =====开始--Windows检查程序是否已经运行，避免重复打开应用程序窗口
		ProcessBuilder pb = null;
		Process p = null;
		BufferedReader in=null;
		try {
			String[] commandList = { "/bin/sh", "-c", "kill -9 " + pid };
			log.info("Kill Process " + pid);
			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			String console = null;
			while ((console = in.readLine()) != null) {
				console += console;
			}
			in.close();
			return console;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 根据PID终止Linux进程
	 * 
	 * @param pid
	 * @return
	 */
	public static String KillProcessByPidLinux(String pid) {

		// =====开始--Windows检查程序是否已经运行，避免重复打开应用程序窗口
		Process p = null;
		ProcessBuilder pb = null;
		BufferedReader in =null;
		try {
			String[] commandList = { "/bin/sh", "-c", "kill -9 " + pid };

			pb = new ProcessBuilder(commandList);
			pb.redirectErrorStream(true);
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			String console = null;
			while ((console = in.readLine()) != null) {
				console += console;
			}
			return console;
		} catch (Exception e) {
			return e.getMessage();
		}finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 用于调试输出命令
	 * 
	 * @param commandLine
	 */
	public static void debugShowCommand(List<String> commandLine) {
		// log.info("CommandLine=" + commandLine);
		// if (commandLine == null) {
		// return;
		// }

	}

	/**
	 * 获取本地IP集合
	 * 
	 * @return
	 */
	public static List<String> getLocalIpList() {
		List<String> ipList = null;
		try {
			ipList = new ArrayList<String>();
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface current = interfaces.nextElement();
				Enumeration<InetAddress> addresses = current.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr.isLoopbackAddress())
						continue;
					if (checkIp(addr.getHostAddress())) {
						ipList.add(addr.getHostAddress());
					}
				}
			}
			return ipList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取系统IP地址
	 * 
	 * @return
	 */
	public static String getIp() {
		try {
			if (isAix() || isWindows()) {
				InetAddress address = InetAddress.getLocalHost();
				return address.getHostAddress();
			} else {
				Enumeration<?> enumeration = NetworkInterface
						.getNetworkInterfaces();
				InetAddress ip = null;
				while (enumeration.hasMoreElements()) {
					NetworkInterface netInterface = (NetworkInterface) enumeration
							.nextElement();
					Enumeration<?> addresses = netInterface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						ip = (InetAddress) addresses.nextElement();
						if (ip != null && ip.isSiteLocalAddress()
								&& !ip.isLoopbackAddress()
								&& !ip.isMulticastAddress()
								&& ip instanceof Inet4Address) {
							if (checkIp(ip.getHostAddress())) {
								return ip.getHostAddress();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.info("获取系统IP地址出错,原因: " + e.getMessage());
		}
		return "127.0.0.1";
	}

	/**
	 * 判断IP地址的合法性，这里采用了正则表达式的方法来判断，合法返回true
	 * */
	public static boolean checkIp(String text) {
		if (text != null && !text.equals("")) {
			// 定义正则表达式
			String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
			// 判断ip地址是否与正则表达式匹配
			if (text.matches(regex)) {
				// 返回判断信息
				return true;
			} else {
				// 返回判断信息
				return false;
			}
		}
		return false;
	}

	/**
	 * 格式化文件路径
	 * 
	 * @param path
	 * @return
	 */
	public static String formatPath(String path) {
		File file = new File(path);
		return file.getPath();
	}

	/**
	 * 拼装字节数组
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static byte[] concatByte(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	/**
	 * 将整型数据INT,转换为byte[4]
	 * 
	 * @param i
	 * @return
	 */
	public static byte[] intToByteArray(int i) {
		byte[] result = new byte[4];
		result[0] = (byte) ((i >> 24) & 0xFF);
		result[1] = (byte) ((i >> 16) & 0xFF);
		result[2] = (byte) ((i >> 8) & 0xFF);
		result[3] = (byte) (i & 0xFF);
		return result;
	}

	/**
	 * 转换编码 GBK->ISO8859-1
	 * 
	 * @param fn
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String gbkToISO88591(String fn)
			throws UnsupportedEncodingException {
		return new String(fn.getBytes("GBK"), "ISO8859-1");
	}

	public static String isoToGbk(String fn)
			throws UnsupportedEncodingException {
		return new String(fn.getBytes("ISO8859-1"), "GBK");
	}
	/**
	 * 转换编码 UTF-8->ISO8859-1
	 * 
	 * @param fn
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String utfToIso(String str)
			throws UnsupportedEncodingException {
		return new String(str.getBytes("utf-8"), "iso-8859-1");
	}

	/**
	 * 转换编码 ISO8859-1 ->UTF-8
	 * 
	 * @param fn
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String isoToUtf(String str)
			throws UnsupportedEncodingException {
		return new String(str.getBytes("iso-8859-1"), "utf-8");
	}

	/**
	 * 判断是否为Windows操作系统
	 * 
	 * @return
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("window") ? true
				: false;
	}

	/**
	 * 判断是否为Aix操作系统
	 * 
	 * @return
	 */
	public static boolean isAix() {
		return System.getProperty("os.name").toLowerCase().contains("aix") ? true
				: false;
	}

	/**
	 * 判断是否为Linux操作系统
	 * 
	 * @return
	 */
	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().contains("linux") ? true
				: false;
	}

	/**
	 * 获取操作系统版本
	 * 
	 * @return
	 */
	public static String getSystemVersion() {
		return System.getProperty("os.name") + "_"
				+ System.getProperty("os.version") + "_"
				+ System.getProperty("os.arch");
	}

	/**
	 * 按照文件名称排序
	 * 
	 * @param fliePath
	 * @return
	 */
	public static List<File> filesOrderByName(String fliePath) {
		List<File> fs = Arrays.asList(new File(fliePath).listFiles());
		Collections.sort(fs, new Comparator<File>() {
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && o2.isFile())
					return -1;
				if (o1.isFile() && o2.isDirectory())
					return 1;
				return o1.getName().compareTo(o2.getName());
			}
		});
		return fs;
	}

	/**
	 * 返回对象的字符串
	 * 
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		if (obj != null) {
			return obj.toString();
		} else {
			return "";
		}
	}

	/**
	 * 判断对象是否为空
	 * 
	 * @param obj
	 * @return
	 */
	public static boolean isNull(Object obj) {
		if (obj == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 发送处理信息
	 * 
	 * @param socketChannel
	 * @param code
	 *            状态码
	 * @param message
	 *            返回的信息
	 * @return
	 * @throws IOException
	 */
	public static void sendStatusCodeMessage(SocketChannel socketChannel,
			int code, String message) throws IOException {
		ByteBuffer buf = null;
		// 状态码,状态信息长度，状态信息长度
		buf = ByteBuffer.allocate(4 + 4 + Tool.utfToIso(message).getBytes(
				"ISO-8859-1").length);
		// ----------------
		buf.putInt(code);// 状态码
		buf.putInt(Tool.utfToIso(message).getBytes("ISO-8859-1").length);// 状态信息长度
		buf.put(Tool.utfToIso(message).getBytes("ISO-8859-1"));// 状态信息内容
		// ---------------------
		buf.flip();
		socketChannel.write(buf);
		buf.clear();
	}
	
	/**
	 * 删除文件
	 * @param path 文件路径(例: C:\file\f.txt)
	 * @return 
	 */
	public static ResultModel deleteFile(String path){
		final ResultModel result=new ResultModel();
		ProcessBuilder pb=null;
		Process p=null;
		BufferedReader in =null;
		
		try{
			File deleteFile=new File(path);
			//不能使用File.delete()方法，涉及到操作系统权限问题。
			if(!deleteFile.exists()){
				result.setCodeMsg(Status.FAILED, "["+deleteFile.getPath()+"] 文件不存在，删除操作已停止!");
				return result;
			}
			boolean canDelete=false;
			List<String> permisionPath=AgentConfig.getPermissionDeletePath();
			if(permisionPath==null){
				result.setCodeMsg(Status.FAILED, "权限不足，拒绝执行删除操作!");
				return result;
			}else{
				//检查需删除文件是否在允许访问的目录之内
				for(String ph:permisionPath){
					File phFile=new File(ph);
					if(deleteFile.getPath().contains(phFile.getPath())){
						canDelete=true;
						break;
					}
				}
			}
			//如果为False表示，需要删除的文件不在允许删除的目录之内。
			if(!canDelete){
				result.setCodeMsg(Status.FAILED, "权限不足，拒绝执行删除操作!");
				return result;
			}
			//通过所有检验后，执行删除操作
			if(Tool.isAix() || Tool.isLinux()){
				String[] command={"/bin/sh","-c","rm -rf \""+deleteFile.getPath()+"\""};
				pb=new ProcessBuilder(command);
				pb.redirectErrorStream(true);
				p = pb.start();
			}else if(Tool.isWindows()){
				List<String> command=new ArrayList<String>();
				if(deleteFile.isDirectory()){
					//删除文件夹
					command.add("cmd.exe");
					command.add("/c");
					command.add("rd");
					command.add("/s");
					command.add("/q");				
					command.add("\""+deleteFile.getPath()+"\"");
				}else{
					//删除文件
					command.add("cmd.exe");
					command.add("/c");
					command.add("del");
					command.add("/a");
					command.add("/f");
					command.add("/q");
					command.add("\""+deleteFile.getPath()+"\"");
				}
				pb=new ProcessBuilder(command);
				pb.redirectErrorStream(true);
				p = pb.start();
			}else{
				result.setCodeMsg(Status.FAILED, "不支持的操作系统: "+Tool.getSystemVersion()+". 删除操作已停止!");
				return result; 
			}
			in = new BufferedReader(new InputStreamReader(
					p.getInputStream(), AgentConfig.getEncoding()));
			String console = null;
			String errInfo="";
			while ((console = in.readLine()) != null) {
				log.info(console);
				errInfo += console;
			}
			//等待进程结束
			int code=p.waitFor();
			if(code==0){
				result.setCodeMsg(Status.SUCCEED, "文件删除成功");
			}else{
				result.setCodeMsg(Status.FAILED, "["+deleteFile.getPath()+"] 文件删除失败，发生异常："+errInfo);
			}
		}catch(Exception e){
			e.printStackTrace();
			result.setCodeMsg(Status.FAILED,"报错信息->"+e.getMessage());
		}finally {
			if (p != null) {
				p.destroy();
			}
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
