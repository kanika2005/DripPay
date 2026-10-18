package com.drippay.collect;

import com.drippay.dto.collect.CollectProfileResponse;
import com.drippay.dto.collect.HandleLookupResponse;
import com.drippay.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collect")
public class CollectController {

    private final CollectService collectService;

    public CollectController(CollectService collectService) {
        this.collectService = collectService;
    }

    @GetMapping("/me")
    public CollectProfileResponse me() {
        return collectService.myCollectProfile(SecurityUtils.currentUser().getId());
    }

    @PutMapping("/me/handle")
    public CollectProfileResponse claim(@RequestBody ClaimHandleBody body) {
        return collectService.claimHandle(SecurityUtils.currentUser().getId(), body.handle());
    }

    @GetMapping("/resolve/{handle}")
    public HandleLookupResponse resolve(@PathVariable String handle) {
        return collectService.resolveHandle(handle, SecurityUtils.currentUser().getId());
    }

    public record ClaimHandleBody(@NotBlank String handle) {
    }
}
