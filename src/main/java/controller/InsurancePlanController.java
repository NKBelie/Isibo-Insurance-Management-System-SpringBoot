package controller;

import insurancePlan.domain.InsurancePlan;
import insurancePlan.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping({"/api/insurance-plans", "/insurancePlan"})
public class InsurancePlanController {

    private final InsuranceService insuranceService;

    @PostMapping
    public ResponseEntity<InsurancePlan> createInsurancePlan(@RequestBody InsurancePlan insurancePlan) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(insuranceService.registerInsurance(insurancePlan));
    }

    @GetMapping
    public List<InsurancePlan> getAllInsurancePlans() {
        return insuranceService.findAllInsurancePlans();
    }

    @GetMapping("/{id}")
    public InsurancePlan getInsurancePlanById(@PathVariable UUID id) {
        return insuranceService.findInsuranceById(id);
    }

    @PutMapping("/{id}")
    public InsurancePlan updateInsurancePlan(@PathVariable UUID id, @RequestBody InsurancePlan insurancePlan) {
        return insuranceService.updateInsurance(id, insurancePlan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsurancePlan(@PathVariable UUID id) {
        insuranceService.deleteInsurance(id);
        return ResponseEntity.ok().build();
    }
}