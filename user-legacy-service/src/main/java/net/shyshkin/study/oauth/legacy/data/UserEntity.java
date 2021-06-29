package net.shyshkin.study.oauth.legacy.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity implements Serializable {

    private static final long serialVersionUID = 5313493413859894403L;

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Column(nullable = false)
    private String encryptedPassword;

    private String emailVerificationToken;

    @Column(nullable = false)
    private Boolean emailVerificationStatus = false;

    @Column(nullable = false)
    private String roles;

}
