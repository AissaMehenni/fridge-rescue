package fr.efrei.fridgerescue.pantry.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un aliment présent dans le frigo, avec sa date limite de consommation. */
@Entity
@Table(name = "food_item")
public class FoodItem {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 16)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FoodItem() {
        // JPA
    }

    public FoodItem(String name, BigDecimal quantity, String unit, Category category, LocalDate expiresOn) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        update(name, quantity, unit, category, expiresOn);
    }

    public final void update(String name, BigDecimal quantity, String unit, Category category, LocalDate expiresOn) {
        this.name = name.trim();
        this.quantity = quantity;
        this.unit = unit.trim();
        this.category = category;
        this.expiresOn = expiresOn;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Category getCategory() { return category; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public Instant getCreatedAt() { return createdAt; }
}
