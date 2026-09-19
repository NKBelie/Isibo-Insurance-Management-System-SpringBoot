package coverage.domain;

import insurancePlan.domain.InsurancePlan;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coverages")
public class Coverage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "coverage_id", nullable = false, unique = true)
    private String coverageId;

    @Column(name = "description", nullable = false, unique = true)
    private String description;

    @Column(name = "coverage_amount", nullable = false)
    private double coverageAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "insurance_plan_id",
            nullable = false
    )
    private InsurancePlan insurancePlan;
}