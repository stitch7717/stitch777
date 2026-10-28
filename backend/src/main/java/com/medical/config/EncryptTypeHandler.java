package com.medical.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 * 身份证字段加解密 TypeHandler：AES-256-GCM。
 *
 * <p>选择 GCM 的原因：带认证标签，密文被篡改时解密直接失败，而不是返回一串乱码；
 * 随机 IV 保证同一明文每次密文不同，避免 ECB 的确定性泄露。
 * 存储格式：{@code ENC:Base64(12字节IV + 密文 + 16字节认证标签)}。</p>
 *
 * <p>MyBatis 通过反射创建 TypeHandler，无法注入 Spring Bean，
 * 因此密钥由 {@link CryptoKeyInitializer} 在启动时写入静态字段。</p>
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec key;

    /** 启动时由 CryptoKeyInitializer 调用；密钥为 Base64 编码的 32 字节。 */
    public static void init(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "app.crypto.aes-key 未配置。本地开发请在 application-local.yml 中配置，"
                            + "部署时通过 AES_KEY 环境变量注入");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.crypto.aes-key 必须是 Base64 编码", e);
        }
        if (raw.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "app.crypto.aes-key 必须是 32 字节（AES-256），当前为 " + raw.length + " 字节");
        }
        key = new SecretKeySpec(raw, "AES");
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        SecretKeySpec secretKey = key;
        if (secretKey == null) {
            throw new IllegalStateException("AES 密钥尚未初始化");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, out, IV_LENGTH, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("身份证字段加密失败", e);
        }
    }

    /**
     * 兼容历史明文：不带 ENC: 前缀的值原样返回。
     * 带前缀但解密失败说明密钥不匹配或数据损坏，直接抛出而不是静默返回密文。
     */
    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        SecretKeySpec secretKey = key;
        if (secretKey == null) {
            throw new IllegalStateException("AES 密钥尚未初始化");
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (all.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] body = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, body, 0, body.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "身份证字段解密失败，请检查 app.crypto.aes-key 是否与写入时一致", e);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }
}
