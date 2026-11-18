package fr.efrei.fridgerescue.pantry.domain;

import java.util.UUID;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(UUID id) {
        super("Aucun aliment avec l'id " + id);
    }
}
