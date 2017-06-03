package code.ponfee.commons.jedis;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.Jedis;

/**
 * redis key（键）操作类
 * @author fupf
 */
public class KeysOperations extends JedisOperations {

    KeysOperations(JedisClient jedisClient) {
        super(jedisClient);
    }

    /**
     * 当key不存在时，返回 -2
     * 当key存在但没有设置剩余生存时间时，返回 -1
     * 否则以秒为单位，返回 key的剩余生存时间
     * @param key
     * @return
     */
    public Long ttl(final String key) {
        return ((JedisHook<Long>) sjedis -> sjedis.ttl(key)).hook(this, null, key);
    }

    /**
     * 获取有效期
     * @param key
     * @return
     */
    public Long pttl(final String key) {
        return call(sjedis -> sjedis.pttl(key), null, key);
    }

    /**
     * 获取key列表
     * @param keyWildcard
     * @return
     */
    public Set<String> keys(final String keyWildcard) {
        return call(shardedJedis -> {
            Set<String> keys = new HashSet<>();
            for (Jedis jedis : shardedJedis.getAllShards()) {
                keys.addAll(jedis.keys(keyWildcard));
            }
            return keys;
        }, null, keyWildcard);
    }

    /**
     * 设置失效时间
     * @param key
     * @param seconds
     * @return 是否设置成功
     */
    public boolean expire(final String key, final int seconds) {
        return call(shardedJedis -> {
            return JedisOperations.expire(shardedJedis, key, seconds);
        }, false, key, seconds);
    }

    /**
     * 设置失效时间
     * @param key
     * @param milliseconds
     * @return 是否设置成功
     */
    public boolean pexpire(final String key, final int milliseconds) {
        return call(shardedJedis -> {
            return JedisOperations.pexpire(shardedJedis, key, milliseconds);
        }, false, key, milliseconds);
    }

    /**
     * 判断key是否存在
     * @param key
     * @return
     */
    public boolean exists(final String key) {
        return call(shardedJedis -> {
            return shardedJedis.exists(key);
        }, false, key);
    }

    /**
     * 删除
     * @param key
     * @return 被删除 key 的数量
     */
    public Long del(final String key) {
        return call(shardedJedis -> {
            return shardedJedis.del(key);
        }, null, key);
    }

    /**
     * 删除
     * @param key
     * @return 被删除 key 的数量
     */
    public Long del(final byte[] key) {
        return call(shardedJedis -> {
            return shardedJedis.del(key);
        }, null, key);
    }

    /**
     * 删除key（匹配通配符）
     * @param keyWildcard
     * @return 被删除 key 的数量
     */
    public long dels(final String keyWildcard) {
        return call(shardedJedis -> {
            long delCounts = 0;
            for (Jedis jedis : shardedJedis.getAllShards()) {
                Set<String> keys = jedis.keys(keyWildcard);
                if (keys != null && keys.size() > 0) {
                    delCounts += jedis.del(keys.toArray(new String[keys.size()]));
                }
            }
            return delCounts;
        }, 0L, keyWildcard);
    }

    /**
     * 返回 key 所储存的值的类型。
     * @param key
     * @return none (key不存在)；string (字符串)；list (列表)；set (集合)；zset (有序集)；hash (哈希表)
     */
    public String type(final String key) {
        return call(shardedJedis -> {
            return shardedJedis.type(key);
        }, null, key);
    }

}
