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
        //忽略静态资源的访问
        web.ignoring().antMatchers("/resources/**");
    }

    //AuthenticationManager：认证的核心接口
    //AuthenticationManagerBuilder：用于构建AuthenticationManager对象的工具
    //ProvideManager：AuthenticationManager接口的默认实现类
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //内置的认证规则
        //auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("123456"));

        //自定义认证规则
        //AuthenticationProvider：ProvideManager持有一组AuthenticationProvider，每个AuthenticationProvider负责一种认证。
        // 委托模式：ProvideManager将认证委托给AuthenticationProvider。
        auth.authenticationProvider(new AuthenticationProvider() {
            //Authentication：用来封装认证信息的接口，不同的实现类代表不同类型的认证信息。认证信息：例如账号密码
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = (String) authentication.getCredentials();

                User user = userService.findUserByName(username);
                if(user == null){
                    throw new UsernameNotFoundException("账号不存在!");
                }

                password = CommunityUtil.md5(password + user.getSuffix());
                if(!user.getPassword().equals(password)){
                    throw new BadCredentialsException("密码不正确! ");
                }

                //principal：主要信息；credentials：证书；authorities：权限；
                return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
            }

            //当前的AuthenticationProvider支持哪种类型的认证。
            @Override
            public boolean supports(Class<?> authentication) {
                //UsernamePasswordAuthenticationToken：Authentication常用的实现类
                return UsernamePasswordAuthenticationToken.class.equals(authentication);
            }
        });
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //登录相关的配置
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

        //退出相关配置
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                });

        //授权配置
        http.authorizeRequests()
                .antMatchers("/letter").hasAnyAuthority("USER", "ADMIN")
                .antMatchers("/admin").hasAnyAuthority("ADMIN")
                .and().exceptionHandling().accessDeniedPage("/denied");


        //增加Filter,处理验证码
        http.addFilterBefore(new Filter() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if(request.getServletPath().equals("/login")){
                    String verifyCode = request.getParameter("verifyCode");
                    //假设验证码是1234
                    if(verifyCode == null || !verifyCode.equalsIgnoreCase("1234")){
                        request.setAttribute("error", "验证码错误");
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                        return;
                    }
                }
                //让请求继续向下执行，如果不写这行代码，请求到此终止。
                chain.doFilter(request, response);
            }
        }, UsernamePasswordAuthenticationFilter.class);

        //记住我
        http.rememberMe()
                .tokenRepository(new InMemoryTokenRepositoryImpl())
                .tokenValiditySeconds(3600 * 24)
                .userDetailsService(userService);
    }
}