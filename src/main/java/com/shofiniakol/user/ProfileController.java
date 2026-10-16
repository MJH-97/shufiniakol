package com.shofiniakol.user;

import com.shofiniakol.security.SecurityUtils;
import com.shofiniakol.user.UserService.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService service;

    @GetMapping
    public UserDto me() {
        return service.get(SecurityUtils.currentUserId());
    }

    @PutMapping
    public UserDto update(@Valid @RequestBody UpdateProfileRequest req) {
        return service.update(SecurityUtils.currentUserId(), req);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        service.deleteAccount(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
