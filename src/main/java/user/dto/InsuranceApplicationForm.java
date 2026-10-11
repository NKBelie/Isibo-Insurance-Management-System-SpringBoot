package user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class InsuranceApplicationForm {

    private String firstName;
    private String lastName;
    private String nationalId;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private UUID insurancePlanId;
}
