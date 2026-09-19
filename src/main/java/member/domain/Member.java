package member.domain;

import base.BaseEntity;
import insurancePlan.domain.InsurancePlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "members")
public class Member extends BaseEntity {

    @Id
    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;

    @Column(name = "member_id", nullable = false, unique = true)
    private String memberId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "insurance_plan_id",
            nullable = false
    )
    private InsurancePlan insurancePlan;
}