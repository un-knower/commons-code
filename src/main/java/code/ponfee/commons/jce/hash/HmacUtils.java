package code.ponfee.commons.jce.hash;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import code.ponfee.commons.io.Files;
import code.ponfee.commons.jce.HmacAlgorithm;
import code.ponfee.commons.util.MavenProjects;
import code.ponfee.commons.util.SecureRandoms;

/**
 * HMAC的一个典型应用是用在“质询/响应”（Challenge/Response）身份认证中
 * Hmac算法封装，计算“text”的HMAC：
 * <code>
 *   if (length(K) > blocksize) {
 *       K = H(K) // keys longer than blocksize are shortened
 *   } else if (length(key) < blocksize) {
 *       K += [0x00 * (blocksize - length(K))] // keys shorter than blocksize are zero-padded 
 *   }
 *   opad = [0x5c * B] XOR K
 *   ipad = [0x36 * B] XOR K
 *   hash = H(opad + H(ipad + text))
 * </code>
 * 其中：H为散列函数，K为密钥，text为数据，
 *     B表示数据块的字长（the blocksize is that of the underlying hash function）
 * @author fupf
 */
public final class HmacUtils {

    private static final int BUFF_SIZE = 4096;

    public static byte[] sha1(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA1.name());
    }

    public static byte[] sha1(byte[] key, InputStream data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA1.name());
    }

    public static String sha1Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(sha1(key, data));
    }

    public static String sha1Hex(byte[] key, InputStream data) {
        return Hex.encodeHexString(sha1(key, data));
    }

    public static byte[] md5(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacMD5.name());
    }

    public static String md5Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(md5(key, data));
    }

    public static byte[] sha224(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA224.name());
    }

    public static String sha224Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(sha224(key, data));
    }

    public static byte[] sha256(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA256.name());
    }

    public static String sha256Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(sha256(key, data));
    }

    public static byte[] sha384(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA384.name());
    }

    public static String sha384Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(sha384(key, data));
    }

    public static byte[] sha512(byte[] key, byte[] data) {
        return crypt(key, data, HmacAlgorithm.HmacSHA512.name());
    }

    public static String sha512Hex(byte[] key, byte[] data) {
        return Hex.encodeHexString(sha512(key, data));
    }

    public static Mac getInitializedMac(String algorithm, byte[] key) {
        if (key == null) {
            throw new IllegalArgumentException("Null key");
        }

        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
            return mac;
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unknown algorithm:" + algorithm);
        } catch (final InvalidKeyException e) {
            throw new IllegalArgumentException("invalid key: " + Hex.encodeHexString(key));
        }
    }

    // ------------------------private methods-------------------------
    private static byte[] crypt(byte[] key, byte[] data, String algName) {
        return getInitializedMac(algName, key).doFinal(data);
    }

    private static byte[] crypt(byte[] key, InputStream input, String algName) {
        try {
            Mac mac = getInitializedMac(algName, key);
            byte[] buffer = new byte[BUFF_SIZE];
            for (int len; (len = input.read(buffer)) != Files.EOF;) {
                mac.update(buffer, 0, len);
            }
            return mac.doFinal();
        } catch (IOException e) {
            throw new IllegalArgumentException("read data error:" + e);
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        byte[] key = SecureRandoms.nextBytes(16);
        System.out.println(sha1Hex(key, new FileInputStream(MavenProjects.getMainJavaFile(HmacUtils.class))));
        System.out.println(sha1Hex(key, new FileInputStream(MavenProjects.getMainJavaFile(HmacUtils.class))));
    }
}
