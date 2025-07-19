package org.example.dentalclinicmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2RedirectController {

    @GetMapping("/oauth2/redirect")
    public ResponseEntity<String> handleOAuth2Redirect(@RequestParam String token) {
        return ResponseEntity.ok(token);
    }
}