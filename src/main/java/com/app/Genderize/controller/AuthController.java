package com.app.Genderize.controller;

import com.app.Genderize.config.SystemProperties;
import com.app.Genderize.config.auth.OAuthStateCache;
import com.app.Genderize.model.RefreshToken;
import com.app.Genderize.model.User;
import com.app.Genderize.service.OAuth2Service;
import com.app.Genderize.service.RefreshTokenDaoService;
import com.app.Genderize.service.TokenService;
import com.app.Genderize.service.UserDaoService;
import com.app.Genderize.util.PkceUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
public class AuthController {

    private final OAuth2Service oAuth2Service;
    private final OAuthStateCache oauthStateCache;
    private final UserDaoService userDaoService;
    private final TokenService tokenService;
    private final RefreshTokenDaoService refreshTokenDaoService;
    private final PkceUtil pkceUtil;
    private final SystemProperties systemProperties;


    @GetMapping("/github")
    public void redirect(HttpServletResponse response) throws IOException {
        String verifier = pkceUtil.generateCodeVerifier();
        String challenge = pkceUtil.generateCodeChallenge(verifier);
        String state = pkceUtil.generateState();

        oauthStateCache.save(state, verifier);

        String url = UriComponentsBuilder
                .fromUriString(systemProperties.getGithubAuthorizeUrl())
                .queryParam("client_id", systemProperties.getGithubClientId())
                .queryParam("redirect_uri", systemProperties.getGithubClientSecret())
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
        response.sendRedirect(url);
    }

    @GetMapping("/github-callback")
    public TokenService.AuthResponse callback(@RequestParam String code,
                                              @RequestParam String state) {
        String verifier = oauthStateCache.get(state);

        if (verifier == null) {
            throw new RuntimeException("Invalid state");
        }
        String githubToken = oAuth2Service.generateGithubAccessToken(code, verifier);
        Map githubUser = oAuth2Service.getUser(githubToken);

        User user = userDaoService.findOrCreateGithubUser(githubUser);

        return tokenService.issue(user);
    }

    @PostMapping("/refresh")
    public TokenService.AuthResponse refresh(@RequestBody Map<String, String> req) {

        RefreshToken token = refreshTokenDaoService.validate(req.get("refresh_token"));

        refreshTokenDaoService.revoke(token);

        User user = userDaoService.findById(token.getUserId());

        return tokenService.issue(user);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody Map<String, String> req) {
        RefreshToken token = refreshTokenDaoService.validate(req.get("refresh_token"));
        refreshTokenDaoService.revoke(token);
    }
}
