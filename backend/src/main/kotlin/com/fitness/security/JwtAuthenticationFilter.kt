package com.fitness.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)
        val claims = jwtService.validateToken(token)

        if (claims != null && claims["type"] == "access") {
            val userId = claims.subject
            val email = claims["email"] as? String ?: ""
            val role = claims["role"] as? String ?: "client"

            val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            val principal = UserPrincipal(userId, email, role)
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}

data class UserPrincipal(
    val userId: String,
    val email: String,
    val role: String
)
