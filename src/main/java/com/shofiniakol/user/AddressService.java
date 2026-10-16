package com.shofiniakol.user;

import com.shofiniakol.common.ApiException;
import com.shofiniakol.common.GeoUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository repo;

    public record AddressRequest(
            @NotNull(message = "اختر نوع العنوان") AddressType type,
            @Size(max = 60) String label,
            @NotBlank(message = "اكتب المدينة") @Size(max = 80) String city,
            @NotBlank(message = "اكتب المنطقة") @Size(max = 80) String area,
            @Size(max = 160) String street,
            @Size(max = 40) String building,
            @Size(max = 20) String floor,
            @Size(max = 20) String apartment,
            @Size(max = 200) String landmark,
            Double latitude, Double longitude, Boolean primary) {
    }

    public record AddressDto(UUID id, AddressType type, String label, String city, String area, String street,
                             String building, String floor, String apartment, String landmark,
                             Double latitude, Double longitude, boolean primary, String displayText) {
        static AddressDto from(Address a) {
            return new AddressDto(a.getId(), a.getType(), a.getLabel(), a.getCity(), a.getArea(), a.getStreet(),
                    a.getBuilding(), a.getFloor(), a.getApartment(), a.getLandmark(), a.getLatitude(),
                    a.getLongitude(), a.isPrimary(), a.toDisplayText());
        }
    }

    @Transactional(readOnly = true)
    public List<AddressDto> list(UUID userId) {
        return repo.findByUserIdOrderByPrimaryDescCreatedAtDesc(userId).stream().map(AddressDto::from).toList();
    }

    @Transactional
    public AddressDto create(UUID userId, AddressRequest req) {
        Address a = new Address();
        a.setUserId(userId);
        apply(a, req);
        boolean first = repo.countByUserId(userId) == 0;
        if (first || Boolean.TRUE.equals(req.primary())) {
            repo.clearPrimary(userId);
            a.setPrimary(true);
        }
        return AddressDto.from(repo.save(a));
    }

    @Transactional
    public AddressDto update(UUID userId, UUID id, AddressRequest req) {
        Address a = find(userId, id);
        apply(a, req);
        if (Boolean.TRUE.equals(req.primary()) && !a.isPrimary()) {
            repo.clearPrimary(userId);
            a.setPrimary(true);
        }
        return AddressDto.from(a);
    }

    @Transactional
    public AddressDto setPrimary(UUID userId, UUID id) {
        Address a = find(userId, id);
        if (!a.isPrimary()) { // the bulk clear below bypasses the persistence context
            repo.clearPrimary(userId);
            a.setPrimary(true);
        }
        return AddressDto.from(a);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Address a = find(userId, id);
        boolean wasPrimary = a.isPrimary();
        repo.delete(a);
        repo.flush();
        if (wasPrimary) {
            repo.findByUserIdOrderByPrimaryDescCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(next -> next.setPrimary(true));
        }
    }

    private Address find(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("ADDRESS_NOT_FOUND", "العنوان غير موجود"));
    }

    private void apply(Address a, AddressRequest r) {
        if ((r.latitude() == null) != (r.longitude() == null)
                || (r.latitude() != null && !GeoUtils.validLatLng(r.latitude(), r.longitude()))) {
            throw ApiException.badRequest("INVALID_LOCATION", "إحداثيات الموقع غير صحيحة");
        }
        a.setType(r.type());
        a.setLabel(blank(r.label()));
        a.setCity(r.city().trim());
        a.setArea(r.area().trim());
        a.setStreet(blank(r.street()));
        a.setBuilding(blank(r.building()));
        a.setFloor(blank(r.floor()));
        a.setApartment(blank(r.apartment()));
        a.setLandmark(blank(r.landmark()));
        a.setLatitude(r.latitude());
        a.setLongitude(r.longitude());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
