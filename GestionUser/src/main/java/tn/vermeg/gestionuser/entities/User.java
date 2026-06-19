package tn.vermeg.gestionuser.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "users")
@TypeAlias("user")
public class User {

    @Id
    private String idUser;
    private String username;
    private String email;
    private String password;
    private Integer phone;
    @CreatedDate
    private Date dateCreation;
    private String departement;

    public User() {
    }

    public User(String idUser, String username, String email, String password, Integer phone, Date dateCreation, String departement) {
        this.idUser = idUser;
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.dateCreation = dateCreation;
        this.departement = departement;
    }

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Integer getPhone() { return phone; }
    public void setPhone(Integer phone) { this.phone = phone; }
    
    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public boolean estValide() {
        return username != null && !username.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
    }
}