package kr.ac.kopo.wodyd.termproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // /error : 오류 화면. 막아 두면 비로그인 사용자가 없는 주소를 열 때 로그인 화면으로 튕긴다
                        .requestMatchers("/", "/signup", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        // 관리자 페이지는 ADMIN 권한만
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 작성/수정 화면은 로그인 필요 (아래 공개 규칙보다 먼저 와야 한다)
                        .requestMatchers("/parties/new", "/parties/*/edit").authenticated()
                        // 파티 목록과 상세는 누구나 볼 수 있다
                        .requestMatchers(HttpMethod.GET, "/parties", "/parties/*").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());
        return http.build();
    }
}
