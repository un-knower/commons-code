package code.ponfee.commons.util;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import code.ponfee.commons.io.Files;
import code.ponfee.commons.jce.hash.HashUtils;

/**
 * https://www.jianshu.com/p/ffc97c4d2306
 * 在计算机系统中数值使用补码来表示和存储
 *  原码：正数的原码为其二进制；负数的原码为对应的正数值在高位补1；True form
 *  反码：正数的反码与原码相同；负数的反码为其原码除符号位以外各位取反；1's complement
 *  补码：正数的补码与原码相同；负数的补码为其反码（最低位）加1；2's complement
 * <p>
 * 
 * 补码系统的最大优点是可以在加法或减法处理中，不需因为数字的正负而使用不同的计算方式
 * 计算机中只有加法，用两数补码相加，结果仍是补码表示
 * 补码转原码：减1再取反
 * 
 * byte b = a byte number;
 * int i = b & 0xff; // 使得i与b的二进制补码一致
 * 
 * Base58 code：except number 0, uppercase letter I and O, lowercase latter l
 * Reference from internet
 * @author Ponfee
 */
public class Base58 {

    public static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final int[] INDEXES = new int[128];
    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended).
     * @param data  the bytes to encode
     * @return the base58-encoded string
     */
    public static String encode(byte[] data) {
        if (data.length == 0) {
            return "";
        }

        // Duplicate data 
        data = copyOfRange(data, 0, data.length);

        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < data.length && data[zeroCount] == 0) {
            ++zeroCount;
        }

        // The actual encoding.
        byte[] temp = new byte[data.length * 2];
        int j = temp.length;
        for (int startAt = zeroCount; startAt < data.length;) {
            byte mod = divmod58(data, startAt);
            if (data[startAt] == 0) {
                ++startAt;
            }
            temp[--j] = (byte) ALPHABET[mod];
        }

        // Strip extra '1' if there are some after decoding.
        while (j < temp.length && temp[j] == ALPHABET[0]) {
            ++j;
        }

        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0) {
            temp[--j] = (byte) ALPHABET[0];
        }

        return new String(temp, j, temp.length - j, US_ASCII);
    }

    /**
     * Decodes the given base58 string into the original data bytes.
     * @param data  the base58-encoded string to decode
     * @return  the decoded data bytes
     */
    public static byte[] decode(String data) {
        if (data.length() == 0) {
            return new byte[0];
        }

        byte[] input58 = new byte[data.length()];

        // Transform the String to a base58 byte sequence  
        for (int i = 0; i < data.length(); ++i) {
            char c = data.charAt(i);
            int digit58 = -1;
            if (c >= 0 && c < 128) {
                digit58 = INDEXES[c];
            }
            if (digit58 < 0) {
                throw new IllegalArgumentException("Illegal character '" 
                                                 + c + "' at [" + i + "]");
            }
            input58[i] = (byte) digit58;
        }

        // Count leading zeroes  
        int zeroCount = 0;
        while (zeroCount < input58.length && input58[zeroCount] == 0) {
            ++zeroCount;
        }

        // The encoding  
        byte[] temp = new byte[data.length()];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input58.length) {
            byte mod = divmod256(input58, startAt);
            if (input58[startAt] == 0) {
                ++startAt;
            }

            temp[--j] = mod;
        }

        // Do no add extra leading zeroes, move j to first non null byte.
        while (j < temp.length && temp[j] == 0) {
            ++j;
        }

        return copyOfRange(temp, j - zeroCount, temp.length);
    }

    /**
     * base58 decode and construct BigInteger
     * @param data the base58-encoded string to decode
     * @return the BigInteger of base58-decoded
     */
    public static BigInteger decodeToBigInteger(String data) {
        return new BigInteger(1, decode(data));
    }

    /**
     * Encodes the given bytes as a base58 string (with appended checksum).
     * @param data  the bytes to encode
     * @return the base58-encoded string is appended checksum
     */
    public static String encodeWithChecksum(byte[] data) {
        byte[] twiceSha256 = HashUtils.sha256(HashUtils.sha256(data));
        data = ArrayUtils.addAll(data, Arrays.copyOfRange(twiceSha256, 0, 4));
        return encode(data);
    }

    /**
     * Decodes the given base58 string into the original data bytes, using the checksum in the
     * last 4 bytes of the decoded data to verify that the rest are correct. The checksum is
     * removed from the returned data.
     * 
     * if the data is not base 58 or the checksum is invalid then throw IllegalArgumentException
     *
     * @param data the base58-encoded string to decode (which should include the checksum)
     */
    public static byte[] decodeWithChecksum(String data) {
        byte[] decoded = decode(data);
        if (decoded.length < 4) {
            throw new IllegalArgumentException("Data too short.");
        }

        byte[] bytes = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
        byte[] twiceSha256 = HashUtils.sha256(HashUtils.sha256(bytes));
        byte[] actualChecksum = Arrays.copyOfRange(twiceSha256, 0, 4);

        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);

        if (!Arrays.equals(checksum, actualChecksum)) {
            throw new IllegalArgumentException("Invalid checksum.");
        }
        return bytes;
    }

    // number -> number / 58, returns number % 58
    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            // b & 0xFF再转int是为了保持二进制补码的一致性
            // byte b = -127; 补码：10000001
            // int  i = -127; 补码：111111111111111111111111 10000001
            // Integer.toBinaryString(b & 0xFF) --> 10000001
            // new BigInteger(1, new byte[] { b }).toString(2); // 10000001
            int digit256 = number[i] & 0xFF;
            int temp = remainder * 256 + digit256;
            number[i] = (byte) (temp / 58);
            remainder = temp % 58;
        }

        return (byte) remainder;
    }

    // number -> number / 256, returns number % 256
    private static byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number58.length; i++) {
            int digit58 = number58[i] & 0xFF;
            int temp = remainder * 58 + digit58;
            number58[i] = (byte) (temp / 256);
            remainder = temp % 256;
        }

        return (byte) remainder;
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

    public static void main(String[] args) {
        System.out.println(encode(Files.toByteArray(MavenProjects.getMainJavaFile(Bytes.class))));
        String base58 = encodeWithChecksum(Files.toByteArray(MavenProjects.getMainJavaFile(Bytes.class)));
        System.out.println(base58);
        System.out.println(new String(decodeWithChecksum(base58)));

        byte[] b128 = new byte[16], b0 = new byte[16], b127 = new byte[16];
        Arrays.fill(b128, (byte) -128);
        Arrays.fill(b0, (byte) 0);
        Arrays.fill(b127, (byte) 127);

        System.out.println("==================base58==================");
        System.out.println(encode(b128));
        //System.out.println(encode(b0));
        //System.out.println(encode(b127));

        /*System.out.println("\n==================base64==================");
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(b128));
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(b0));
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(b127));*/
    }
}