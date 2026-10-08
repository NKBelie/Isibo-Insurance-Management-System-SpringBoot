package claim.controller;

import claim.domain.Claim;
import claim.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<Claim> create(@RequestBody Claim claim) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(claim));
    }

    @GetMapping
    public List<Claim> findAll() {
        return claimService.findAll();
    }

    @GetMapping("/{id}")
    public Claim findById(@PathVariable UUID id) {
        return claimService.findById(id);
    }

    @PutMapping("/{id}")
    public Claim update(@PathVariable UUID id, @RequestBody Claim claim) {
        return claimService.update(id, claim);
    }

    @PostMapping("/{id}/decision")
    public Claim review(@PathVariable UUID id,
                        @RequestParam String status,
                        @RequestParam UUID officerId) {
        return claimService.review(id, status, officerId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        claimService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
