package code.ponfee.commons.jce;

import java.nio.charset.Charset;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import code.ponfee.commons.io.Files;
import code.ponfee.commons.jce.crypto.Algorithm;
import code.ponfee.commons.jce.crypto.Mode;
import code.ponfee.commons.jce.crypto.Padding;
import code.ponfee.commons.jce.crypto.SymmetricCryptor;
import code.ponfee.commons.jce.crypto.SymmetricCryptorBuilder;
import code.ponfee.commons.jce.security.RSACryptor;
import code.ponfee.commons.jce.security.RSAPrivateKeys;
import code.ponfee.commons.jce.security.RSAPublicKeys;
import code.ponfee.commons.util.MavenProjects;

/**
 * 加解密服务提供
 * @author fupf
 */
public abstract class CryptoProvider {

    /**
     * 数据加密
     * @param original  原文
     * @return
     */
    public abstract byte[] encrypt(byte[] original);

    /**
     * 数据解密
     * @param encrypted  密文
     * @return
     */
    public abstract byte[] decrypt(byte[] encrypted);

    /**
     * sign the data
     * @param data
     * @return signature
     */
    public byte[] sign(byte[] data) {
        throw new UnsupportedOperationException("canot support sign.");
    }

    /**
     * verify the data signature
     * @param data
     * @param signed
     * @return true|false
     */
    public boolean verify(byte[] data, byte[] signed) {
        throw new UnsupportedOperationException("canot support verify signature.");
    }

    /**
     * 字符串数据加密
     * @param plaintext  明文
     * @return
     */
    public final String encrypt(String plaintext) {
        return encrypt(plaintext, Charset.forName(Files.SYSTEM_CHARSET));
    }

    /**
     * 字符串数据加密
     * @param plaintext 明文
     * @param charset   字符串编码
     * @return
     */
    public final String encrypt(String plaintext, Charset charset) {
        if (plaintext == null) {
            return null;
        }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                   this.encrypt(plaintext.getBytes(charset))
               );
    }

    /**
     * 数据解密
     * @param ciphertext  密文数据的base64编码
     * @return
     */
    public final String decrypt(String ciphertext) {
        return decrypt(ciphertext, Charset.forName(Files.SYSTEM_CHARSET));
    }

    /**
     * 数据解密
     * @param ciphertext  密文数据的base64编码
     * @param charset     明文字符串编码
     * @return
     */
    public final String decrypt(String ciphertext, Charset charset) {
        if (ciphertext == null) {
            return null;
        }

        return new String(decrypt(Base64.getUrlDecoder().decode(ciphertext)), charset);
    }

    /**
     * sign of data
     * @param data
     * @return
     */
    public final String sign(String data) {
        return sign(data, Files.SYSTEM_CHARSET);
    }

    /**
     * data
     * @param data
     * @param charset
     * @return
     */
    public final String sign(String data, String charset) {
        if (StringUtils.isEmpty(data)) {
            return null;
        }
        byte[] signed = sign(data.getBytes(Charset.forName(charset)));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
    }

    /**
     * verify the data
     * @param data
     * @param signed
     * @return
     */
    public final boolean verify(String data, String signed) {
        return verify(data, Files.SYSTEM_CHARSET, signed);
    }

    /**
     * verify the data
     * @param data
     * @param charset
     * @param signed
     * @return
     */
    public final boolean verify(String data, String charset, String signed) {
        return verify(data.getBytes(Charset.forName(charset)), 
                      Base64.getUrlDecoder().decode(signed));
    }

    /**
     * 对称密钥组件
     * @param symmetricCryptor {@link SymmetricCryptor}
     * @return
     */
    public static CryptoProvider symmetricKeyProvider(final SymmetricCryptor key) {
        return new CryptoProvider() {
            private final SymmetricCryptor symmetricKey = key;

            @Override
            public byte[] encrypt(byte[] original) {
                Preconditions.checkArgument(original != null);
                return symmetricKey.encrypt(original);
            }

            @Override
            public byte[] decrypt(byte[] encrypted) {
                Preconditions.checkArgument(encrypted != null);
                return symmetricKey.decrypt(encrypted);
            }
        };
    }

    /**
     * rsa private key密钥组件
     * @param pkcs8PrivateKey  the string of pkcs8 private key format
     * @return
     */
    public static CryptoProvider privateKeyProvider(final String pkcs8PrivateKey) {
        return new CryptoProvider() {
            private final RSAPrivateKey priKey = RSAPrivateKeys.fromPkcs8(pkcs8PrivateKey);
            private final RSAPublicKey  pubKey = RSAPrivateKeys.extractPublicKey(priKey);

            @Override
            public byte[] encrypt(byte[] original) {
                Preconditions.checkArgument(original != null);
                return RSACryptor.encrypt(original, pubKey); // 公钥加密
            }

            @Override
            public byte[] decrypt(byte[] encrypted) {
                Preconditions.checkArgument(encrypted != null);
                return RSACryptor.decrypt(encrypted, priKey); // 私钥解密
            }

            @Override
            public byte[] sign(byte[] data) {
                return RSACryptor.signSha1(data, priKey);
            }

            @Override
            public boolean verify(byte[] data, byte[] signed) {
                return RSACryptor.verifySha1(data, pubKey, signed);
            }
        };
    }

    /**
     * rsa public key密钥组件
     * @param pkcs8PublicKey  the string of pkcs8 public key format
     * @return
     */
    public static CryptoProvider publicKeyProvider(final String pkcs8PublicKey) {
        return new CryptoProvider() {
            private final RSAPublicKey pubKey = RSAPublicKeys.fromPkcs8(pkcs8PublicKey);

            @Override
            public byte[] encrypt(byte[] original) {
                Preconditions.checkArgument(original != null);
                return RSACryptor.encrypt(original, pubKey); // 公钥加密
            }

            @Override
            public byte[] decrypt(byte[] encrypted) {
                throw new UnsupportedOperationException("cannot support decrypt.");
            }

            @Override
            public boolean verify(byte[] data, byte[] signed) {
                return RSACryptor.verifySha1(data, pubKey, signed);
            }
        };
    }

    public static void main(String[] args) {
        System.out.println("\n============================RSA crypt==========================");
        CryptoProvider rsa = privateKeyProvider("MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEA9pU2mWa+yJwXF1VQb3WL5uk06Rc2jARYPlcV0JK0x4fMXboR9rpMlpJ9cr4B1wbJdBEa8H+kSgbJROFKsmkhFQIDAQABAkAcGiNP1krV+BwVl66EFWRtW5ShH/kiefhImoos7BtYReN5WZyYyxFCAf2yjMJigq2GFm8qdkQK+c+E7Q3lY6zdAiEA/wVfy+wGQcFh3gdFKhaQ12fBYMCtywxZ3Edss0EmxBMCIQD3h4vfENmbIMH+PX5dAPbRfrBFcx77/MxFORMESN0bNwIgL5kJMD51TICTi6U/u4NKtWmgJjbQOT2s5/hMyYg3fBECIEqRc+qUKenYuXg80Dd2VeSQlMunPZtN8b+czQTKaomLAiEA02qUv/p1dT/jc2BDtp9bl8jDiWFg5FNFcH6bBDlwgts=");
        String str = Files.toString(MavenProjects.getMainJavaFile(CryptoProvider.class)).replaceAll("\r|\n|\\s+", "");
        String data = rsa.encrypt(str);
        System.out.println("加密后：" + data);
        System.out.println("解密后：" + rsa.decrypt(data));

        System.out.println("\n============================RSA sign==========================");
        String signed = rsa.sign(str);
        System.out.println("签名："+signed);
        System.out.println("验签："+rsa.verify(str, signed));

        System.out.println("\n============================AES crypt==========================");
        CryptoProvider aes = symmetricKeyProvider(SymmetricCryptorBuilder.newBuilder(Algorithm.AES)
                                                                         .key("z]_5Fi!X$ed4OY8j".getBytes())
                                                                         .mode(Mode.CBC).ivParameter("SVE<r[)qK`n%zQ'o".getBytes())
                                                                         .padding(Padding.PKCS7Padding).provider(Providers.BC)
                                                                         .build());
        data = aes.encrypt(str);
        System.out.println("加密后：" + data);
        System.out.println("解密后：" + aes.decrypt(data));
    }

}
