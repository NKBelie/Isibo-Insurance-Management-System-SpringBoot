package controller;

import coverage.domain.Coverage;
import coverage.service.CoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/coverage")
public class CoverageController {

    private final CoverageService coverageService;

    // CREATE
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Coverage registerCoverage(
            @RequestBody Coverage coverage) {

        return coverageService.registerCoverage(coverage);
    }

    // READ ALL
    @GetMapping("/searchAll")
    @ResponseStatus(HttpStatus.OK)
    public List<Coverage> searchAllCoverage() {

        return coverageService.findAllCoverage();
    }

    // READ ONE
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Coverage searchCoverageById(
            @PathVariable UUID id) {

        Coverage coverage = new Coverage();
        coverage.setId(id);

        return coverageService.findAllCoverageById(coverage);
    }

    // UPDATE
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Coverage updateCoverage(
            @PathVariable UUID id,
            @RequestBody Coverage coverage) {

        coverage.setId(id);

        return coverageService.updateCoverage(coverage);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Coverage deleteCoverage(
            @PathVariable UUID id) {

        Coverage coverage = new Coverage();
        coverage.setId(id);

        return coverageService.deleteCoverage(coverage);
    }
}