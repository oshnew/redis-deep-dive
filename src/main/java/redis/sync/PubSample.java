package redis.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import redis.common.ConstantsRedis;

/**
 *
 * @author 엄승하
 */
public class PubSample {

	public static void main(String[] args) {

		RedisClient client = RedisClient.create(ConstantsRedis.REDIS_URI);
		StatefulRedisConnection<String, String> sender = client.connect();

		//send to channel
		sender.sync().publish(ConstantsRedis.PUB_SUB_CHANNEL, "Message 1");
		sender.sync().publish(ConstantsRedis.PUB_SUB_CHANNEL, "Message 2");
		sender.sync().publish(ConstantsRedis.PUB_SUB_CHANNEL, "Message 3");

		System.out.println("finish pub");
	}
}


