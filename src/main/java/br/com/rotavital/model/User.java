package br.com.rotavital.model;

import java.util.Locale;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String nome;
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    protected User() {}

    public User(String nome, String email) {
        this.nome = nome.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
}
