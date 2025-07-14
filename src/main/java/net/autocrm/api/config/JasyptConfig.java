package net.autocrm.api.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.autocrm.common.ConfigKeys;

@Configuration
public class JasyptConfig {

	@Value( ConfigKeys.JASYPT_ENC_PW )
	private String key;

	@Bean(name = "jasyptStringEncryptor")
    StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key); //패스워드 (비밀키라 노출되면 안 됨)
        config.setAlgorithm("PBEWithHMACSHA512AndAES_256"); //사용하는 알고리즘
        config.setKeyObtentionIterations("1000"); //암호화 키를 얻기 위해 반복해야하는 해시 횟수. 클 수록 암호화는 오래 걸리긴 하지만 보안 강도가 높아짐
        config.setPoolSize("1");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64"); //암호화 이후 어떤 형태로 값을 받을지 설정
        encryptor.setConfig(config);

        return encryptor;
    }
}
