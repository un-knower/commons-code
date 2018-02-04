package code.ponfee.commons.jce.rsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

import code.ponfee.commons.io.Files;
import code.ponfee.commons.jce.Key;
import code.ponfee.commons.jce.hash.HashUtils;
import code.ponfee.commons.util.Bytes;

/**
 * RSA Cryptor based sha512 xor 
 * @author Ponfee
 */
public class RSAHashCryptor extends RSANoPaddingCryptor {

    private static final int SHORT_BYTE_LEN = 2;

    /**
     * (origin ⊕ passwd) ⊕ passwd = origin
     * @param input
     * @param length
     * @param ek
     * @return
     */
    public @Override byte[] encrypt(byte[] input, int length, Key ek) {
        RSAKey rsaKey = (RSAKey) ek;
        BigInteger exponent = getExponent(rsaKey);

        // 生成随机密码
        BigInteger passwd = super.random(rsaKey.n);

        // 对密码进行RSA加密，encryptedPasswd = passwd^e mode n
        byte[] encryptedPasswd = passwd.modPow(exponent, rsaKey.n).toByteArray();

        // 对密码进行HASH
        byte[] hashedPasswd = HashUtils.sha512(passwd.toByteArray());

        int kLen = SHORT_BYTE_LEN + encryptedPasswd.length;
        byte[] result = Arrays.copyOf(Bytes.fromShort((short) encryptedPasswd.length), kLen + length);
        System.arraycopy(encryptedPasswd, 0, result, SHORT_BYTE_LEN, encryptedPasswd.length);

        for (int hLen = hashedPasswd.length, i = 0, j = 0; i < length; i++, j++) {
            if (j == hLen) {
                j = 0;
            }
            result[kLen + i] = (byte) (input[i] ^ hashedPasswd[j]);
        }
        return result;
    }

    public @Override byte[] decrypt(byte[] input, Key dk) {
        RSAKey rsaKey = (RSAKey) dk;
        BigInteger exponent = getExponent(rsaKey);

        // 获取被加密的密码数据
        int len = Bytes.toShort(input);
        byte[] encryptedPasswd = Arrays.copyOfRange(input, SHORT_BYTE_LEN, SHORT_BYTE_LEN + len);

        // 解密被加密的密码数据，passwd = encryptedPasswd^d mode n
        BigInteger passwd = new BigInteger(1, encryptedPasswd).modPow(exponent, rsaKey.n);

        // 对密码进行HASH
        byte[] hashedPasswd = HashUtils.sha512(passwd.toByteArray());

        byte[] result = new byte[input.length - SHORT_BYTE_LEN - encryptedPasswd.length];
        int kLen = SHORT_BYTE_LEN + encryptedPasswd.length;
        for (int hLen = hashedPasswd.length, i = 0, j = 0; i < result.length; i++, j++) {
            if (j == hLen) {
                j = 0;
            }
            result[i] = (byte) (input[kLen + i] ^ hashedPasswd[j]);
        }
        return result;
    }

    public @Override void encrypt(InputStream input, Key ek, OutputStream output) {
        RSAKey rsaKey = (RSAKey) ek;
        BigInteger exponent = getExponent(rsaKey);

        // 生成随机密码
        BigInteger passwd = super.random(rsaKey.n);

        // 对密码进行RSA加密，encryptedPasswd = passwd^e mode n
        byte[] encryptedPasswd = passwd.modPow(exponent, rsaKey.n).toByteArray();

        // 对密码进行HASH
        byte[] hashedPasswd = HashUtils.sha512(passwd.toByteArray());
        int hLen = hashedPasswd.length;

        try {
            output.write(Bytes.fromShort((short) encryptedPasswd.length)); // SHORT_BYTE_LEN
            output.write(encryptedPasswd); // encrypted passwd

            byte[] buffer = new byte[getOriginBlockSize(rsaKey)];
            for (int len, i, j; (len = input.read(buffer)) != Files.EOF;) {
                for (i = 0, j = 0; i < len; i++, j++) {
                    if (j == hLen) {
                        j = 0;
                    }
                    output.write((byte) (buffer[i] ^ hashedPasswd[j]));
                }
            }
            output.flush();
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public @Override void decrypt(InputStream input, Key dk, OutputStream output) {
        try {
            if (input.available() < SHORT_BYTE_LEN + 1) {
                throw new IllegalArgumentException("Invalid cipher data");
            }

            RSAKey rsaKey = (RSAKey) dk;
            BigInteger exponent = getExponent(rsaKey);

            byte[] prefixBytes = new byte[SHORT_BYTE_LEN];
            input.read(prefixBytes);

            // 获取被加密的密码数据
            byte[] encryptedPasswd = new byte[Bytes.toShort(prefixBytes)];
            input.read(encryptedPasswd);

            // 解密被加密的密码数据，passwd = encryptedPasswd^d mode n
            BigInteger passwd = new BigInteger(1, encryptedPasswd).modPow(exponent, rsaKey.n);

            // 对密码进行HASH
            byte[] hashedPasswd = HashUtils.sha512(passwd.toByteArray());
            byte[] buffer = new byte[getCipherBlockSize(rsaKey)];
            for (int len, hLen = hashedPasswd.length, i, j; (len = input.read(buffer)) != Files.EOF;) {
                for (i = 0, j = 0; i < len; i++, j++) {
                    if (j == hLen) {
                        j = 0;
                    }
                    output.write((byte) (buffer[i] ^ hashedPasswd[j]));
                }
            }
            output.flush();
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    public @Override int getOriginBlockSize(RSAKey rsaKey) {
        return 4096;
    }

    public @Override int getCipherBlockSize(RSAKey rsaKey) {
        return this.getOriginBlockSize(rsaKey);
    }
}
