package claim.domain;

import healthcareProvider.domain.HealthcareProvider;
import insuranceOfficer.domain.InsuranceOfficer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import member.domain.Member;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claim_id", nullable = false, unique = true)
    private String claimId;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount_claimed", nullable = false)
    private double amountClaimed;

    @Column(nullable = false)
    private String status;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_national_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "healthcare_provider_id", nullable = false)
    private HealthcareProvider healthcareProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_officer_id")
    private InsuranceOfficer insuranceOfficer;
}
