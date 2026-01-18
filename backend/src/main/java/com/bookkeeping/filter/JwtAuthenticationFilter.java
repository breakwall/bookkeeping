package com.bookkeeping.filter;

import com.bookkeeping.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private static final String[] WHITELIST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/health"  // 健康检查端点，允许 Docker 健康检查访问
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 白名单路径直接放行
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取Token
        String token = getTokenFromRequest(request);
        
        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"未授权，请先登录\"}");
            return;
        }
        
        // 将用户ID存入Request属性，供后续使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        for (String whitePath : WHITELIST) {
            if (path.startsWith(whitePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 从Request中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
