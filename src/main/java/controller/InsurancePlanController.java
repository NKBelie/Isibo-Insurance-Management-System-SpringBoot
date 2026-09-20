package controller;

import insurancePlan.domain.InsurancePlan;
import insurancePlan.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/insurancePlan")
public class InsurancePlanController {

    private final InsuranceService insuranceService;

    // CREATE
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public InsurancePlan registerInsurance(
            @RequestBody InsurancePlan insurancePlan) {

        return insuranceService.registerInsurance(insurancePlan);
    }

    // READ ALL
    @GetMapping("/searchAll")
    @ResponseStatus(HttpStatus.OK)
    public List<InsurancePlan> searchAllInsurance() {

        return insuranceService.findAllInsurancePlans();
    }

    // READ ONE
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InsurancePlan searchInsuranceById(
            @PathVariable UUID id) {

        InsurancePlan insurancePlan = new InsurancePlan();
        insurancePlan.setId(id);

        return insuranceService.findInsuranceById(insurancePlan);
    }

    // UPDATE
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InsurancePlan updateInsurance(
            @PathVariable UUID id,
            @RequestBody InsurancePlan insurancePlan) {

        insurancePlan.setId(id);

        return insuranceService.updateInsurance(insurancePlan);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InsurancePlan deleteInsurance(
            @PathVariable UUID id) {

        InsurancePlan insurancePlan = new InsurancePlan();
        insurancePlan.setId(id);

        return insuranceService.deleteInsurance(insurancePlan);
    }
}