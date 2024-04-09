package cn.likh.core.exception;

/**
 * 组件定义异常
 * @author shaolin.li
 */
public class MQAsyncDefinitionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 异常信息
	 */
	private String message;

	public MQAsyncDefinitionException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
