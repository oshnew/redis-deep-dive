package redis.sync;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * redis 커넥션풀 기반 연결 샘플
 *
 * @author 엄승하
 */
public class ConnectionSampleWithPool {

	private static final String REDIS_CON_URL = "redis://192.168.56.1:6379/0"; //로컬 redis 0번 사용

	private static AtomicInteger counter = new AtomicInteger(0);

	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {

		GenericObjectPool<StatefulRedisConnection<String, String>> pool = nonClusterPoolUsage(); //sync & 비 클러스터모드 커넥션풀
		//pool = useClusterPoolUsage(); //클러스터모드일때

		testSetValue(pool, "key-pool-test-1", "key-pool-test-1");

		int nThreads = 5;
		CyclicBarrier barrier = new CyclicBarrier(nThreads + 1); //동시 테스트를 위해서 각각의 스레드를 대기처리할 목적
		ExecutorService es = Executors.newFixedThreadPool(nThreads);

		for (int i = 0; i < nThreads; i++) {

			es.submit(() -> {
				int idx = counter.addAndGet(1);
				//System.out.println(String.format("idx:'%s'", idx));

				barrier.await(3000, TimeUnit.MILLISECONDS); //대기: cyclicBarrier 를 생성할 때, 인자값으로 준 count 개수만큼 // await를 호출한다면 모든 쓰레드의 wait 상태가 종료
				System.out.println(String.format("getNumberWaiting: '%s'", barrier.getNumberWaiting()));
				//System.out.println(String.format("Thread idx: %s", idx));

				testSetValue(pool, String.valueOf("test-thread-" + idx), String.valueOf(idx));
				return null;
			});
		}

		//Thread.sleep(5 * 1000); //커넥션풀 close 전에 대기 후, redis-cli(서버)에서 client list 명령어로 현재 연결된 클라이언트 리스트를 확인해보면 됨

		barrier.await();
		es.shutdown(); // 작업이 모두 완료되면 쓰레드풀을 종료

		System.out.println(String.format("before finish getNumberWaiting: '%s'", barrier.getNumberWaiting()));

		//pool.close();
		System.out.println("finish");
	}

	/**
	 * set value 테스트
	 *
	 * @param pool
	 * @param key
	 * @param value
	 */
	private static void testSetValue(GenericObjectPool<StatefulRedisConnection<String, String>> pool, String key, String value) {

		try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) { //pool을 이용해서 커맨드 실행
			connection.sync().set(key, value);
			String rsltValue = connection.sync().get(key);
			System.out.println(String.format("get key's value '%s' -> '%s'", key, rsltValue));
		} catch (RedisConnectionException e) {
			System.out.println(String.format("Failed to connect to Redis server: %s", e));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 비 클러스터 모드일때 커넥션풀 셋팅
	 *
	 * @return
	 */
	private static GenericObjectPool<StatefulRedisConnection<String, String>> nonClusterPoolUsage() {

		ClientResources resources = DefaultClientResources.builder().reconnectDelay(Delay.fullJitter(Duration.ofMillis(100), // minimum 100 millisecond delay
			Duration.ofSeconds(5),      // maximum 5 second delay
			100, TimeUnit.MILLISECONDS) // 100 millisecond base
		).build(); //지수 Backoff Jitter 설정

		RedisClient client = RedisClient.create(resources, REDIS_CON_URL);
		//		client.setOptions(ClientOptions.builder().autoReconnect(true).build());
		//Configure a client-side timeout설정
		client.setOptions(ClientOptions.builder().socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(100)).build()) // 100 millisecond connection timeout
			.timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(5)).build()) // 5 second command timeout
			.build());

		return ConnectionPoolSupport.createGenericObjectPool(() -> client.connect(), createPoolConfig());
	}

	/**
	 * 클러스터모드일때 커넥션 풀 셋팅
	 *  - 참고: https://github.com/Azure/azure-redis-cache-samples/blob/master/Java/ClientSamples/src/main/java/lettuce/PoolUsage.java
	 *
	 * @return
	 */
	private static GenericObjectPool<StatefulRedisClusterConnection<String, String>> useClusterPoolUsage() {

		RedisClusterClient clusterClient = RedisClusterClient.create(REDIS_CON_URL);
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

		poolConfig.setMaxTotal(200);
		poolConfig.setMaxIdle(100);

		// "true" will result better behavior when unexpected load hits in production
		// "false" makes it easier to debug when your maxTotal/minIdle/etc settings need adjusting.
		poolConfig.setBlockWhenExhausted(true);
		poolConfig.setMaxWaitMillis(1000);
		poolConfig.setMinIdle(100);

		return poolConfig;
	}

}
