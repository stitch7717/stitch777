package com.medical.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 身份证号 AES 加密存储 TypeHandler。
 * 写入永远加密；读取时兼容历史明文（解密失败原样返回）。
 * 应用到 sys_user.id_card 类敏感列（biz_patient.id_card）。
 */
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static final String KEY = "CHANGE_ME";
    private static final String ALGO = "AES";

    private static SecretKeySpec keySpec() {
        return new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), ALGO);
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec());
            return "ENC:" + Base64.getEncoder().encodeToString(
                    cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith("ENC:")) {
            // 历史明文兼容
            return stored;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keySpec());
            return new String(cipher.doFinal(
                    Base64.getDecoder().decode(stored.substring(4))), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return stored;
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
