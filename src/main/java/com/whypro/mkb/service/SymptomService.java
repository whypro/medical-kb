package com.whypro.mkb.service;

import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.data.mongodb.core.MongoTemplate;  
import org.springframework.data.mongodb.core.query.Criteria;  
import org.springframework.data.mongodb.core.query.Query;  
import org.springframework.stereotype.Service;

import com.whypro.mkb.pojo.Symptom;  


@Service
public class SymptomService {
	
	private static String SYMPTOM_COLLECTION = "symptom";  
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	public Symptom findSymptomByName(String name) {  
        return mongoTemplate.findOne(
        	new Query(Criteria.where("name").is(name)), 
        	Symptom.class, 
        	SYMPTOM_COLLECTION
        );  
    } 
}
