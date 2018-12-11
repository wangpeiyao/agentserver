package cn.com.hnisi.type;

/**
 * 系统平台
 * @author FengGeGe
 *
 */
public enum PlatformType {
	/**
	 * AIX系统
	 */
	AIX(1),
	/**
	 * Linux平台
	 */
	LINUX(2),
	/**
	 * Windows平台
	 */
	WINDOWS(3);
	
	private int value;

	PlatformType(int value) {

	}

	public int getValue() {
		// TODO 自动生成的方法存根
		return value;
	}
}
