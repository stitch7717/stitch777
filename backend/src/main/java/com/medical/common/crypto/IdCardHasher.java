package com.medical.common.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 身份证检索列哈希：HMAC-SHA256(pepper, 规范化身份证号)。
 *
 * <p>加密列无法直接用于等值查询（GCM 每次密文不同），因此额外存一列确定性哈希。
 * 使用 HMAC 而不是裸 SHA-256：哈希值泄露后，攻击者无法用彩虹表反推身份证号。</p>
 */
public final class IdCardHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static volatile byte[] pepper;

    private IdCardHasher() {
    }

    /** 启动时由 CryptoKeyInitializer 调用。 */
    public static void init(String pepperText) {
        if (pepperText == null || pepperText.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException(
                    "app.crypto.id-card-pepper 未配置或长度不足 16 字节。"
                            + "本地开发请在 application-local.yml 中配置，部署时通过 ID_CARD_PEPPER 注入");
        }
        pepper = pepperText.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @return 64 位小写十六进制；身份证为空时返回 null
     */
    public static String hash(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return null;
        }
        byte[] secret = pepper;
        if (secret == null) {
            throw new IllegalStateException("身份证哈希密钥尚未初始化");
        }
        try {
            String normalized = idCard.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("身份证哈希计算失败", e);
        }
    }
}
