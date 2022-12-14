package com.springbootdemo.springsecuritydemo.config;

import com.springbootdemo.springsecuritydemo.entity.User;
import com.springbootdemo.springsecuritydemo.service.UserService;
import com.springbootdemo.springsecuritydemo.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter{
    private UserService userService;

    @Autowired
    public SecurityConfig(UserService userService ){
        this.userService = userService;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        //???????????????????????????
        web.ignoring().antMatchers("/resources/**");
    }

    //AuthenticationManager????????????????????????
    //AuthenticationManagerBuilder???????????????AuthenticationManager???????????????
    //ProvideManager???AuthenticationManager????????????????????????
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //?????????????????????
        //auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("123456"));

        //?????????????????????
        //AuthenticationProvider???ProvideManager????????????AuthenticationProvider?????????AuthenticationProvider?????????????????????
        // ???????????????ProvideManager??????????????????AuthenticationProvider???
        auth.authenticationProvider(new AuthenticationProvider() {
            //Authentication??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = (String) authentication.getCredentials();

                User user = userService.findUserByName(username);
                if(user == null){
                    throw new UsernameNotFoundException("???????????????!");
                }

                password = CommunityUtil.md5(password + user.getSuffix());
                if(!user.getPassword().equals(password)){
                    throw new BadCredentialsException("???????????????! ");
                }

                //principal??????????????????credentials????????????authorities????????????
                return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
            }

            //?????????AuthenticationProvider??????????????????????????????
            @Override
            public boolean supports(Class<?> authentication) {
                //UsernamePasswordAuthenticationToken???Authentication??????????????????
                return UsernamePasswordAuthenticationToken.class.equals(authentication);
            }
        });
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //?????????????????????
        http.formLogin()
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                })
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                        request.setAttribute("error", exception.getMessage());
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                    }
                });

        //??????????????????
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                });

        //????????????
        http.authorizeRequests()
                .antMatchers("/letter").hasAnyAuthority("USER", "ADMIN")
                .antMatchers("/admin").hasAnyAuthority("ADMIN")
                .and().exceptionHandling().accessDeniedPage("/denied");


        //??????Filter,???????????????
        http.addFilterBefore(new Filter() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if(request.getServletPath().equals("/login")){
                    String verifyCode = request.getParameter("verifyCode");
                    //??????????????????1234
                    if(verifyCode == null || !verifyCode.equalsIgnoreCase("1234")){
                        request.setAttribute("error", "???????????????");
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                        return;
                    }
                }
                //??????????????????????????????????????????????????????????????????????????????
                chain.doFilter(request, response);
            }
        }, UsernamePasswordAuthenticationFilter.class);

        //?????????
        http.rememberMe()
                .tokenRepository(new InMemoryTokenRepositoryImpl())
                .tokenValiditySeconds(3600 * 24)
                .userDetailsService(userService);
    }
}
