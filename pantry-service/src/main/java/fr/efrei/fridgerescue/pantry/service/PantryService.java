package fr.efrei.fridgerescue.pantry.service;

import fr.efrei.fridgerescue.pantry.api.FoodItemRequest;
import fr.efrei.fridgerescue.pantry.api.FoodItemResponse;
import fr.efrei.fridgerescue.pantry.api.PantryStats;
import fr.efrei.fridgerescue.pantry.domain.FoodItem;
import fr.efrei.fridgerescue.pantry.domain.FoodItemRepository;
import fr.efrei.fridgerescue.pantry.domain.Freshness;
import fr.efrei.fridgerescue.pantry.domain.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PantryService {

    private final FoodItemRepository repository;
    private final Clock clock;

    public PantryService(FoodItemRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<FoodItemResponse> list() {
        LocalDate today = today();
        return repository.findAllByOrderByExpiresOnAscNameAsc().stream()
                .map(item -> FoodItemResponse.of(item, today))
                .toList();
    }

    /** Aliments qui périment dans {@code days} jours ou moins (périmés inclus). */
    public List<FoodItemResponse> expiringWithin(int days) {
        LocalDate today = today();
        return repository.findByExpiresOnLessThanEqualOrderByExpiresOnAsc(today.plusDays(days)).stream()
                .map(item -> FoodItemResponse.of(item, today))
                .toList();
    }

    public FoodItemResponse get(UUID id) {
        return FoodItemResponse.of(find(id), today());
    }

    @Transactional
    public FoodItemResponse create(FoodItemRequest request) {
        FoodItem item = new FoodItem(request.name(), request.quantity(), request.unit(),
                request.category(), request.expiresOn());
        return FoodItemResponse.of(repository.save(item), today());
    }

    @Transactional
    public FoodItemResponse update(UUID id, FoodItemRequest request) {
        FoodItem item = find(id);
        item.update(request.name(), request.quantity(), request.unit(), request.category(), request.expiresOn());
        return FoodItemResponse.of(item, today());
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    public PantryStats stats() {
        Map<Freshness, Long> counts = list().stream()
                .collect(Collectors.groupingBy(FoodItemResponse::freshness, Collectors.counting()));
        Function<Freshness, Long> count = f -> counts.getOrDefault(f, 0L);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new PantryStats(total, count.apply(Freshness.EXPIRED), count.apply(Freshness.URGENT),
                count.apply(Freshness.SOON), count.apply(Freshness.FRESH));
    }

    private FoodItem find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
