package com.cth.sdm.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private boolean mfaRequired;
    private String mfaType;
    private String message;
}
