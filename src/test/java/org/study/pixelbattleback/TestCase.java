package org.study.pixelbattleback;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.study.pixelbattleback.dto.PixelRequest;
import org.study.pixelbattleback.dto.ResultResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.*;

//--- Первый прогонки ---//
//LongSummaryStatistics{count=10000, sum=12422, min=0, average=1,242200, max=23}
//LongSummaryStatistics{count=10000, sum=12260, min=0, average=1,226000, max=9}
//LongSummaryStatistics{count=10000, sum=11974, min=0, average=1,197400, max=8}
//LongSummaryStatistics{count=10000, sum=12154, min=0, average=1,215400, max=11}
//LongSummaryStatistics{count=10000, sum=14754, min=0, average=1,475400, max=27}

/////итог
//LongSummaryStatistics{count=10000, sum=10095, min=0, average=1,009500, max=6}
//LongSummaryStatistics{count=10000, sum=9896, min=0, average=0,989600, max=4}
//LongSummaryStatistics{count=10000, sum=9944, min=0, average=0,994400, max=5}
//LongSummaryStatistics{count=10000, sum=10021, min=0, average=1,002100, max=5}
//LongSummaryStatistics{count=10000, sum=9973, min=0, average=0,997300, max=13}

public class TestCase {
    private final static Logger logger = LoggerFactory.getLogger(TestCase.class);

    private static final int THREAD_COUNT = 100;

    private static final int REQUEST_COUNT = 100;

    public static final String URL = "http://127.0.0.1:8080/drawPixel";

    @Test
    public void testParallel() {
        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<?>> futureList = new ArrayList<>();
        for (int threadNumber = 0; threadNumber < THREAD_COUNT; ++threadNumber) {
            int threadColor = ThreadLocalRandom.current().nextInt(1, 255 << 17);
            final RestTemplate rest = new RestTemplate();

            warn(rest, threadColor);

            futureList.add(pool.submit(() -> {
                PixelRequest pixel = new PixelRequest();
                pixel.setColor(threadColor);
                StopWatch stopWatch = new StopWatch();
                LongSummaryStatistics stats = new LongSummaryStatistics();
                for (int i = 0; i < REQUEST_COUNT; i++) {
                    pixel.setX(ThreadLocalRandom.current().nextInt(0, 100));
                    pixel.setY(ThreadLocalRandom.current().nextInt(0, 100));
                    stopWatch.start();
                    try {
                        ResultResponse res = rest.postForObject(URL, pixel, ResultResponse.class);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    stopWatch.stop();
                    stats.accept(stopWatch.getLastTaskTimeMillis());
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 100));
                }
                return stats;
            }));
        }
        LongSummaryStatistics stats = new LongSummaryStatistics();
        for (Future<?> voidFuture : futureList) {
            try {
                stats.combine((LongSummaryStatistics) voidFuture.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(stats);
    }

    private void warn(RestTemplate rest, int threadColor) {
        PixelRequest pixel = new PixelRequest();
        pixel.setColor(threadColor);
        for (int i = 0; i < REQUEST_COUNT; i++) {
            pixel.setX(ThreadLocalRandom.current().nextInt(0, 100));
            pixel.setY(ThreadLocalRandom.current().nextInt(0, 100));
            rest.postForObject(URL, pixel, ResultResponse.class);
        }
    }
}
