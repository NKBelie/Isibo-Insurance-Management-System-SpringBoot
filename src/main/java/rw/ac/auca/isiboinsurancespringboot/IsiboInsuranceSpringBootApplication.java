package rw.ac.auca.isiboinsurancespringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"claim", "controller", "config", "coverage", "healthcareProvider", "insuranceOfficer", "insurancePlan", "member", "payment", "user", "base", "exception", "rw.ac.auca.isiboinsurancespringboot"})
@EntityScan(basePackages = {"claim", "coverage", "healthcareProvider", "insuranceOfficer", "insurancePlan", "member", "payment", "user"})
@EnableJpaRepositories(basePackageClasses = {
        claim.repository.ClaimRepository.class,
        coverage.repository.CoverageRepository.class,
        healthcareProvider.repository.HealthcareProviderRepository.class,
        insuranceOfficer.repository.InsuranceOfficerRepository.class,
        insurancePlan.repository.InsurancePlanRepository.class,
        member.repository.MemberRepository.class,
        payment.repository.PaymentRepository.class,
        user.repository.UserRepository.class
})
public class IsiboInsuranceSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(IsiboInsuranceSpringBootApplication.class, args);
    }
}
