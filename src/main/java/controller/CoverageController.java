package controller;

import coverage.domain.Coverage;
import coverage.service.CoverageService;
import insurancePlan.domain.InsurancePlan;
import insurancePlan.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("coverage")
public class CoverageController {
    private final CoverageService coverageService;
    @GetMapping("SearchAll")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<Coverage> searchAllInsurance(){
        return coverageService.findAllCoverage();
    }
}
