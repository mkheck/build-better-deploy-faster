package com.thehecklers.airportservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class AirportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirportServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner loadData(AirportService svc, AirportRepository repo) {
		return args -> {
			repo.deleteAll()
					.thenMany(Flux.just("KSTL", "KSUS", "KCPS", "KALN", "KBLV", "KCOU", "KJEF", "KSPI", "KDEC", "KCMI",
							"KMDH", "KMWA", "KCGI", "KTBN"))
					.map(svc::retrieveAirport)
					.flatMap(repo::saveAll)
					.subscribe();
		};
	}

	@Bean
	WebClient client() {
		return WebClient.create("https://avwx.rest/api/station");
	}
}

@RestController
@RequestMapping("/")
class AirportController {
	private final AirportService service;

	public AirportController(AirportService service) {
		this.service = service;
	}

	@GetMapping
	public Flux<Airport> allAirports() {
		return service.getAllAirports().log();
	}

	@GetMapping("/list")
	public Flux<String> airportList() {
		return service.getAllAirports()
				.map(ap -> ap.icao() + ", " + ap.name() + "\n")
				.log();
	}

	@GetMapping("/airport/{id}")
	public Mono<Airport> airportById(@PathVariable String id) {
		return service.getAirportById(id).log();
	}
}

@Service
class AirportService {
	@Value("${avwx.token:NoValidToken}")
	private String token;
	private final WebClient client;
	private final AirportRepository repo;

	public AirportService(WebClient client, AirportRepository repo) {
		this.client = client;
		this.repo = repo;
	}

	public final Flux<Airport> getAllAirports() {
		return repo.findAll();
	}

	public final Mono<Airport> getAirportById(String id) {
		return repo.findById(id);
	}

	public final Mono<Airport> retrieveAirport(String id) {
		return client.get()
				.uri("/{id}?token={token}", id, token)
				.retrieve()
				.bodyToMono(Airport.class);
	}
}

interface AirportRepository extends ReactiveCrudRepository<Airport, String> {}

@Document
record Airport(@Id String icao,
			   String city, String state, String elevation_ft, String name,
			   double latitude, double longitude,
			   Iterable<Runway> runways) {}

record Runway(String ident1, String ident2,
			  int length_ft, int width_ft) {}