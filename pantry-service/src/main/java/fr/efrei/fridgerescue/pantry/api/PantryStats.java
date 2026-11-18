package fr.efrei.fridgerescue.pantry.api;

/** Compteurs affichés en tête du tableau de bord. */
public record PantryStats(long total, long expired, long urgent, long soon, long fresh) {
}
