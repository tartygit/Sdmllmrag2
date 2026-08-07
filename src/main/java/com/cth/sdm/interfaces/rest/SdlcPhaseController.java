package com.cth.sdm.interfaces.rest;

import com.cth.sdm.application.dto.SdlcPhaseDto;
import com.cth.sdm.application.service.SdlcPhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sdlc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SDLC Phase Management", description = "Endpoints to manage and track SDLC phases and associated deliverables")
public class SdlcPhaseController {

    private final SdlcPhaseService sdlcPhaseService;

    @GetMapping("/phases")
    @Operation(summary = "Get all SDLC Phases", description = "Retrieve a compiled overview of all 7 SDLC phases including their deliverables, owner, and approval status mappings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved phases overview")
    })
    public ResponseEntity<List<SdlcPhaseDto>> getAllPhases() {
        return ResponseEntity.ok(sdlcPhaseService.getAllPhases());
    }

    @GetMapping("/phases/{code}")
    @Operation(summary = "Get SDLC Phase by Code", description = "Retrieve mapped deliverables for a single SDLC phase by code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved phase details"),
            @ApiResponse(responseCode = "400", description = "Invalid phase code specified")
    })
    public ResponseEntity<SdlcPhaseDto> getPhaseByCode(@PathVariable("code") String code) {
        try {
            return ResponseEntity.ok(sdlcPhaseService.getPhaseByCode(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
