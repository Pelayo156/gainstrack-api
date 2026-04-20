package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.dto.MuscleGroupResponse;
import com.molina.gainstrack.api.service.MuscleGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/muscle-groups")
public class MuscleGroupController {

    private final MuscleGroupService muscleGroupService;

    public MuscleGroupController(MuscleGroupService muscleGroupService) {
        this.muscleGroupService = muscleGroupService;
    }

     @GetMapping
    public ResponseEntity<List<MuscleGroupResponse>> findAll() {
        return ResponseEntity.ok(muscleGroupService.findAll());
    }
}
