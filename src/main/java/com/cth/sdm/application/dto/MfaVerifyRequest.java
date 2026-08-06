package com.cth.sdm.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {
    private String username;
    private String code;
}
