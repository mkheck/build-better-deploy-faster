package com.thehecklers.weatherservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@SpringBootApplication
public class WeatherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherServiceApplication.class, args);
	}

	@Bean
	WebClient client() {
		return WebClient.create("https://avwx.rest/api");
	}
}

@RestController
@RequestMapping("/")
class WeatherController {
	@Value("${avwx.token:NoValidToken}")
	private String token;
	private final WebClient client;

	WeatherController(WebClient client) {
		this.client = client;
	}

	@GetMapping
	public final Mono<METAR> retrieveDefaultMetar() {
		return retrieveMetar("KSTL");
	}

	@GetMapping("/metar/{id}")
	public final Mono<METAR> retrieveMetar(@PathVariable String id) {
		return client.get()
				.uri("/metar/{id}?token={token}", id, token)
				.retrieve()
				.bodyToMono(METAR.class)
				.log();
	}

	@GetMapping("/taf/{id}")
	public final Mono<TAF> retrieveTaf(@PathVariable String id) {
		return client.get()
				.uri("/taf/{id}?token={token}", id, token)
				.retrieve()
				.bodyToMono(TAF.class)
				.log();
	}
}

record METAR(String raw,
			 Time time,
			 String flight_rules) {}

record TAF(Time start_time, Time end_time,
		   Iterable<Forecast> forecast) {}

record Forecast(String raw,
				Time start_time, Time end_time,
				String flight_rules,
				Visibility visibility,
				WindDirection wind_direction,
				WindSpeed wind_speed,
				Iterable<String> other) {}

record Time(ZonedDateTime dt, String repr) {}

record Visibility(String repr) {}

record WindDirection(String repr) {}

record WindSpeed(String repr) {}