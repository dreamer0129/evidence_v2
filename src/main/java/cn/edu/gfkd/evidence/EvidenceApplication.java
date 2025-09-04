package cn.edu.gfkd.evidence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class EvidenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvidenceApplication.class, args);
	}

}
