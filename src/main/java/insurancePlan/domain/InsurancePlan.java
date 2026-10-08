package insurancePlan.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import coverage.domain.Coverage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import member.domain.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "insurance_plans")
public class InsurancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "insurance_plan_id", nullable = false, unique = true)
    private String insurancePlanId;

    @Column(name = "insurance_plan_name", nullable = false)
    private String insurancePlanName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_premium", nullable = false)
    private double monthlyPremium;

    @Column(name = "coverage_limit", nullable = false)
    private double coverageLimit;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(name = "status", nullable = false)
    private String status;

    @JsonManagedReference
    @OneToMany(mappedBy = "insurancePlan", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Coverage> coverages = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "insurancePlan", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Member> members = new ArrayList<>();
}