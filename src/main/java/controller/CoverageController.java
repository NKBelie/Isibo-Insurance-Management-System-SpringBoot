package controller;

import coverage.domain.Coverage;
import coverage.service.CoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping({"/api/coverage", "/coverage"})
public class CoverageController {

    private final CoverageService coverageService;

    @PostMapping
    public ResponseEntity<Coverage> createCoverage(@RequestBody Coverage coverage) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coverageService.registerCoverage(coverage));
    }

    @GetMapping
    public List<Coverage> getAllCoverage() {
        return coverageService.findAllCoverage();
    }

    @GetMapping("/{id}")
    public Coverage getCoverageById(@PathVariable UUID id) {
        return coverageService.findCoverageById(id);
    }

    @PutMapping("/{id}")
    public Coverage updateCoverage(@PathVariable UUID id, @RequestBody Coverage coverage) {
        return coverageService.updateCoverage(id, coverage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoverage(@PathVariable UUID id) {
        coverageService.deleteCoverage(id);
        return ResponseEntity.ok().build();
    }
}