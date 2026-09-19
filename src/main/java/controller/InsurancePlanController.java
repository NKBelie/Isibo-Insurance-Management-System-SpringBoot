package controller;

import insurancePlan.domain.InsurancePlan;
import insurancePlan.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("insurancePlan")
public class InsurancePlanController {
    private final InsuranceService insuranceService;
    @GetMapping("SearchAll")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<InsurancePlan> searchAllInsurance(){
        return insuranceService.findAllInsurancePlans();
    }
}
