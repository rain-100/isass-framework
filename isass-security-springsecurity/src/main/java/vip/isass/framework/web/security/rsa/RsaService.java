package vip.isass.framework.web.security.rsa;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.springframework.stereotype.Service;
import vip.isass.framework.web.security.config.SecurityProperties;

import jakarta.annotation.PostConstruct;

@Service
public class RsaService {

    // 共用的 id，使用共用的密码对
    private static final String DEFAULT_ID = "PUBLIC";

    private final SecurityProperties securityProperties;

    private RsaKey rsaKey;

    public RsaService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void initialize() {
        SecurityProperties.Rsa properties = securityProperties.getRsa();
        Assert.notBlank(properties.getPrivateKey(), "security.rsa.private-key 必填");
        Assert.notBlank(properties.getPublicKey(), "security.rsa.public-key 必填");
        rsaKey = RsaKey.builder()
                .rsa(SecureUtil.rsa(properties.getPrivateKey(), properties.getPublicKey()))
                .privateKeyStr(properties.getPrivateKey())
                .publicKeyStr(properties.getPublicKey())
                .build();
    }

    /** RSA keys are configured per process; dynamic key ids are no longer supported. */
    private RsaKey loadKey(String id) {
        Assert.isTrue(StrUtil.isBlank(id) || DEFAULT_ID.equals(id), "仅支持默认 RSA 密钥");
        return rsaKey;
    }

    public String getBase64PublicKey() {
        return getBase64PublicKey(null);
    }

    public String getBase64PublicKey(String id) {
        return loadKey(id).getPublicKeyStr();
    }

    public String encrypt(String plainText) {
        return encrypt(null, plainText);
    }

    public String encrypt(String id, String plainText) {
        Assert.notBlank(plainText, "plainText 必填");
        return loadKey(id).getRsa().encryptBase64(plainText, KeyType.PublicKey);
    }

    public String decrypt(String cipherText) {
        return decrypt(null, cipherText);
    }

    public String decrypt(String id, String cipherText) {
        Assert.notBlank(cipherText, "cipherText 必填");
        try {
            return loadKey(id).getRsa().decryptStr(cipherText, KeyType.PrivateKey);
        } catch (Exception e) {
            throw new RuntimeException("无法解密密文，密文格式错误或秘钥不匹配");
        }
    }

}
