package redis.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import redis.common.ConstantsRedis;

/**
 *
 * @author 엄승하
 */
public class SortedSetSample {

	public static void main(String[] args) {

		RedisClient redisClient = RedisClient.create(ConstantsRedis.REDIS_URI);
		StatefulRedisConnection<String, String> connection = redisClient.connect();
		RedisCommands<String, String> syncCommands = connection.sync();

		final String key = "rank";

		if (syncCommands.exists(key) > 0) {
			syncCommands.del(key); //테스트 데이터 저장 전에 삭제 진행
		}

		//성능을 생각하면 가능하면 하나의 컬렉션에는 1만개 이하의 아이템을 담는게 좋음
		for (int i = 1; i <= 100; i++) {
			syncCommands.zadd(key, i, String.format("user-%s", i));
		}

		syncCommands.zincrby(key, 100, "user-1");
		System.out.println("갯수:" + syncCommands.zcard(key));

		System.out.println(syncCommands.zrange(key, 1, 2));

		connection.close();
		redisClient.shutdown();

		System.out.println("finish");

	}
}
