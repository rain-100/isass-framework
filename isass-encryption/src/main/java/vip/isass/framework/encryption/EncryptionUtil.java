// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.encryption;

import org.jasypt.util.text.BasicTextEncryptor;

/**
 * @author Rain
 */
public class EncryptionUtil {

    public static void main(String[] args) {
        String str = "111111";
        String encryptStr = encrypt(str);
        String decryptStr = decrypt(encryptStr);
        System.out.println("原文：");
        System.out.println(str);
        System.out.println("密文：");
        System.out.println(encryptStr);
        System.out.println("解密：");
        System.out.println(decryptStr);
    }

    public static String encrypt(String word) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword("EbfYkXoaxZTKJ7");
        return textEncryptor.encrypt(word);
    }

    public static String decrypt(String word) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword("EbfYkXoaxZTKJ7");
        return textEncryptor.decrypt(word);
    }

}
