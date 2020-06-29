/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.consumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.dubbo.demo.DemoService;

import org.apache.dubbo.rpc.RpcContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Application {

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(
                    1,
                    new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("failAfter-%d")
                            .build());

    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-consumer.xml");
        context.start();
        DemoService demoService = context.getBean("demoService", DemoService.class);
//        CompletableFuture<String> timeout = failAfter(Duration.ofMillis(500));
        long start = System.currentTimeMillis();
        System.out.println(start);
//        CompletableFuture<String> hello = demoService.sayHelloAsync("world")
//                .whenComplete((str, e) -> System.out.println("str0 :" + str + "  cost: " + (System.currentTimeMillis() - start)));
//        CompletableFuture<String> world = CompletableFuture.supplyAsync(() -> demoService.sayHello("world"))
//                .whenComplete((str, e) -> System.out.println("str: " + str + "  cost: " + (System.currentTimeMillis() - start)));
//        demoService.sayHello("world2");
//        Future<String> world2 = RpcContext.getContext().getFuture();

        CompletableFuture<Void> hello = within(demoService.sayHelloAsync("world"), Duration.ofMillis(500))
                .thenAccept(Application::s)
                .exceptionally(degrade());

        CompletableFuture<Void> world = within(CompletableFuture.supplyAsync(() -> demoService.sayHello("world")), Duration.ofMillis(500))
                .thenAccept(Application::s)
                .exceptionally(degrade());

        CompletableFuture.allOf(hello, world).join();
        System.out.println("=======end========cost: " + (System.currentTimeMillis() - start));
    }

    public static Function<Throwable, Void> degrade() {
        return throwable -> {
            System.out.println("==========0============");
            System.out.println(System.currentTimeMillis());
            System.out.println(throwable.getMessage());
            return null;
        };
    }

    public static void s(String str) {
        System.out.println("str: " + str + "  cost: ");
    }


    public static <T> CompletableFuture<T> failAfter(Duration duration) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        scheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            return promise.completeExceptionally(ex);
        }, duration.toMillis(), MILLISECONDS);
        return promise;
    }

    public static <T> CompletableFuture<T> within(CompletableFuture<T> future, Duration duration) {
        final CompletableFuture<T> timeout = failAfter(duration);
        return future.applyToEither(timeout, Function.identity());
    }
}
