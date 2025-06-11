package com.czdxwx.aiinterviewcoachbackend.service;

import com.czdxwx.aiinterviewcoachbackend.entity.User;
import com.czdxwx.aiinterviewcoachbackend.service.dto.AuthResponseDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.LoginRequestDto;
import com.czdxwx.aiinterviewcoachbackend.service.dto.RegisterRequestDto;

public interface AuthService {
    User register(RegisterRequestDto registerRequestDto);
    AuthResponseDto login(LoginRequestDto loginRequestDto);
}