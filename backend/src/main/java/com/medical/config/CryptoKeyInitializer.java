package com.medical.config;

import com.medical.common.crypto.IdCardHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 把配置中的密钥交给 MyBatis TypeHandler / 哈希工具（它们由框架反射创建，无法依赖注入）。
 * 配置缺失时在启动阶段直接失败，避免运行到写入身份证才报错。
 */
@Component
public class CryptoKeyInitializer {

    public CryptoKeyInitializer(@Value("${app.crypto.aes-key:}") String aesKey,
                                @Value("${app.crypto.id-card-pepper:}") String idCardPepper) {
        EncryptTypeHandler.init(aesKey);
        IdCardHasher.init(idCardPepper);
    }
}
