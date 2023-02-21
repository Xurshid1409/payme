package uz.payme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name")
    private String fullName;

    private String pinfl;
    @Column(name = "given_date")
    private String givenDate;
    private String gender;

    @Column(name = "birth_date")
    private String birthDate;
    private String citizenship;
    private String nationality;
    @Column(name = "permanent_address", length = 2048)
    private String permanentAddress;
    private String district;
    private String region;

    @Column(length = 2048)
    private String position;
    @Column(name = "code_password", length = 512)
    private String codePassword;

    //bo'limi
    @Column(name = "dep_name", length = 2048)
    private String depName;

    @Column(length = 2048)
    private String organization;

    @Column(name = "serial_and_number", length = 15)
    private String serialAndNumber;

    @Column(name = "phone_number", length = 14)
    private String phoneNumber;

    @Column(length = 1024)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "FK_USER_ROLE"))
    private Role role;

}