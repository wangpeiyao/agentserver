package cn.com.hnisi.type;

/**
 * 状态
 * @author WenZhiFeng
 * 2018年1月31日
 */
public enum Status {
	/**
	 * 0-正常、成功
	 */
	SUCCEED(0),
	/**
	 * -1-处理、异常
	 */
	FAILED(-1),
	/**
	 * 99-发生异常
	 */
	EXCEPTION(99);
	
	private int value;

	Status(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
