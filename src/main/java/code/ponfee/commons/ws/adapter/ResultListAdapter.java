package code.ponfee.commons.ws.adapter;

import java.lang.reflect.Array;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.google.common.collect.Lists;

import code.ponfee.commons.model.Result;
import code.ponfee.commons.reflect.GenericUtils;
import code.ponfee.commons.ws.adapter.model.ArrayItem;

/**
 * Result<List<T>>转换器
 * @author fupf
 * @param <T>
 */
//ParameterizedTypeImpl cannot be cast to TypeVariable
//abstract class ResultListAdapter<T> extends XmlAdapter<Result<Item<T>[]>, Result<List<T>>> {
public abstract class ResultListAdapter<T> extends XmlAdapter<Result<ArrayItem<T>>, Result<List<T>>> {

    protected final Class<T> type;

    protected ResultListAdapter() {
        type = GenericUtils.getActualTypeArgument(this.getClass());
    }

    @Override
    public Result<List<T>> unmarshal(Result<ArrayItem<T>> v) {
        if (v.getData() == null) {
            return new Result<>(v.getCode(), v.getMsg(), null);
        } else if (v.getData().getItem() == null) {
            return new Result<>(v.getCode(), v.getMsg(), Lists.newArrayList());
        }

        List<T> list = Lists.newArrayList(v.getData().getItem());
        return new Result<>(v.getCode(), v.getMsg(), list);
    }

    @SuppressWarnings("unchecked")
    public @Override Result<ArrayItem<T>> marshal(Result<List<T>> v) {
        if (v.getData() == null) {
            return new Result<>(v.getCode(), v.getMsg(), null);
        }

        T[] array = v.getData().toArray((T[]) Array.newInstance(type, v.getData().size()));
        return new Result<>(v.getCode(), v.getMsg(), new ArrayItem<>(array));
    }

}
