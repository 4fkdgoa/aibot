package net.autocrm.api.exception;

import java.util.Map;

public class GenericRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 930023499584477271L;

	protected Throwable rootCause;
	Map<String, Object> rtn;

	public GenericRuntimeException(Throwable ex) {
		super(ex.getMessage());
		this.rootCause = ex;
	}

	public GenericRuntimeException(Throwable ex, String msg) {
		super(msg);
		this.rootCause = ex;
	}

	public GenericRuntimeException(Throwable ex, Map<String, Object> rtn) {
		this(ex);
		this.rtn = rtn;
	}

	public GenericRuntimeException(Throwable ex, String msg, Map<String, Object> rtn) {
		this(ex, msg);
		this.rtn = rtn;
	}

	public GenericRuntimeException(String s) {
		super(s);
	}

	public GenericRuntimeException(String s, Map<String, Object> rtn) {
		this(s);
		this.rtn = rtn;
	}

	public void setRootCause(Throwable ex) {
		this.rootCause = ex;
	}

	public Throwable getRootCause() {
		return rootCause;
	}

	public Map<String, Object> getRtn() {
		if ( this.rtn != null && !this.rtn.containsKey("RST_CD") ) {
			this.rtn.put("IF_RST_CD", "99");
			this.rtn.put("IF_RST_MSG", "FAIL");
		}
		return this.rtn;
	}
}
