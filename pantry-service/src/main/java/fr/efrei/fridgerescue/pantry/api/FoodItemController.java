package fr.efrei.fridgerescue.pantry.api;

import fr.efrei.fridgerescue.pantry.service.PantryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@Validated
public class FoodItemController {

    private final PantryService pantry;

    public FoodItemController(PantryService pantry) {
        this.pantry = pantry;
    }

    @GetMapping
    public List<FoodItemResponse> list() {
        return pantry.list();
    }

    @GetMapping("/expiring")
    public List<FoodItemResponse> expiring(@RequestParam(defaultValue = "3") @Min(0) @Max(365) int days) {
        return pantry.expiringWithin(days);
    }

    @GetMapping("/stats")
    public PantryStats stats() {
        return pantry.stats();
    }

    @GetMapping("/{id}")
    public FoodItemResponse get(@PathVariable UUID id) {
        return pantry.get(id);
    }

    @PostMapping
    public ResponseEntity<FoodItemResponse> create(@Valid @RequestBody FoodItemRequest request) {
        FoodItemResponse created = pantry.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public FoodItemResponse update(@PathVariable UUID id, @Valid @RequestBody FoodItemRequest request) {
        return pantry.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        pantry.delete(id);
        return ResponseEntity.noContent().build();
    }
}
