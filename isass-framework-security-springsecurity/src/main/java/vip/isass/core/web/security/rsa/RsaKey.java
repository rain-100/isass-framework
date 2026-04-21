package vip.isass.core.web.security.rsa;

import cn.hutool.crypto.asymmetric.RSA;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RsaKey {

    private RSA rsa;

    private String publicKeyStr;

    private String privateKeyStr;

}
