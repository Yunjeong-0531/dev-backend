package com.example.stay.auth.controller;

import com.example.stay.auth.dto.TermResponse;
import com.example.stay.auth.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    /** 가입 화면에 보여줄 약관 목록 */
    @GetMapping
    public List<TermResponse> getTerms() {
        return termService.getActiveTerms();
    }
}
