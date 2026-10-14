package com.example.authserver.security;

import com.example.authserver.config.AuthServerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminBootstrapTokenFilter extends OncePerRequestFilter {
    private final String expected;
    public AdminBootstrapTokenFilter(AuthServerProperties properties){this.expected=properties.admin().bootstrapToken();}
    public static boolean isValid(HttpServletRequest request,AuthServerProperties properties){return matches(request.getHeader("Authorization"),properties.admin().bootstrapToken());}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith(request.getContextPath()+"/admin/");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        if(SecurityContextHolder.getContext().getAuthentication()==null&&matches(request.getHeader("Authorization"),expected)){
            var auth=new UsernamePasswordAuthenticationToken("bootstrap-admin",null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request,response);
    }
    private static boolean matches(String header,String expected){if(header==null||!header.startsWith("Bearer ")||expected==null)return false;byte[] supplied=header.substring(7).getBytes(StandardCharsets.UTF_8);byte[] configured=expected.getBytes(StandardCharsets.UTF_8);return MessageDigest.isEqual(supplied,configured);}
}
