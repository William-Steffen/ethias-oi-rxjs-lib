package model;

import lombok.*;
import javax.persistence.Column;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class Employee extends Person{

    @Column
    private String employeeNumber;

    @Column
    private boolean isActive;

    @Builder
    public Employee(Long id, String firstname, String lastname, int age, String employeeNumber, boolean isActive, Address address) {
        super(id, firstname, lastname, age, address);
        this.employeeNumber = employeeNumber;
        this.isActive = isActive;
    }
}
