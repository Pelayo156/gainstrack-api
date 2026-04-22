package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.GymResponse;
import com.molina.gainstrack.api.service.GymService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gyms")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) { this.gymService = gymService; }

    @PostMapping("/save")
    public ResponseEntity<GymResponse> save(@RequestBody Long userId, String name) {
        return ResponseEntity.status(201).body(gymService.save(userId, name));
    }

    @GetMapping
    public ResponseEntity<List<GymResponse>> findAll(@RequestBody Long userId) {
        return ResponseEntity.ok(gymService.findAll(userId));
    }

    @PostMapping("/set-primary/{id}")
    public ResponseEntity<Void> setPrimary(@PathVariable("id") Long id, @RequestBody Long userId) {
        gymService.setPrimary(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        gymService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
