package redis.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import reactor.core.publisher.Mono;
import redis.common.RedisClientConfig;

import java.io.IOException;

/**
 *
 * @author 엄승하
 */
public class AsyncSortedSetSample {

	static ObjectMapper OM = new ObjectMapper();

	public static void main(String[] args) throws Exception {

		RedisClient redisClient = RedisClient.create(RedisClientConfig.REDIS_URI);
		StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(new ByteArrayCodec());

		final RedisReactiveCommands<byte[], byte[]> command = connection.reactive();

		final String key = "rank2";

		//성능을 생각하면 가능하면 하나의 컬렉션에는 1만개 이하의 아이템을 담는게 좋음
		//		for (int i = 1; i <= 100; i++) {
		//			command.zadd(OM.writeValueAsBytes(key), Double.valueOf(i), String.format("user-%s", i));
		//		}

		for (int i = 1; i <= 100; i++) {
			System.out.println(i);
			Mono<Long> tt = command.zadd(OM.writeValueAsBytes(key), Double.valueOf(i), String.format("user-%s", i));
		}

		//Mono<byte[]> result = command.get(OM.writeValueAsBytes(key));

		command.get(OM.writeValueAsBytes(key)).map(byteUser -> {
			try {
				System.out.println("Check point 1");
				System.out.println(OM.readValue(byteUser, String.class));
			} catch (IOException e) {
				e.printStackTrace();
			}

			connection.close();
			redisClient.shutdown();

			System.out.println("finish");

			return "";
		});
	}
}

