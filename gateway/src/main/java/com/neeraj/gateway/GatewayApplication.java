package com.neeraj.gateway;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@EnableCircuitBreaker
@EnableHystrix
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Rate Limiting
                .route("fortune_api", r -> r.host("api.fortunes.io")
                        .filters(f -> f.requestRateLimiter().rateLimiter(RedisRateLimiter.class, c -> c.setBurstCapacity(1).setReplenishRate(1))
                                .configure(c -> c.setKeyResolver(exchange -> {
                                    return Mono.just(exchange.getRequest().getHeaders().getFirst("X-API-KEY"));
                                })))
                        .uri("lb://fortune"))
                // Showing Circuit Breaker with Netflix Hystrix
                .route("fortune", r -> r.path("/service/randomfortune")
                        .filters(f -> f.hystrix(config -> config.setName("fortune")
                                .setFallbackUri("forward:/defaultFortune")))
                        .uri("http://localhost:8081"))
                // Showing How we can filter the request and convert queryParam to PathParam
                .route("hello", r -> r.path("/service/hello")
                        .filters(f -> f.filter((exchange, chain) -> {
                            String name = exchange.getRequest().getQueryParams().getFirst("name");
                            ServerHttpRequest request = exchange.getRequest().mutate()
                                    .path("/hello/" + name)
                                    .build();
                            return chain.filter(exchange.mutate().request(request).build());
                        }))
                        .uri("lb://hello"))
                // How we can add the routes based on multiple predicates
                .route("ui", r -> r.path("/css/**")
                        .or().path("/js/**")
                        .or().path("/")
//                        .uri("http://localhost:8082"))
                        .uri("lb://ui")) // Netflix Ribbon provides client side load balancing, hence we need not hardcode the hostname.
                // Simply re-routing the request.
                .route("monolith", r -> r.path("/**")
//                        .uri("http://localhost:8081"))
                        .uri("lb://monolith"))
                .build();
    }

    @GetMapping("/defaultFortune")
    public String defaultFortune() {
        return "Default Fortune";
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
