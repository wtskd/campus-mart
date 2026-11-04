package com.campus.auth.controller;

import com.campus.auth.dto.LoginRequest;
import com.campus.auth.dto.LoginResult;
import com.campus.auth.service.AuthService;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLogAspect;
import com.campus.common.security.LoginUser;
import com.campus.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证接口
 *
 * @author campus-mart
 */
@Tag(name = "认证", description = "登录、退出、验证码、动态菜单")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(authService.captcha());
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = OperLogAspect.clientIp(request);
        return Result.ok("登录成功", authService.login(req, ip, true));
    }

    @Operation(summary = "App/小程序登录（无需图形验证码）")
    @PostMapping("/app/login")
    public Result<LoginResult> appLogin(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = OperLogAspect.clientIp(request);
        return Result.ok("登录成功", authService.login(req, ip, false));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.getUserId());
        return Result.ok("退出成功", null);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<LoginUser> me() {
        Long userId = SecurityUtils.requireUserId();
        return Result.ok(authService.me(userId));
    }

    @Operation(summary = "动态菜单树")
    @GetMapping("/menus")
    public Result<List<com.campus.api.dto.MenuNodeDTO>> menus() {
        Long userId = SecurityUtils.requireUserId();
        return Result.ok(authService.menus(userId));
    }
}
