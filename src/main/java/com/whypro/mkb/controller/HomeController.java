package com.whypro.mkb.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.whypro.mkb.pojo.Disease;
import com.whypro.mkb.service.DiseaseService;

@Controller  
@RequestMapping("/home")  
public class HomeController {  
      
    private static Logger logger = Logger.getLogger(HomeController.class.getName());  
      
    @Autowired  
    DiseaseService diseaseService;  

    @RequestMapping(value="/index", method=RequestMethod.GET)
	public String show() {
    	Disease disease = diseaseService.findDiseaseByName("小儿合胞病毒肺炎");
    	System.out.println(disease.getName()+"\n"+disease.getSummary());
	    return "/home/index";
	}
}  