package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import validator.NestedObject;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Person {

    private Long id;

    private String firstname;

    private String lastname;

    private int age;

    @NestedObject
    private Address address;

}
