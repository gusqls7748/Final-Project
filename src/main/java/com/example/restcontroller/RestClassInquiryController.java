package com.example.restcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.entity.ClassInquiry;
import com.example.entity.Member;
import com.example.service.classproduct.ClassInquiryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/api/inquiry")
public class RestClassInquiryController {

    @Autowired ClassInquiryService inquiryService;

    final String format = "RestClassInquiryController => {}";

    @PostMapping(value = "/insertinquiry.json")
    public Map<String, Object> insertinquiryPOST(@RequestBody ClassInquiry obj, @AuthenticationPrincipal User user) {

        Map<String, Object> retMap = new HashMap<>();

        //log.info(format, obj.toString());

        Member member = new Member();
        member.setId(user.getUsername());

        obj.setMember(member);

        //log.info(format, obj.toString());

        int ret = inquiryService.insertClassInquiryOne(obj);

        //log.info(format, obj);

        retMap.put("ret", ret);

        return retMap;
    }

    @GetMapping(value = "/selectinquiry.json")
    public Map<String, Object> selectinquiryGET(
        @RequestParam(name = "classcode") long classcode,
        @RequestParam(name = "page") int page) {

        log.info(format, classcode);
        log.info(format, page);

        Map<String, Object> retMap = new HashMap<>();

        List<ClassInquiry> list = inquiryService.selectClassInquiryList(classcode);

        List<com.example.dto.ClassInquiry> retlist = new ArrayList<>();

        for(ClassInquiry rsp : list) {

            com.example.dto.ClassInquiry result = new com.example.dto.ClassInquiry();
            result.setNo(rsp.getNo());
            result.setTitle(rsp.getTitle());
            result.setContent(rsp.getContent());
            result.setRegdate(rsp.getRegdate());
            result.setMemberid(rsp.getMember().getId());

            retlist.add(result);

        }

        long pages = inquiryService.selectCountClassInquiryList(classcode);

        //log.info(format, retlist.toString());
        //log.info(format, pages);

        retMap.put("list", retlist);
        retMap.put("pages", pages);

        return retMap;
    }
}