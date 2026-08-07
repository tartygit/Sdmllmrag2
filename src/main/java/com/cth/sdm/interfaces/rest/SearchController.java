package com.cth.sdm.interfaces.rest;

import com.cth.sdm.application.service.SearchService;
import com.cth.sdm.domain.model.DocumentIndex;
import com.cth.sdm.infrastructure.logging.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Search Services", description = "Endpoints for full-text and structured multi-field document search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    @AuditLog(action = "DOCUMENT_SEARCH")
    @Operation(summary = "Search and Filter Documents", description = "Query documents across full-text keywords and filter dynamically by SDLC phase, application code, owner, and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully executed search query")
    })
    public ResponseEntity<List<DocumentIndex>> search(
            @RequestParam(value = "query", required = false)
            @Parameter(description = "Full-text search keyword matching document title, description, or content") String query,

            @RequestParam(value = "phaseCode", required = false)
            @Parameter(description = "SDLC Phase Code (e.g., REQ, DES, DEV)") String phaseCode,

            @RequestParam(value = "appCode", required = false)
            @Parameter(description = "Application code prefix matching the generated document number") String appCode,

            @RequestParam(value = "owner", required = false)
            @Parameter(description = "Document owner username") String owner,

            @RequestParam(value = "status", required = false)
            @Parameter(description = "Document approval status (DRAFT, APPROVED, etc.)") String status
    ) {
        log.info("Received document search request: query={}, phaseCode={}, appCode={}, owner={}, status={}",
                query, phaseCode, appCode, owner, status);

        List<DocumentIndex> results = searchService.search(query, phaseCode, appCode, owner, status);
        return ResponseEntity.ok(results);
    }
}
