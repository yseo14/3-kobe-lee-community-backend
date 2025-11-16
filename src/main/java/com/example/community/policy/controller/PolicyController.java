package com.example.community.policy.controller;


import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class PolicyController {

    @Value("${company.name}")
    private String companyName;

    @Value("${company.contact-email}")
    private String contactEmail;

    private final LocalDate lastUpdated = LocalDate.of(2025, 10, 24);

    @GetMapping("/terms")
    public String terms(Model model) {
        setCommonAttributes(model, "이용약관");
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        setCommonAttributes(model, "개인정보처리방침");
        return "privacy";
    }

    private void setCommonAttributes(Model model, String pageTitle) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("companyName", companyName);
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("lastUpdated", lastUpdated);
    }
}