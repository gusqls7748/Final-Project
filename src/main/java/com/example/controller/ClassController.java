package com.example.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.dto.ActivityCate;
import com.example.dto.Apply;
import com.example.dto.CityCate;
import com.example.dto.ClassProduct;
import com.example.dto.ClassUnit;
import com.example.entity.Basket;
import com.example.entity.ClassImage;
import com.example.entity.Member;
import com.example.service.KakaoLocalAPI;
import com.example.service.basket.BasketService;
import com.example.service.classproduct.ClassInsertService;
import com.example.service.classproduct.ClassManageService;
import com.example.service.classproduct.ClassSelectService;
import com.example.service.classproduct.ClassUnitService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping(value = "/class")
@Slf4j
public class ClassController {

    final String format = "ClassController => {}";

    @Autowired ClassSelectService cService;
    @Autowired ClassInsertService iService;
    @Autowired ClassManageService manageService;
    @Autowired ClassUnitService unitService;
    @Autowired BasketService basketService;

    @Autowired ResourceLoader resourceLoader;
    @Value("${default.image}") String defaultImg;

    @GetMapping(value = "/select.do")
    public String selectGET(
        @RequestParam(name = "search", defaultValue = "", required = false) String search,
        @AuthenticationPrincipal User user,
        Model model) {

        if (search.equals("")) {
            return "redirect:/class/select.do?search=list";
        }

        List<ActivityCate> list1 = cService.selectActivityCateList();
        List<CityCate> list2 = cService.selectCityCateList();
        model.addAttribute("user", user);
        model.addAttribute("list1", list1);
        model.addAttribute("list2", list2);

        return "/class/select";
    }

    @GetMapping(value = "/product.do")
    public String selectoneGET(
        @RequestParam(name = "classcode", defaultValue = "0") long classcode,
        Model model,
        @AuthenticationPrincipal User user
        ) {

        if (classcode == 0) {
            return "redirect:/class/select.do?search=list";
        }

        if (user != null) {
            model.addAttribute("user", user);
        }

        ClassProduct obj = cService.selectClassProductOne(classcode);
        long mainImg = manageService.selectClassMainImageNo(classcode);
        List<Long> subImg = manageService.selectClassSubImageNoList(classcode);
        long profile = manageService.selectClassProfileImageNo(classcode);

        log.info(format, obj.toString());

        model.addAttribute("obj", obj);
        model.addAttribute("mainImg", mainImg);
        model.addAttribute("subImg", subImg);
        model.addAttribute("profile", profile);
        

        return "class/selectone";
    }

    @PostMapping(value = "/product.do")
    public String productPOST(@ModelAttribute Apply apply, HttpSession httpSession) {

        //log.info(apply.toString());

        long unitno = apply.getUnitno();
        
        ClassUnit obj = cService.selectClassUnitOne(unitno);

        if(obj.getMaximum()-obj.getCnt() > apply.getPerson()) {

            httpSession.setAttribute("unitno", unitno);
            httpSession.setAttribute("person", apply.getPerson());

            return "redirect:/apply/insert.do";
        }
        else {

            return "redirect:/alert.do";
        }

        
    }

    @GetMapping(value = "/insert.do")
    public String insertGET(@AuthenticationPrincipal User user, Model model) {

        if(user != null) {
        model.addAttribute("user", user);
        }
        model.addAttribute("actlist", cService.selectActivityCateList());
        model.addAttribute("citylist", cService.selectCityCateList());

        return "/class/insert";
    }

    @PostMapping(value = "/insert.do")
    public String insertPOST(
            @ModelAttribute ClassProduct obj,
            @AuthenticationPrincipal User user,
            @RequestParam(name = "profile", required = false) MultipartFile profileImg,
            @RequestParam(name = "classSub", required = false) List<MultipartFile> classSubImg,
            @RequestParam(name = "classMain", required = false) MultipartFile classMainImg)
            throws IOException {

        log.info(format, profileImg);
        log.info(format, classSubImg);
        log.info(format, classMainImg);

        // 1.Rest API 호출 -> 주소 기반 위도, 경도 값 반환(Map)

        Map<String, String> map = KakaoLocalAPI.getCoordinate(obj.getAddress1());

        // 2. ClassProduct 객체 obj에 위도, 경도, 사용자 ID SET
        obj.setLatitude(map.get("y"));
        obj.setLongitude(map.get("x"));
        obj.setMemberid(user.getUsername());  // security session에 저장된 ID 정보를 호출

        // 3. 결과 확인
        log.info(format, obj.toString());

        // 4. ClassImage 리스트 객체 생성

        List<ClassImage> list = new ArrayList<>();

        ClassImage profile = new ClassImage();

        profile.setFilename(profileImg.getOriginalFilename());
        profile.setFilesize(profileImg.getSize());
        profile.setFiletype(profileImg.getContentType());
        profile.setFiledata(profileImg.getBytes());
        profile.setTypechk(1);
        list.add(profile);

        if (classSubImg != null) {

            for (MultipartFile file : classSubImg) {

                ClassImage classSub = new ClassImage();
                classSub.setFilename(file.getOriginalFilename());
                classSub.setFilesize(file.getSize());
                classSub.setFiletype(file.getContentType());
                classSub.setFiledata(file.getBytes());
                classSub.setTypechk(3);
                list.add(classSub);

            }
        }

        if (classMainImg != null) {
            
            ClassImage classMain = new ClassImage();

            classMain.setFilename(classMainImg.getOriginalFilename());
            classMain.setFilesize(classMainImg.getSize());
            classMain.setFiletype(classMainImg.getContentType());
            classMain.setFiledata(classMainImg.getBytes());
            classMain.setTypechk(2);
            list.add(classMain);
        }

        int ret = iService.insertClassOne(obj, list);

        log.info(format, ret);

        if (ret == 1) {
            return "redirect:/member/myclass.do?menu=";
        } else {
            return "redirect:/class/insert.do";
        }

    }

    @GetMapping(value = "/update.do")
    public String myclassUpdateGET(@AuthenticationPrincipal User user,
        @RequestParam(name = "classcode", defaultValue = "0") long classcode,
        Model model) {

        ClassProduct obj = manageService.selectClassOne(classcode);
        log.info(format, obj.toString());
        
        long profileImg = manageService.selectClassProfileImageNo(classcode);
        long mainImg = manageService.selectClassMainImageNo(classcode);

        model.addAttribute("actlist", cService.selectActivityCateList());
        model.addAttribute("citylist", cService.selectCityCateList());
        model.addAttribute("obj", obj);
        model.addAttribute("user", user);
        model.addAttribute("profileImg", profileImg);
        model.addAttribute("mainImg", mainImg);
        model.addAttribute("subImg", manageService.selectClassSubImageNoList(classcode));
        return "/class/update";
    }

    @PostMapping(value = "/update.do")
    public String myclassUpdatePOST(@AuthenticationPrincipal User user,
        @ModelAttribute ClassProduct obj,
        @ModelAttribute com.example.dto.ClassImage obj2,
        @RequestParam(name = "classcode", defaultValue = "0") long classcode,
        Model model) {
    
        int ret = manageService.updateClassOne(obj);
        int ret2 = manageService.updateClassImageOne(obj2);
        log.info("update class --- => {}", obj.toString());
        log.info("update image --- => {}", obj2.toString());
        if ( ret == 1 || ret2 == 1) {
            log.info("update success class ---- =>{}", ret);
            log.info("update success image ---- =>{}", ret2);
            return "redirect:/member/myclass.do";
        } else {
            return "redirect:/class/update.do?classcode=" +classcode;
        }
    }

    @PostMapping(value = "/delete.do")
    public String myclassdeletePOST( @AuthenticationPrincipal User user,
        @ModelAttribute ClassProduct obj, 
        Model model
    ) {
        log.info(format, obj.toString());
        int ret = manageService.updateClassInactive(obj);
        if(ret ==1 ){
            return "redirect:/home.do";
        }
        return "/member/myclass";
    }

    @PostMapping(value = "/basket.do")
    public String basketPOST(
        @RequestParam(name = "unitno") long unitno,
        @RequestParam(name = "cnt") int cnt,
        @AuthenticationPrincipal User user
        ) {

        String id = user.getUsername();

        Member member = new Member();
        member.setId(id);

        com.example.entity.ClassUnit classunit = new com.example.entity.ClassUnit();
        classunit.setNo(unitno);

        Basket obj = new Basket();
        
        obj.setCnt(cnt);
        obj.setMember(member);
        obj.setClassunit(classunit);

        int ret = basketService.insertBasketOne(obj);
        
        log.info(format, ret);

        return "redirect:alert.do";
    }


    @GetMapping(value = "/image")
    public ResponseEntity<byte[]> classImage(
        @RequestParam(name = "no", defaultValue = "0", required = false) long no
        ) throws IOException {

            log.info(format, no);

            com.example.dto.ClassImage obj = manageService.selectClassImageOne(no);

            log.info(format, obj.toString());

            HttpHeaders headers = new HttpHeaders();

            if(obj != null) {

                if(obj.getFilesize() > 0) {

                    headers.setContentType(MediaType.parseMediaType(obj.getFiletype()));

                    ResponseEntity<byte[]> response = new ResponseEntity<byte[]>(obj.getFiledata(), headers, HttpStatus.OK);
                    
                    return response;
                }
            }

            InputStream is = resourceLoader.getResource(defaultImg).getInputStream();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(is.readAllBytes(), headers, HttpStatus.OK);
    }
    

}
