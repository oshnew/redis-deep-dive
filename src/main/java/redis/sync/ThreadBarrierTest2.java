package redis.sync;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 멀티스레드의 동시 실행 테스트
 *  - 참고: https://multifrontgarden.tistory.com/266
 *
 * @author
 */
public class ThreadBarrierTest2 {

	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {

		CyclicBarrier cyclicBarrier = new CyclicBarrier(5);
		ExecutorService es = Executors.newFixedThreadPool(4);
		for (int i = 0; i < 4; i++) {
			int n = i;
			es.submit(() -> {
				cyclicBarrier.await();
				System.out.println("order :: " + n);
				return 1;
			});
		}

		Thread.sleep(5000);
		cyclicBarrier.await();
		es.shutdown();
		System.out.println("finish");

	}

}
