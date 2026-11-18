package fr.efrei.fridgerescue.pantry.domain;

/** Statut de fraîcheur calculé à partir du nombre de jours restants. */
public enum Freshness {
    EXPIRED, URGENT, SOON, FRESH;

    /** URGENT : aujourd'hui ou demain ; SOON : sous 4 jours. */
    public static Freshness of(long daysLeft) {
        if (daysLeft < 0) return EXPIRED;
        if (daysLeft <= 1) return URGENT;
        if (daysLeft <= 4) return SOON;
        return FRESH;
    }
}
