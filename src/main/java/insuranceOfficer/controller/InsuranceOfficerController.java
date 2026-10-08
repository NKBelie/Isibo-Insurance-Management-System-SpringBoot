package insuranceOfficer.controller;

import insuranceOfficer.domain.InsuranceOfficer;
import insuranceOfficer.service.InsuranceOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/officers")
@RequiredArgsConstructor
public class InsuranceOfficerController {

    private final InsuranceOfficerService officerService;

    @PostMapping
    public ResponseEntity<InsuranceOfficer> create(@RequestBody InsuranceOfficer officer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officerService.create(officer));
    }

    @GetMapping
    public List<InsuranceOfficer> findAll() {
        return officerService.findAll();
    }

    @GetMapping("/{id}")
    public InsuranceOfficer findById(@PathVariable UUID id) {
        return officerService.findById(id);
    }

    @PutMapping("/{id}")
    public InsuranceOfficer update(@PathVariable UUID id, @RequestBody InsuranceOfficer officer) {
        return officerService.update(id, officer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        officerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
