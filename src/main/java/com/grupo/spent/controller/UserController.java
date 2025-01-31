package com.grupo.spent.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grupo.spent.dtos.requests.LoginDto;
import com.grupo.spent.dtos.requests.RegisterDto;
import com.grupo.spent.dtos.responses.LoginResponseDto;
import com.grupo.spent.dtos.responses.RegisterResponseDto;
import com.grupo.spent.entities.User;
import com.grupo.spent.exceptions.HttpException;
import com.grupo.spent.exceptions.NotFoundException;
import com.grupo.spent.services.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDto registerDto, HttpServletRequest request)
            throws NotFoundException, UnsupportedEncodingException, MessagingException {
        try {
            if (userService.existsUserByEmail(registerDto.getEmail())) {
                throw new HttpException(HttpStatus.BAD_REQUEST, "This User already exists.");
            }

            User user = userService.register(registerDto.getEmail(), registerDto.getUsername(), registerDto.getName(),
                    registerDto.getPassword(), getSiteURL(request));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponseDto(user.getEmail(), user.getUsername(), user.getFirstName()));
        } catch (HttpException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@Param("code") String code) {
        if (userService.verify(code)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:5173/verify-success"))
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:5173/verify-fail"))
                    .build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) throws NotFoundException {
        try {
            var accessToken = userService.login(loginDto.getEmail(), loginDto.getPassword());
            User user = userService.findUserByEmail(loginDto.getEmail());
            if (user.isVerified()) {
                return ResponseEntity.status(HttpStatus.OK).body(new LoginResponseDto(user.getUsername(), accessToken));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User account is not verified");

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getUserById(@PathVariable String username) throws NotFoundException {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.findUserByUsername(username));
        } catch (HttpException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }
}
