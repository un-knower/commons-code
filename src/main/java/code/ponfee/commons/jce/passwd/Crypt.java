package code.ponfee.commons.jce.passwd;

import static code.ponfee.commons.jce.HmacAlgorithm.ALGORITHM_MAPPING;

import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Preconditions;

import code.ponfee.commons.jce.HmacAlgorithm;
import code.ponfee.commons.jce.hash.HmacUtils;
import code.ponfee.commons.util.SecureRandoms;

/**
 * the passwd crypt based hmac
 * @author Ponfee
 */
public class Crypt {

    private static final String SEPARATOR = "$";

    public static String create(String passwd) {
        return create(HmacAlgorithm.HmacSHA256, passwd, 32);
    }

    /**
     * create crypt
     * @param alg
     * @param passwd
     * @param rounds
     * @return
     */
    public static String create(HmacAlgorithm alg, String passwd, int rounds) {
        Preconditions.checkArgument(rounds >= 1 && rounds <= 0xff, 
                                    "iterations must between 1 and 255");

        byte[] salt = SecureRandoms.nextBytes(32);
        long algIdx = ALGORITHM_MAPPING.inverse().get(alg) & 0xf; // maximum is 0xf
        byte[] hashed = crypt(alg, passwd.getBytes(), salt, rounds);

        return new StringBuilder(6 + (salt.length + hashed.length) * 4 / 3 + 4)
                    .append(SEPARATOR).append(Long.toString(algIdx << 8L | rounds, 16))
                    .append(SEPARATOR).append(toBase64(salt))
                    .append(SEPARATOR).append(toBase64(hashed))
                    .toString();
    }

    /**
     * check the passwd crypt
     * @param passwd
     * @param hashed
     * @return
     */
    public static boolean check(String passwd, String hashed) {
        String[] parts = hashed.split("\\" + SEPARATOR);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid hashed value");
        }

        long params = Long.parseLong(parts[1], 16);
        HmacAlgorithm alg = ALGORITHM_MAPPING.get((int) (params >> 8 & 0xf));
        byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
        byte[] testHash = crypt(alg, passwd.getBytes(), salt, (int) params & 0xff);

        // compare
        return Arrays.equals(Base64.getUrlDecoder().decode(parts[3]), testHash);
    }

    /**
     * crypt with hmac
     * @param alg
     * @param password
     * @param salt
     * @param rounds
     * @return
     */
    private static byte[] crypt(HmacAlgorithm alg, byte[] password, 
                                byte[] salt, int rounds) {
        Mac mac = HmacUtils.getInitializedMac(alg, salt);
        for (int i = 0; i < rounds; i++) {
            password = mac.doFinal(password);
        }
        return password;
    }

    private static String toBase64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String passwd = "passwd";
        String hashed = create(passwd);
        System.out.println(hashed);
        for (int i = 0; i < 100000; i++) {
            if (!check(passwd, hashed)) {
                System.err.println("fail");
            }
        }
        System.out.println(System.currentTimeMillis()-start);

        System.out.println(Hex.encodeHexString(crypt(HmacAlgorithm.HmacSHA256, "password".getBytes(), "salt".getBytes(), 64)));
    }
}