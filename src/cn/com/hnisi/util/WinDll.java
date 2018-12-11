package cn.com.hnisi.util;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Windows工具类，根据程序启动的命令行，获取其对应的PID
 * @author 温志锋
 *
 */
public class WinDll {
	//调用：User32.dll
	public interface User32 extends StdCallLibrary {
		User32 INSTANCE = (User32) Native.loadLibrary("User32", User32.class);// 加载系统User32

		int GetWindowTextA(HWND arg0, Pointer arg1, int arg2);

		HWND GetDesktopWindow();

		void EnumWindows(WNDENUMPROC arg0, Pointer arg1);

		void EnumChildWindows(HWND arg0, WNDENUMPROC arg1, Pointer arg2);

		int GetWindowThreadProcessId(HWND arg0, IntByReference arg1);

		boolean IsWindowVisible(HWND arg0);

	}
	//调用：WinCmd.dll
	public interface WinCmd extends StdCallLibrary {
		
		WinCmd INSTANCE = (WinCmd) Native.loadLibrary("WinCmd", WinCmd.class);// 加载系统User32
		//根据进程ID获取命令行
		WString GetPebCommandLine(int pid);
		//根据子进程获取父进程
		int GetParentProcessID(int pid);

	}

	

	/**
	 * 获取进程的父PID
	 * @param pid
	 * @return
	 */
	public static int getParentProcessId(int pid){
		WinCmd win = WinCmd.INSTANCE;
		return win.GetParentProcessID(pid);
	}
	/**
	 * 根据内容查找是否存在于进程程序中(查找前会替换掉所有空格)
	 * @param cmdLine
	 * @return WinDll.ProcessInfo
	 */
	public static WinDll.ProcessInfo GetCmdLine(String commandLine) {
		final List<WinDll.ProcessInfo> processInfos = new ArrayList<WinDll.ProcessInfo>();
		final WinCmd win = WinCmd.INSTANCE;
		HWND hwnd = User32.INSTANCE.GetDesktopWindow();
		WinUser.WNDENUMPROC wn = new WinUser.WNDENUMPROC() {
			public boolean callback(HWND hwnd, Pointer arg1) {
				// 设置编码防止乱码
			 	System.setProperty("jna.encoding", "GBK");
				// 根据句柄获取窗口标题
				 Pointer textPointer = new Memory(500);
				 User32.INSTANCE.GetWindowTextA(hwnd, textPointer, 500);
				 String caption = Native.toString(textPointer.getByteArray(0,
				 500));
	
				// 根据句柄获取PID
				IntByReference pidRef = new IntByReference();
				User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
				
				int pid=pidRef.getValue();
				if(pid>0){
					//获取父进程
					int parentProcessId=win.GetParentProcessID(pid);
					
					//根据PID获取CommandLine
					String commandLine = "";
					WString cmd = win.GetPebCommandLine(pid);
					if (cmd != null) {
						commandLine = cmd.toString();
					}
					
					//保存结果
					WinDll.ProcessInfo process=new WinDll().new ProcessInfo();
					process.setCaption(caption);
					process.setParentProcessId(parentProcessId);
					process.setProcessId(pid);
					process.setCommandLine(commandLine);
					
					processInfos.add(process);
				}
				return true;
			}
		};
		processInfos.clear();
		//杖举桌面所有窗口程序
		User32.INSTANCE.EnumChildWindows(hwnd, wn, null);
		//根据传入的条件，在List结果中匹配
		for(WinDll.ProcessInfo process:processInfos){
			//只需要查找cmd进程
			if(process.getCommandLine().toLowerCase().contains("\\cmd.exe")){
				//去掉字符串中的空格字符，再进行查找
				if(process.getCommandLine().replace(" ", "").toLowerCase().contains(commandLine.replace(" ", "").toLowerCase())){	
					processInfos.clear();
					return process;
				}
			}
		}
		return null;
	}
	
	/**
	 * 保存进程信息
	 * @author FengGeGe
	 *
	 */
	public class ProcessInfo{
		public ProcessInfo(){}
		private String caption="";//标题
		private int parentProcessId=-1;//父进程PID
		private int processId=-1;//进程PID
		private String commandLine="";//命令行
		public String getCaption() {
			return caption;
		}
		public void setCaption(String caption) {
			this.caption = caption;
		}
		public int getProcessId() {
			return processId;
		}
		public void setProcessId(int processId) {
			this.processId = processId;
		}
		public String getCommandLine() {
			return commandLine;
		}
		public void setCommandLine(String commandLine) {
			this.commandLine = commandLine;
		}
		public int getParentProcessId() {
			return parentProcessId;
		}
		public void setParentProcessId(int parentProcessId) {
			this.parentProcessId = parentProcessId;
		}
	}
	
}
