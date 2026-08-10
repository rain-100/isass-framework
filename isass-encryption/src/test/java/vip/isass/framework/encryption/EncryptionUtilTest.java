// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.encryption;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilTest {

    @Test
    void encryptDecryptRoundTrip() {
        String original = "111111";
        String encrypted = EncryptionUtil.encrypt(original);
        String decrypted = EncryptionUtil.decrypt(encrypted);
        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encryptProducesDifferentOutputEachTime() {
        String original = "test-password";
        String encrypted1 = EncryptionUtil.encrypt(original);
        String encrypted2 = EncryptionUtil.encrypt(original);
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(EncryptionUtil.decrypt(encrypted1)).isEqualTo(original);
        assertThat(EncryptionUtil.decrypt(encrypted2)).isEqualTo(original);
    }

    @Test
    void decryptOriginalInput() {
        String original = "my-secret-key";
        String encrypted = EncryptionUtil.encrypt(original);
        String decrypted = EncryptionUtil.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }
}
