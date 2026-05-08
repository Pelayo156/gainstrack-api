package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.routine.RoutineDetailResponse;
import com.molina.gainstrack.api.dto.routine.RoutineRequest;
import com.molina.gainstrack.api.dto.routine.RoutineSummaryResponse;
import com.molina.gainstrack.api.service.RoutineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping
    public ResponseEntity<List<RoutineSummaryResponse>> findAll() {
        return ResponseEntity.ok().body(this.routineService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoutineDetailResponse> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(this.routineService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoutineSummaryResponse> save(@RequestBody RoutineRequest request) {
        return ResponseEntity.status(201).body(this.routineService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @RequestBody RoutineRequest request) {
        this.routineService.update(id, request);
        return ResponseEntity.noContent().build();
    }
}
