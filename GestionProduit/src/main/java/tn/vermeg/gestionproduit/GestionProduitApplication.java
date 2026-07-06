package tn.vermeg.gestionproduit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GestionProduitApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionProduitApplication.class, args);
	}

}