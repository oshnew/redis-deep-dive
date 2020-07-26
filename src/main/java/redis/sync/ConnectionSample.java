package redis.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import redis.common.ConstantsRedis;

/**
 *
 * @author 엄승하
 */
public class ConnectionSample {

	public static void main(String[] args) {

		RedisClient redisClient = RedisClient.create(ConstantsRedis.REDIS_URI);
		StatefulRedisConnection<String, String> connection = redisClient.connect();
		RedisCommands<String, String> syncCommands = connection.sync();

		syncCommands.set("key", "Hello, Redis!");
		syncCommands.set("key", "Hello, Redis!" + System.currentTimeMillis());

		syncCommands.set("ttl-data", "It is ttl data");
		syncCommands.expire("ttl-data", 10);

		connection.close();
		redisClient.shutdown();

		System.out.println("finish");

	}
}
