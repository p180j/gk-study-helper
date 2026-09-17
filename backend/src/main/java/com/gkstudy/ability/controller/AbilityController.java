package com.gkstudy.ability.controller;

import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ability.dto.AbilityOverview;
import com.gkstudy.ability.service.AbilityReplayService;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/abilities")
public class AbilityController {
    private final AbilityService abilityService;
    private final AbilityReplayService replayService;

    public AbilityController(AbilityService abilityService, AbilityReplayService replayService) {
        this.abilityService = abilityService; this.replayService = replayService;
    }

    @GetMapping
    public ApiResponse<List<AbilityProfile>> profiles(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(abilityService.profiles(userId));
    }

    @GetMapping("/overview")
    public ApiResponse<AbilityOverview> overview(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(abilityService.overview(userId));
    }

    @PostMapping("/replay/{userId}")
    public ApiResponse<List<AbilityProfile>> replay(@PathVariable Long userId) { return ApiResponse.success(replayService.replay(userId)); }
}
