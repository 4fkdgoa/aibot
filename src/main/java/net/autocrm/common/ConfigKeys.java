package net.autocrm.common;

public interface ConfigKeys {

	String CORS_ALLOWED_ORIGIN = "${cors.allow.origin:*}";
	String CORS_ALLOWED_HEADERS = "${cors.allow.headers:Origin, X-Requested-With, Content-Type, Accept, X-Auth-Token, accessToken, Cache-Control, Pragma, Expires, Surrogate-Control}";
	
	String JASYPT_ENC_PW = "${jasypt.encryptor.password:jasypt_key@crm}";
	
	String JDBC_DRIVER = "${jdbc.driver:com.microsoft.sqlserver.jdbc.SQLServerDriver}";
	String JDBC_URL = "${jdbc.url}";
	String JDBC_USER = "${jdbc.user}";
	String JDBC_PASS = "${jdbc.pass}";

	String S3_FOLDER_AUTO = "${aws.s3.folder.auto}";
	String S3_FOLDER_STOCK = "${aws.s3.folder.stock}";

	String URL_SHORT_LENGTH = "${url.short.length:6}";
}
