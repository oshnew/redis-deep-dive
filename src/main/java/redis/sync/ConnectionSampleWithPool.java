package redis.sync;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.common.RedisClientConfig;

/**
 *
 * @author 엄승하
 */
public class ConnectionSampleWithPool {

	public static void main(String[] args) throws Exception {

		GenericObjectPool<StatefulRedisConnection<String, String>> pool = nonClusterPoolUsage();

		try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {
			connection.sync().set("key-pool-test", String.valueOf(System.currentTimeMillis()));
			String value = connection.sync().get("key-pool-test");
			System.out.println(value);
		} catch (RedisConnectionException e) {
			System.out.println(String.format("Failed to connect to Redis server: %s", e));

		} catch (Exception e) {
			e.printStackTrace();
		}

		pool.close();
		System.out.println("finish");

	}

	/**
	 * 비 클러스터 모드일때 커넥션풀 셋팅
	 *
	 * @return
	 */
	private static GenericObjectPool<StatefulRedisConnection<String, String>> nonClusterPoolUsage() {

		RedisClient client = RedisClient.create(RedisClientConfig.REDIS_URI);
		client.setOptions(ClientOptions.builder().autoReconnect(true).build());

		return ConnectionPoolSupport.createGenericObjectPool(() -> client.connect(), createPoolConfig());
	}

	/**
	 * 클러스터모드일때 커넥션 풀 셋팅
	 *  - 참고: https://github.com/Azure/azure-redis-cache-samples/blob/master/Java/ClientSamples/src/main/java/lettuce/PoolUsage.java
	 *
	 * @return
	 */
	private static GenericObjectPool<StatefulRedisClusterConnection<String, String>> useClusterPoolUsage() {

		RedisClusterClient clusterClient = RedisClusterClient.create(RedisClientConfig.REDIS_URI);
		clusterClient.setOptions(ClusterClientOptions.builder().autoReconnect(true).build());

		return ConnectionPoolSupport.createGenericObjectPool(() -> clusterClient.connect(), createPoolConfig());
	}

	/**
	 * 커넥션풀 설정 생성
	 *
	 * @return
	 */
	private static GenericObjectPoolConfig createPoolConfig() {

		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

		poolConfig.setMaxTotal(20);
		poolConfig.setMaxIdle(10);

		// "true" will result better behavior when unexpected load hits in production
		// "false" makes it easier to debug when your maxTotal/minIdle/etc settings need adjusting.
		poolConfig.setBlockWhenExhausted(true);
		poolConfig.setMaxWaitMillis(1000);
		poolConfig.setMinIdle(5);

		return poolConfig;
	}

}
