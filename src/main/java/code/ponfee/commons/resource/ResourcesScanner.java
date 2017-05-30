package code.ponfee.commons.resource;

import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * <pre>
 *   用法：
 *   new ResourcesScanner("\/**\/").scan4text("*.properties")
 *   new ResourcesScanner("\/**\/").scan4text("*.class");
 *   new ResourcesScanner("/").scan4text("*.xml");
 *   
 *   new ResourcesScanner("code.ponfee").scan4class();
 *   new ResourcesScanner("code.ponfee").scan4class(new Class[] { Service.class });
 * </pre>
 * 
 * @Title：资源扫描
 * @author fupf
 */
public class ResourcesScanner {
    private static Logger logger = LoggerFactory.getLogger(ResourcesScanner.class);
    private static final String DEFAULT_CHARSET = "UTF-8";

    private List<String> scanPaths = new LinkedList<String>();

    /**
     * @param paths 扫描路径
     */
    public ResourcesScanner(String... paths) {
        if (paths == null || paths.length == 0) {
            paths = new String[] { "*" };
        }

        for (String pkg : paths) {
            this.scanPaths.add(pkg);
        }
    }

    /**
     * 类扫描
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<Class<?>> scan4class() {
        return scan4class(new Class[] {});
    }

    /**
     * 类扫描
     * @param annotations 包含指定注解的类
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<Class<?>> scan4class(Class<? extends Annotation>... annotations) {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        if (this.scanPaths.isEmpty()) return classSet;

        List<TypeFilter> typeFilters = new LinkedList<TypeFilter>();
        if (annotations != null) {
            for (Class<? extends Annotation> annotation : annotations) {
                typeFilters.add(new AnnotationTypeFilter(annotation, false));
            }
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String packageName : this.scanPaths) {
            try {
                packageName = packageName.replace('.', '/');
                Resource[] resources = resolver.getResources(CLASSPATH_ALL_URL_PREFIX + packageName + "/**/*.class");
                MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
                for (Resource resource : resources) {
                    if (!resource.isReadable()) continue;

                    MetadataReader reader = readerFactory.getMetadataReader(resource);
                    if (!matchesFilter(reader, typeFilters, readerFactory)) continue;

                    try {
                        classSet.add(Class.forName(reader.getClassMetadata().getClassName()));
                    } catch (Throwable e) {
                        logger.error("load class error", e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return classSet;
    }

    /**
     * 流扫描
     * @return Map<String, byte[]>
     */
    public Map<String, byte[]> scan4binary() {
        return scan4binary("*");
    }

    /**
     * 流扫描
     * @param wildcard 通配符
     * @return
     */
    public Map<String, byte[]> scan4binary(String wildcard) {
        if (wildcard == null) wildcard = "*";
        Map<String, byte[]> result = new HashMap<String, byte[]>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String path : this.scanPaths) {
            try {
                Resource[] resources = resolver.getResources(CLASSPATH_ALL_URL_PREFIX + path + wildcard);
                for (Resource resource : resources) {
                    try (InputStream in = resource.getInputStream()) {
                        result.put(resource.getFilename(), IOUtils.toByteArray(in));
                    } catch (IOException e) {
                        logger.error("scan binary error", e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * 文本扫描
     * @return
     */
    public Map<String, String> scan4text() {
        return scan4text(null, DEFAULT_CHARSET);
    }

    /**
     * 文本扫描
     * @param wildcard
     * @return
     */
    public Map<String, String> scan4text(String wildcard) {
        return scan4text(wildcard, DEFAULT_CHARSET);
    }

    /**
     * 文本扫描
     * @param wildcard
     * @param charset
     * @return
     */
    public Map<String, String> scan4text(String wildcard, String charset) {
        Map<String, String> result = new HashMap<String, String>();
        for (Entry<String, byte[]> entry : scan4binary(wildcard).entrySet()) {
            try {
                result.put(entry.getKey(), new String(entry.getValue(), charset));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * 检查当前扫描到的Bean含有任何一个指定的注解标记
     * @param reader
     * @param typeFilters
     * @param factory
     * @return
     * @throws IOException
     */
    private boolean matchesFilter(MetadataReader reader, List<TypeFilter> typeFilters,
        MetadataReaderFactory factory) throws IOException {
        if (typeFilters.isEmpty()) return true;

        for (TypeFilter filter : typeFilters) {
            if (filter.match(reader, factory)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        //System.out.println(new ResourcesScanner("/**/").scan4binary("*.class"));
        //System.out.println(new ResourcesScanner("/**/*.properties").scan4text());
        System.out.println(new ResourcesScanner("code/ponfee/commons/").scan4class());
    }

}
