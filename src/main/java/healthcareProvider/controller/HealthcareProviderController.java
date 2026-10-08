package healthcareProvider.controller;

import healthcareProvider.domain.HealthcareProvider;
import healthcareProvider.service.HealthcareProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class HealthcareProviderController {

    private final HealthcareProviderService providerService;

    @PostMapping
    public ResponseEntity<HealthcareProvider> create(@RequestBody HealthcareProvider provider) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(provider));
    }

    @GetMapping
    public List<HealthcareProvider> findAll() {
        return providerService.findAll();
    }

    @GetMapping("/{id}")
    public HealthcareProvider findById(@PathVariable UUID id) {
        return providerService.findById(id);
    }

    @PutMapping("/{id}")
    public HealthcareProvider update(@PathVariable UUID id, @RequestBody HealthcareProvider provider) {
        return providerService.update(id, provider);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        providerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
