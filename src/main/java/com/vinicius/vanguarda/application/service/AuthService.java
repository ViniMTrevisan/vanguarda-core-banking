package com.vinicius.vanguarda.application.service;

import com.vinicius.vanguarda.domain.entity.User;
import com.vinicius.vanguarda.domain.repository.UserRepository;
import com.vinicius.vanguarda.domain.usecase.LoginUserUseCase;
import com.vinicius.vanguarda.domain.usecase.RegisterUserUseCase;
import org.springframework.stereotype.Service;

/**
* Auth Service - Application Layer
* Orchestrates use cases
*/
@Service
public class AuthService {
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;

    public AuthService(UserRepository userRepository,
    RegisterUserUseCase.PasswordEncoder passwordEncoder,
    RegisterUserUseCase.TokenGenerator tokenGenerator) {
        this.registerUserUseCase = new RegisterUserUseCase(userRepository, passwordEncoder, tokenGenerator);
        this.loginUserUseCase = new LoginUserUseCase(userRepository, passwordEncoder, tokenGenerator);
    }

    public record AuthResponse(String token, String userId, String email, String name) {}

    public AuthResponse register(String email, String name, String password) {
        var result = registerUserUseCase.execute(new RegisterUserUseCase.Input(email, name, password));
        return new AuthResponse(result.token(), result.user().getId(), result.user().getEmail(), result.user().getName());
    }

    public AuthResponse login(String email, String password) {
        var result = loginUserUseCase.execute(new LoginUserUseCase.Input(email, password));
        return new AuthResponse(result.token(), result.user().getId(), result.user().getEmail(), result.user().getName());
    }
}