package com.molina.gainstrack.api.controller;

import com.molina.gainstrack.api.service.GymService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gyms")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) { this.gymService = gymService; }
}
