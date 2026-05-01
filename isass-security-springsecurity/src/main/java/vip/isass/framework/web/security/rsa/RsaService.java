package vip.isass.framework.web.security.rsa;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RsaService {

    private static final String RSA_REDIS_KEY = "auth:rsa:{id}";

    // 共用的 id，使用共用的密码对
    private static final String DEFAULT_ID = "PUBLIC";

    @Resource
    private RedisTemplate<String, Map<String, String>> redisTemplate;

    private Cache<String, RsaKey> rsaCache = CacheUtil.newLRUCache(50, TimeUnit.DAYS.toMillis(1));

    /**
     * 根据 id 加载秘钥对象，不存在则创建一个并缓存
     */
    private RsaKey loadKey(String id) {
        final String rsaKeyId = StrUtil.isBlank(id) ? DEFAULT_ID : id;
        return rsaCache.get(rsaKeyId, () -> {
            String key = RSA_REDIS_KEY.replace("{id}", rsaKeyId);
            Map<String, String> rsaKeyMap = redisTemplate.opsForValue().get(key);
            RsaKey rsaKey;
            if (MapUtil.isEmpty(rsaKeyMap)) {
                RSA rsa = SecureUtil.rsa();
                rsaKey = RsaKey.builder()
                    .rsa(rsa)
                    .privateKeyStr(rsa.getPrivateKeyBase64())
                    .publicKeyStr(rsa.getPublicKeyBase64())
                    .build();
                redisTemplate.opsForValue().set(
                    key,
                    MapUtil.<String, String>builder()
                        .put("privateKeyStr", rsaKey.getPrivateKeyStr())
                        .put("publicKeyStr", rsaKey.getPublicKeyStr())
                        .build());
            } else {
                String privateKeyStr = MapUtil.getStr(rsaKeyMap, "privateKeyStr");
                String publicKeyStr = MapUtil.getStr(rsaKeyMap, "publicKeyStr");
                rsaKey = RsaKey.builder()
                    .rsa(SecureUtil.rsa(privateKeyStr, publicKeyStr))
                    .privateKeyStr(privateKeyStr)
                    .publicKeyStr(publicKeyStr)
                    .build();
            }
            return rsaKey;
        });
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
