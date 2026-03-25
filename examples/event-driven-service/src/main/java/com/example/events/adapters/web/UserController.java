package com.example.events.adapters.web;

import com.example.events.application.RegisterUserUseCase;
import com.marcusprado02.commons.kernel.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST endpoint for user registration. */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final RegisterUserUseCase registerUser;

  public UserController(RegisterUserUseCase registerUser) {
    this.registerUser = registerUser;
  }

  @PostMapping
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    Result<String> result = registerUser.execute(request.email(), request.name());
    return result.isOk()
        ? ResponseEntity.ok(new RegisterResponse(result.getOrNull()))
        : ResponseEntity.badRequest().body(result.problemOrNull());
  }

  public record RegisterRequest(String email, String name) {}

  public record RegisterResponse(String userId) {}
}
