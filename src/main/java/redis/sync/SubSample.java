package redis.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import redis.common.ConstantsRedis;

/**
 *
 * @author 엄승하
 */
public class SubSample {

	public static void main(String[] args) {

		RedisClient client = RedisClient.create(ConstantsRedis.REDIS_URI);

		//subscribe
		StatefulRedisPubSubConnection<String, String> con = client.connectPubSub();

		RedisPubSubListener<String, String> listener = new RedisPubSubAdapter<String, String>() {

			@Override
			public void message(String channel, String message) {
				System.out.println(String.format("subscribe ==> Channel: '%s', Message: '%s'", channel, message));
			}
		};

		con.addListener(listener);

		long subStat = System.currentTimeMillis();
		RedisPubSubCommands<String, String> sync = con.sync();

		System.out.println("\n\n===== start subscribe");

		while (true) {
			sync.subscribe(ConstantsRedis.PUB_SUB_CHANNEL);

			pub();

			if (System.currentTimeMillis() > (subStat + 1_000)) { //1초 후 종료 => pub에서 10초내에 메시지를 보내면 로그 확인 가능
				System.out.println("\n\n==== finish subscribe");
				break;
			}
		}

	}

	public static void pub() {

		RedisClient client = RedisClient.create(ConstantsRedis.REDIS_URI);
		StatefulRedisConnection<String, String> sender = client.connect();

		System.out.println("start pub");
		//send to channel
		sender.sync().publish(ConstantsRedis.PUB_SUB_CHANNEL, "Message 1. request millis: " + System.currentTimeMillis());

		System.out.println("finish pub");
	}
}


