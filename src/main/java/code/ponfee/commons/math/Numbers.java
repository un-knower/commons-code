package code.ponfee.commons.math;

import code.ponfee.commons.util.ObjectUtils;
import com.google.common.primitives.Chars;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * 数字工具类
 *
 * @author Ponfee
 */
public final class Numbers {
    private Numbers() {}

    public static final Integer INTEGER_ZERO = 0;
    public static final int     INT_ZERO     = 0;
    public static final byte    BYTE_ZERO    = 0x00;

    // --------------------------------------------------------------character convert
    public static char toChar(Object obj, char defaultVal) {
        return toWrapChar(obj, defaultVal);
    }

    public static char toChar(Object obj) {
        return toWrapChar(obj, '\u0000');
    }

    public static Character toWrapChar(Object obj) {
        return toWrapChar(obj, null);
    }

    public static Character toWrapChar(Object obj, Character defaultVal) {
        if (obj == null) {
            return defaultVal;
        } else if (obj instanceof Character) {
            return (Character) obj;
        } else if (obj instanceof Number) {
            return (char) ((Number) obj).intValue();
        } else if (obj instanceof byte[]) {
            return Chars.fromByteArray((byte[]) obj);
        } else {
            String str = obj.toString();
            return str.length() == 1 ? str.charAt(0) : defaultVal;
        }
    }

    // -----------------------------------------------------------------to primary number
    public static byte toByte(Object obj) {
        return toByte(obj, (byte) 0);
    }

    public static byte toByte(Object obj, byte defaultVal) {
        if (obj instanceof Byte) {
            return (byte) obj;
        }
        return ((Double) toDouble(obj, defaultVal)).byteValue();
    }

    public static short toShort(Object obj) {
        return toShort(obj, (short) 0);
    }

    public static short toShort(Object obj, short defaultVal) {
        if (obj instanceof Short) {
            return (short) obj;
        }
        return ((Double) toDouble(obj, defaultVal)).shortValue();
    }

    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }

    public static int toInt(Object obj, int defaultVal) {
        if (obj instanceof Integer) {
            return (int) obj;
        }
        return ((Double) toDouble(obj, defaultVal)).intValue();
    }

    public static long toLong(Object obj) {
        return toLong(obj, 0L);
    }

    public static long toLong(Object obj, long defaultVal) {
        if (obj instanceof Long) {
            return (long) obj;
        }
        return ((Double) toDouble(obj, defaultVal)).longValue();
    }

    public static float toFloat(Object obj) {
        return toFloat(obj, 0.0F);
    }

    public static float toFloat(Object obj, float defaultVal) {
        if (obj instanceof Float) {
            return (float) obj;
        }
        return ((Double) toDouble(obj, defaultVal)).floatValue();
    }

    public static double toDouble(Object obj) {
        return toWrapDouble(obj, 0.0D);
    }

    public static double toDouble(Object obj, double defaultVal) {
        return toWrapDouble(obj, defaultVal);
    }

    // -----------------------------------------------------------------to wrapper number
    public static Byte toWrapByte(Object obj) {
        return toWrapByte(obj, null);
    }

    public static Byte toWrapByte(Object obj, Byte defaultVal) {
        if (obj instanceof Byte) {
            return (Byte) obj;
        }
        Double value = toWrapDouble(obj, toWrapDouble(defaultVal));
        return value == null ? null : value.byteValue();
    }

    public static Short toWrapShort(Object obj) {
        return toWrapShort(obj, null);
    }

    public static Short toWrapShort(Object obj, Short defaultVal) {
        if (obj instanceof Short) {
            return (Short) obj;
        }
        Double value = toWrapDouble(obj, toWrapDouble(defaultVal));
        return value == null ? null : value.shortValue();
    }

    public static Integer toWrapInt(Object obj) {
        return toWrapInt(obj, null);
    }

    public static Integer toWrapInt(Object obj, Integer defaultVal) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        Double value = toWrapDouble(obj, toWrapDouble(defaultVal));
        return value == null ? null : value.intValue();
    }

    public static Long toWrapLong(Object obj) {
        return toWrapLong(obj, null);
    }

    public static Long toWrapLong(Object obj, Long defaultVal) {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        Double value = toWrapDouble(obj, toWrapDouble(defaultVal));
        return value == null ? null : value.longValue();
    }

    public static Float toWrapFloat(Object obj) {
        return toWrapFloat(obj, null);
    }

    public static Float toWrapFloat(Object obj, Float defaultVal) {
        if (obj instanceof Float) {
            return (Float) obj;
        }
        Double value = toWrapDouble(obj, toWrapDouble(defaultVal));
        return value == null ? null : value.floatValue();
    }

    public static Double toWrapDouble(Object obj) {
        return toWrapDouble(obj, null);
    }

    public static Double toWrapDouble(Object obj, Double defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }

        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException ignored) {
            //ignored.printStackTrace();
            return defaultVal;
        }
    }

    public static Double toWrapDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    // ---------------------------------------------------------------------number format
    /**
     * 数字精度化
     *
     * @param value
     * @param scale
     * @return
     */
    public static double scale(Object value, int scale) {
        Double val = toDouble(value);

        if (scale < 0) {
            return val;
        }

        return new BigDecimal(val).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 向下转单位
     *
     * @param value
     * @param scale
     * @return
     */
    public static double lower(double value, int scale) {
        return new BigDecimal(value / Math.pow(10, scale))
                .setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 向上转单位
     *
     * @param value
     * @param pow
     * @return
     */
    public static double upper(double value, int pow) {
        return new BigDecimal(value * Math.pow(10, pow)).doubleValue();
    }

    /**
     * 百分比
     *
     * @param numerator
     * @param denominator
     * @param scale
     * @return
     */
    public static String percent(double numerator, double denominator, int scale) {
        if (denominator == 0) {
            return "--";
        }

        return percent(numerator / denominator, scale);
    }

    /**
     * 百分比
     *
     * @param value
     * @param scale
     * @return
     */
    public static String percent(double value, int scale) {
        String format = "#,##0";
        if (scale > 0) {
            format += "." + StringUtils.leftPad("", scale, '0');
        }
        return new DecimalFormat(format + "%").format(value);
    }

    /**
     * 数字格式化
     *
     * @param obj
     * @return
     */
    public static String format(Object obj) {
        return format(obj, "###,###.###");
    }

    /**
     * 数字格式化
     *
     * @param obj
     * @param format
     * @return
     */
    public static String format(Object obj, String format) {
        NumberFormat fmt = new DecimalFormat(format);
        if (obj instanceof CharSequence) {
            String str = obj.toString().replaceAll(",", "");
            if (str.endsWith("%")) {
                return fmt.format(Double.parseDouble(str.substring(0, str.length() - 1))) + "%";
            } else {
                return fmt.format(Double.parseDouble(str));
            }
        } else {
            return fmt.format(obj);
        }
    }

    /**
     * 两数相加
     *
     * @param num1
     * @param num2
     * @return
     */
    public static double add(Double num1, Double num2) {
        return ObjectUtils.ifNull(num1, 0D)
             + ObjectUtils.ifNull(num2, 0D);
    }

    /**
     * 区间取值
     *
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static int bounds(Integer value, int min, int max) {
        if (value == null || value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    /**
     * 分片
     *
     * @param quantity
     * @param segment
     * @return
     */
    public static int[] average(int quantity, int segment) {
        int[] array = new int[segment];
        int remainder = quantity % segment;
        Arrays.fill(array, 0, remainder, quantity / segment + 1);
        Arrays.fill(array, remainder, segment, quantity / segment);
        return array;
    }

    /**
     * 数字比较
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Long a, Long b) {
        return (a == b) || (a != null && a.equals(b));
    }

    /**
     * 数字比较
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Long a, Integer b) {
        return a != null && b != null && a.intValue() == b;
    }

    /**
     * 数字比较
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Integer a, Integer b) {
        return (a == b) || (a != null && a.equals(b));
    }

    // --------------------------------------------------------------------------金额汉化
    private static final String[] CN_UPPER_NUMBER = {
        "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"
    };
    private static final String[] CN_UPPER_MONETARY_UNIT = {
        "分", "角", "元", "拾", "佰", "仟", "万", "拾", "佰",
        "仟", "亿", "拾", "佰", "仟", "兆", "拾", "佰", "仟"
    };
    private static final BigDecimal MAX_VALUE = new BigDecimal("9999999999999999.995");

    /**
     * 金额汉化（单位元）
     *
     * @param amount
     * @return
     */
    public static String chinesize(BigDecimal amount) {
        if (amount.compareTo(MAX_VALUE) >= 0) {
            throw new IllegalArgumentException("The amount value too large.");
        }
        int signum = amount.signum(); // 正负数：0,1,-1
        if (signum == 0) {
            return "零元整";
        }

        // * 100
        long number = amount.movePointRight(2).setScale(0, BigDecimal.ROUND_HALF_UP)
                            .abs().longValue();
        int scale = (int) (number % 100), numIndex;
        if (scale == 0) {
            numIndex = 2;
            number = number / 100;
        } else if (scale % 10 == 0) {
            numIndex = 1;
            number = number / 10;
        } else {
            numIndex = 0;
        }
        boolean getZero = numIndex != 0;

        StringBuilder builder = new StringBuilder();
        for (int zeroSize = 0, numUnit; number > 0; number = number / 10, ++numIndex) {
            numUnit = (int) (number % 10); // get the last number
            if (numUnit > 0) {
                if ((numIndex == 9) && (zeroSize >= 3)) {
                    builder.insert(0, CN_UPPER_MONETARY_UNIT[6]);
                }
                if ((numIndex == 13) && (zeroSize >= 3)) {
                    builder.insert(0, CN_UPPER_MONETARY_UNIT[10]);
                }
                builder.insert(0, CN_UPPER_MONETARY_UNIT[numIndex]);
                builder.insert(0, CN_UPPER_NUMBER[numUnit]);
                getZero = false;
                zeroSize = 0;
            } else {
                ++zeroSize;
                if (!getZero) {
                    builder.insert(0, CN_UPPER_NUMBER[numUnit]);
                }
                if (numIndex == 2) {
                    if (number > 0) {
                        builder.insert(0, CN_UPPER_MONETARY_UNIT[numIndex]);
                    }
                } else if ( (((numIndex - 2) & 0x03) == 0) && (number % 1000 > 0) ) {
                    builder.insert(0, CN_UPPER_MONETARY_UNIT[numIndex]);
                }
                getZero = true;
            }
        }

        if (signum == -1) {
            builder.insert(0, "负"); // 负数
        }

        if (scale == 0) {
            builder.append("整"); // 整数
        }
        return builder.toString();
    }

    /**
     * to upper hex string and remove prefix 0
     *
     * @param num the BigInteger
     * @return upper hex string
     */
    public static String toHex(BigInteger num) {
        String hex = Hex.encodeHexString(num.toByteArray(), false);
        if (hex.matches("^0+$")) {
            return "0";
        }
        return hex.replaceFirst("^0*", "");
    }

    public static BigInteger toBigInteger(String hex) {
        return new BigInteger(hex, 16);
    }

    public static void main(String[] args) {
        System.out.println(lower(441656, 2));
        System.out.println(percent(0.00241, 1));

        System.out.println(ObjectUtils.toString(average(10, 20)));

        double money = 2020004.7;
        System.out.println("[" + money + "]   ->   [" + chinesize(new BigDecimal(money)) + "]");

        money = 85050414.776;
        System.out.println("[" + money + "]   ->   [" + chinesize(new BigDecimal(money)) + "]");

        System.out.println(chinesize(new BigDecimal("9999999999999999.9949")));
        System.out.println(chinesize(new BigDecimal("9999999999999999.995")));
    }
}
