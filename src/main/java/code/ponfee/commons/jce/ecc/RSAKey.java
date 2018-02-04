package code.ponfee.commons.jce.ecc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * The RSA Key
 * @author Ponfee
 */
@SuppressWarnings("restriction")
public class RSAKey implements Key {

    // Should RSA public exponent be only in {3, 5, 17, 257 or 65537} 
    public static final BigInteger RSA_F4 = BigInteger.valueOf(65537);

    public final BigInteger n;
    public final BigInteger e;

    public final BigInteger d;
    public final BigInteger p;
    public final BigInteger q;
    public final BigInteger pe;
    public final BigInteger qe;
    public final BigInteger coeff;

    public final boolean isSecret;

    public RSAKey(int keySize) {
        this(keySize, RSA_F4.intValue());
    }

    public RSAKey(int keySize, int e) {
        this.isSecret = true;
        KeyPair pair = generateKey(keySize, e);
        this.e = pair.e;
        this.p = pair.p;
        this.q = pair.q;
        this.n = pair.n;
        this.pe = pair.pe;
        this.qe = pair.qe;
        this.d = pair.d;
        this.coeff = pair.coeff;
    }

    public RSAKey(BigInteger n, BigInteger e,
                  BigInteger d, BigInteger p, BigInteger q,
                  BigInteger pe, BigInteger qe, BigInteger coeff) {

        Preconditions.checkArgument(n != null && e != null && d != null 
                                    && p != null && q != null && pe != null 
                                    && qe != null && coeff != null);
        this.isSecret = true;
        this.n = n;
        this.e = e;
        this.d = d;
        this.p = p;
        this.q = q;
        this.pe = pe;
        this.qe = qe;
        this.coeff = coeff;
    }

    public RSAKey(BigInteger n, BigInteger e) {
        Preconditions.checkArgument(n != null && e != null);
        this.isSecret = false;
        this.n = n;
        this.e = e;

        this.d = null;
        this.p = null;
        this.q = null;
        this.pe = null;
        this.qe = null;
        this.coeff = null;
    }

    public boolean isPublic() {
        return (!isSecret);
    }

    /**
     * get the public key
     */
    public @Override RSAKey getPublic() {
        return new RSAKey(n, e);
    }

    // Secret: (secret, n, e, d, p, q, pe, qe, coeff)
    // Public: (secret, n, e)
    public @Override void writeKey(OutputStream out) throws IOException {
        DerOutputStream der = new DerOutputStream();
        der.putInteger(this.isSecret ? 0 : 1);
        der.putInteger(this.n);
        der.putInteger(this.e);
        if (this.isSecret) {
            der.putInteger(this.d);
            der.putInteger(this.p);
            der.putInteger(this.q);
            der.putInteger(this.pe);
            der.putInteger(this.qe);
            der.putInteger(this.coeff);
        }
        DerValue dervalue = new DerValue((byte) 48, der.toByteArray());
        out.write(dervalue.toByteArray());
        der.close();
    }

    // Secret: (secret, n, e, d, p, q, pe, qe, coeff)
    // Public: (secret, n, e)
    public @Override RSAKey readKey(InputStream in) throws IOException {
        DerValue der = new DerInputStream(IOUtils.toByteArray(in)).getDerValue();
        if (der.getTag() != 48) {
            throw new IOException("Not a SEQUENCE");
        }

        DerInputStream derIn = der.getData();
        boolean secret = der.getInteger() == 0;
        BigInteger n = getBigInteger(derIn);
        BigInteger e = getBigInteger(derIn);
        RSAKey rsaKey = null;
        if (secret) {
            BigInteger d = getBigInteger(derIn);
            BigInteger p = getBigInteger(derIn);
            BigInteger q = getBigInteger(derIn);
            BigInteger pe = getBigInteger(derIn);
            BigInteger qe = getBigInteger(derIn);
            BigInteger coeff = getBigInteger(derIn);
            rsaKey = new RSAKey(n, e, d, p, q, pe, qe, coeff);
        } else {
            rsaKey = new RSAKey(n, e);
        }
        if (derIn.available() != 0) {
            throw new IOException("Extra data available");
        }
        return rsaKey;
    }

    private static BigInteger getBigInteger(DerInputStream derIn) {
        BigInteger biginteger;
        try {
            biginteger = derIn.getBigInteger();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (biginteger.signum() < 0) {
            biginteger = new BigInteger(1, biginteger.toByteArray());
        }
        return biginteger;
    }

    /**
     * generate the key pair
     * @param keySize
     * @param e
     * @return
     */
    public static KeyPair generateKey(int keySize, int e) {
        KeyPair keyPair = new KeyPair();
        keyPair.e = BigInteger.valueOf(e);
        int i = keySize + 1 >> 1;
        int j = keySize - i;
        do {
            keyPair.p = BigInteger.probablePrime(i, Cryptor.SECURE_RANDOM);
            do {
                keyPair.q = BigInteger.probablePrime(j, Cryptor.SECURE_RANDOM);
                if (keyPair.p.compareTo(keyPair.q) < 0) {
                    BigInteger temp = keyPair.p;
                    keyPair.p = keyPair.q;
                    keyPair.q = temp;
                }
                keyPair.n = keyPair.p.multiply(keyPair.q);
            } while (keyPair.n.bitLength() < keySize);
            keyPair.p1 = keyPair.p.subtract(BigInteger.ONE);
            keyPair.q1 = keyPair.q.subtract(BigInteger.ONE);
            keyPair.phi = keyPair.p1.multiply(keyPair.q1);
        } while (!keyPair.e.gcd(keyPair.phi).equals(BigInteger.ONE));

        keyPair.d = keyPair.e.modInverse(keyPair.phi);
        keyPair.pe = keyPair.d.mod(keyPair.p1);
        keyPair.qe = keyPair.d.mod(keyPair.q1);
        keyPair.coeff = keyPair.q.modInverse(keyPair.p);

        return keyPair;
        // new sun.security.rsa.RSAPublicKeyImpl(n, e);
        // new sun.security.rsa.RSAPrivateCrtKeyImpl(n, e, d, p, q, pe, qe, coeff);
        // return new java.security.KeyPair(RSAPublicKeyImpl, RSAPrivateCrtKeyImpl);
    }

    /**
     * public key
     * @param keySize
     * @param e
     * @return
     */
    public static KeyPair generateKey2(int keySize, int e) {
        KeyPair keyPair = new KeyPair();
        int qs = keySize >> 1;
        keyPair.e = BigInteger.valueOf(e);
        for (;;) {
            for (;;) {
                keyPair.p = new BigInteger(keySize - qs, 1, Cryptor.SECURE_RANDOM);
                if (keyPair.p.subtract(BigInteger.ONE).gcd(keyPair.e).compareTo(BigInteger.ONE) == 0
                    && keyPair.p.isProbablePrime(10)) {
                    break;
                }
            }
            for (;;) {
                keyPair.q = new BigInteger(qs, 1, Cryptor.SECURE_RANDOM);
                if (keyPair.q.subtract(BigInteger.ONE).gcd(keyPair.e).compareTo(BigInteger.ONE) == 0
                    && keyPair.q.isProbablePrime(10)) {
                    break;
                }
            }
            if (keyPair.p.compareTo(keyPair.q) <= 0) {
                BigInteger t = keyPair.p;
                keyPair.p = keyPair.q;
                keyPair.q = t;
            }
            keyPair.p1 = keyPair.p.subtract(BigInteger.ONE);
            keyPair.q1 = keyPair.q.subtract(BigInteger.ONE);
            keyPair.phi = keyPair.p1.multiply(keyPair.q1);
            if (keyPair.phi.gcd(keyPair.e).compareTo(BigInteger.ONE) == 0) {
                keyPair.n = keyPair.p.multiply(keyPair.q);
                keyPair.d = keyPair.e.modInverse(keyPair.phi);
                keyPair.pe = keyPair.d.mod(keyPair.p1);
                keyPair.qe = keyPair.d.mod(keyPair.q1);
                keyPair.coeff = keyPair.q.modInverse(keyPair.p);
                break;
            }
        }
        return keyPair;
    }

    private static class KeyPair {
        private BigInteger n;
        private BigInteger e;

        private BigInteger d;
        private BigInteger p;
        private BigInteger q;
        private BigInteger pe;
        private BigInteger qe;
        private BigInteger coeff;

        private BigInteger p1;
        private BigInteger q1;
        private BigInteger phi;
    }

    public static void main(String[] args) {
        Stopwatch watch = Stopwatch.createStarted();
        generateKey(4096, RSA_F4.intValue());
        System.out.println("generateKey1: " + watch.stop());

        watch.reset().start();
        generateKey2(4096, RSA_F4.intValue());
        System.out.println("generateKey2: " + watch.stop());
    }
}
