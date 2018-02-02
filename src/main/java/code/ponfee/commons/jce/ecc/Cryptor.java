package code.ponfee.commons.jce.ecc;

import java.security.SecureRandom;

/**
 * This class of the Cryptor base class
 * @author Ponfee
 */
public abstract class Cryptor {

    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public final byte[] encrypt(byte[] original, Key ek) {
        return encrypt(original, original.length, ek);
    }

    /**
     * encrypt original data in length byte
     * @param original 
     * @param length the byte length of original
     * @param ek encrypt key
     * @return
     */
    public abstract byte[] encrypt(byte[] original, int length, Key ek);

    /**
     * decrypt the cipher use decrypt key
     * @param cipher
     * @param dk
     * @return
     */
    public abstract byte[] decrypt(byte[] cipher, Key dk);

    /**
     * generate cryptor key
     * @return
     */
    public abstract Key generateKey();

    /**
     * encrypt block size
     * normal 64 byte of sha-512
     * @param ek encrypt key
     * @return
     */
    public int getBlockSize(Key ek) {
        return 64;
    }

}
