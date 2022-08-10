package com.springbootdemo.springsecuritydemo.controller;

import com.springbootdemo.springsecuritydemo.entity.User;
import com.springbootdemo.springsecuritydemo.util.CommunityConstant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
//访问路径的时候前面还要加上community
public class HomeController implements CommunityConstant {

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    //查询全部帖子，并显示在首页上
    public String getIndexPage(Model model) {
        //认证成功后，结果会通过SecurityContextHolder存入SecurityContext中
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(obj instanceof User){
            model.addAttribute("loginuser", obj);
        }
        return "/index";
    }

    @RequestMapping(path = "/discuss", method = RequestMethod.GET)
    public String getDiscussPage() {
        return "/site/discuss";
    }

    @RequestMapping(path = "/letter", method = RequestMethod.GET)
    public String getLetterPage() {
        return "/site/letter";
    }

    @RequestMapping(path = "/admin", method = RequestMethod.GET)
    public String getAdminPage() {
        return "/site/admin";
    }

    @RequestMapping(path = "/loginpage", method = {RequestMethod.GET, RequestMethod.POST})
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }


    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }

}