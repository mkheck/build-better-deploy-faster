package com.thehecklers.conditionsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;

@SpringBootApplication
public class ConditionsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConditionsServiceApplication.class, args);
	}

	@Bean
	@LoadBalanced
	WebClient.Builder loadBalancedBuilder() {
		return WebClient.builder();
	}

/*	// Option 2
	@Bean
	WebClient apClient(WebClient.Builder builder) {
		return builder.baseUrl("http://airport-service").build();
	}

	@Bean
	WebClient wxClient(WebClient.Builder builder) {
		return builder.baseUrl("http://weather-service").build();
	}*/
}

@RestController
@RequestMapping("/")
class ConditionsController {
	private final WebClient.Builder builder;

	private WebClient apClient;
	private WebClient wxClient;

	ConditionsController(WebClient.Builder builder) {
		this.builder = builder;

		apClient = builder.baseUrl("http://airport-service").build();
		wxClient = builder.baseUrl("http://weather-service").build();
	}

/*	// Option 2
	private final WebClient apClient;
	private final WebClient wxClient;

	ConditionsController(WebClient apClient, WebClient wxClient) {
		this.apClient = apClient;
		this.wxClient = wxClient;
	}*/

	@GetMapping
	public String greeting() {
		return "Greetings and salutations, everyone!!";
	}

	@GetMapping("/summary")
	public Flux<METAR> getConditionsSummary() {
		return apClient.get()
				.retrieve()
				.bodyToFlux(Airport.class)
				.flatMap(ap -> wxClient.get()
						.uri("/metar/{icao}", ap.icao())
						.retrieve()
						.bodyToMono(METAR.class))
				.log();
	}
}

record Airport(String icao) {}

record METAR(String raw, Time time, String flight_rules) {}

record Time(ZonedDateTime dt, String repr) {}