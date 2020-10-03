package uy.com.pepeganga.productsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@SpringBootApplication
@EnableDiscoveryClient
@EntityScan({"uy.com.pepeganga.business.common.entities"})
public class ProductsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductsServiceApplication.class, args);
	}


}
