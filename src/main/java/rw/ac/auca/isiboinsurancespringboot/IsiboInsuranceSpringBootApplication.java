package rw.ac.auca.isiboinsurancespringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "rw.ac.auca.*")
@EntityScan(basePackages = "rw.ac.auca.*")
@EnableJpaRepositories(basePackages = {"rw.ac.auca.*"})
public class IsiboInsuranceSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(IsiboInsuranceSpringBootApplication.class, args);
    }

}
