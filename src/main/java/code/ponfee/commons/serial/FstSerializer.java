package code.ponfee.commons.serial;

import java.util.Map;

import org.nustaq.serialization.FSTConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import code.ponfee.commons.io.GzipProcessor;
import code.ponfee.commons.reflect.ClassUtils;

/**
 * fst序例化
 * @author fupf
 */
public class FstSerializer extends Serializer {

    // createDefaultConfiguration
    private static final ThreadLocal<FSTConfiguration> FST_CFG =
            ThreadLocal.withInitial(FSTConfiguration::createStructConfiguration);

    @Override
    public <T extends Object> byte[] serialize(T t, boolean isCompress) {
        if (t == null) {
            return null;
        }

        byte[] data = FST_CFG.get().asByteArray(t);
        if (isCompress) {
            data = GzipProcessor.compress(data);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Object> T deserialize(byte[] data, Class<T> clazz, boolean isCompress) {
        if (data == null) {
            return null;
        }

        if (isCompress) {
            data = GzipProcessor.decompress(data);
        }
        T t = (T) FST_CFG.get().asObject(data);
        if (!clazz.isInstance(t)) {
            throw new ClassCastException(ClassUtils.getClassName(t.getClass())
                     + " can't be cast to " + ClassUtils.getClassName(clazz));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Map<String, Object> map = ImmutableMap.of("a", 1, "b", Lists.newArrayList("1", "2"));
        Serializer serializer = new FstSerializer(); // error
        //Serializer serializer = new JsonSerializer();
        //Serializer serializer = new HessianSerializer();
        byte[] data = serializer.serialize(map);
        //map = serializer.deserialize(data, HashMap.class);
        map = serializer.deserialize(data, Map.class);
        System.out.println(map.getClass());
    }

}
