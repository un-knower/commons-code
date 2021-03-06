package code.ponfee.commons.model;

import java.util.Dictionary;
import java.util.Map;

import com.github.pagehelper.PageHelper;

import code.ponfee.commons.math.Numbers;
import code.ponfee.commons.reflect.Fields;

/**
 * 分页参数处理类
 * 基于github上的mybatis分页工具
 * @author fupf
 */
public final class PageHandler {

    public static final PageHandler NORMAL = new PageHandler("pageNum", "pageSize", "offset", "limit");

    private final String paramPageNum;
    private final String paramPageSize;
    private final String paramOffset;
    private final String paramLimit;

    public PageHandler(String paramPageNum, String paramPageSize, 
                       String paramOffset, String paramLimit) {
        this.paramPageNum = paramPageNum;
        this.paramPageSize = paramPageSize;
        this.paramOffset = paramOffset;
        this.paramLimit = paramLimit;
    }

    public <T> void handle(T params) {
        Integer pageSize = getInt(params, paramPageSize);
        Integer limit = getInt(params, paramLimit);

        // 默认通过pageSize查询
        if (pageSize == null && limit == null) {
            pageSize = 0;
        }

        // 分页处理
        if (pageSize != null) {
            startPage(getInt(params, paramPageNum), pageSize);
        } else {
            offsetPage(getInt(params, paramOffset), limit);
        }
    }

    /**
     * 分页查询（类oracle方式）</p>
     * pageSize=0时查询全部数据
     * @param pageNum
     * @param pageSize
     */
    public static void startPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 0) {
            pageSize = 0;
        }
        PageHelper.startPage(pageNum, pageSize);
    }

    /**
     * 分页查询（类mysql方式）</p>
     * RowBounds.limit=0则会查询出全部的结果
     * @param offset
     * @param limit
     */
    public static void offsetPage(Integer offset, Integer limit) {
        if (offset == null || offset < 0) {
            offset = 0;
        }
        if (limit == null || limit < 0) {
            limit = 0;
        }
        PageHelper.offsetPage(offset, limit);
    }

    /**
     * get page number from java bean or map or dictionary
     * @param params
     * @param name
     * @return
     */
    private static <T> Integer getInt(T params, String name) {
        try {
            Object value;
            if (Map.class.isInstance(params) || Dictionary.class.isInstance(params)) {
                value = params.getClass().getMethod("get", Object.class).invoke(params, name);
            } else {
                value = Fields.get(params, name);
            }
            return Numbers.toWrapInt(value);
        } catch (Exception e) {
            return null;
        }
    }

}
