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

    // Nullable para preservar cadastros anteriores, que não podem autenticar sem senha.
    @Column(length = 100)
    private String senhaHash;

    public String getSenhaHash() { return senhaHash; }

    public User(String nome, String email, String senhaHash) {
        this(nome, email);
        this.senhaHash = java.util.Objects.requireNonNull(senhaHash);
    }

    protected User() {}

    public User(String nome, String email) {
        this.nome = nome.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
}
