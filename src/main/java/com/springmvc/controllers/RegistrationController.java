package com.springmvc.controllers;


import com.springmvc.models.User;
import com.springmvc.models.dto.CaptchaResponseDto;
import com.springmvc.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Controller
public class RegistrationController {
    private final static String CAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";
    @Autowired
    private UserService userService;
    @Value("${recaptcha.secret}")
    private String secret;
    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/registration")
    public String registration(@ModelAttribute("user") User user, Model model){
        model.addAttribute("user",user);
        return "registration";
    }

    @PostMapping("/registration")
    public String registrationuser(
            @RequestParam("g-recaptcha-response") String captchaResponce,
            @ModelAttribute("user") @Valid User user,
            BindingResult bindingResult, Model model){
        String url =  String.format(CAPTCHA_URL,secret,captchaResponce);
        CaptchaResponseDto responseDto = restTemplate.postForObject(url, Collections.emptyList(), CaptchaResponseDto.class);
        if(!responseDto.isSuccess()){
            model.addAttribute("captchaError", "Fill captcha");
        }

        if(user.getPassword() !=null && !user.getPassword().equals(user.getPassword2())){
            model.addAttribute("passError", "Passwords are different!");
            return "/registration";
        }
        if(bindingResult.hasErrors() || !responseDto.isSuccess()){
            Map<String,String> errors = ControllerUtils.getErrors(bindingResult);
            model.mergeAttributes(errors);
            model.addAttribute("user", user);
            return "/registration";

        }

        if(user.getUsername()!=null && !userService.registrationuser(user)){
            model.addAttribute("nameExistError", "Username exists!");
            return "/registration";
        }
        return "redirect:/login";
    }
    @GetMapping("/activate/{code}")
    public String activate(Model model, @PathVariable String code){
        boolean isActivated = userService.activateUser(code);

        if(isActivated){
            model.addAttribute("messageT", "alert-success");
            model.addAttribute("message", "User successfully");
        } else{
            model.addAttribute("messageT", "alert-danger");
            model.addAttribute("message", "Activation code is not found");
        }
        return "login";
    }
}
