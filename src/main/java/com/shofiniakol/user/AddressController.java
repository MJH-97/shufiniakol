package com.shofiniakol.user;

import com.shofiniakol.security.SecurityUtils;
import com.shofiniakol.user.AddressService.AddressDto;
import com.shofiniakol.user.AddressService.AddressRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Addresses")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService service;

    @GetMapping
    public List<AddressDto> list() {
        return service.list(SecurityUtils.currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto create(@Valid @RequestBody AddressRequest req) {
        return service.create(SecurityUtils.currentUserId(), req);
    }

    @PutMapping("/{id}")
    public AddressDto update(@PathVariable UUID id, @Valid @RequestBody AddressRequest req) {
        return service.update(SecurityUtils.currentUserId(), id, req);
    }

    @PostMapping("/{id}/primary")
    public AddressDto setPrimary(@PathVariable UUID id) {
        return service.setPrimary(SecurityUtils.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
