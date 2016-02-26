package com.whypro.mkb.service;

import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.data.mongodb.core.MongoTemplate;  
import org.springframework.data.mongodb.core.query.Criteria;  
import org.springframework.data.mongodb.core.query.Query;  
import org.springframework.stereotype.Service;

import com.whypro.mkb.pojo.Disease;  


@Service
public class DiseaseService {
	
	private static String DISEASE_COLLECTION = "disease";  
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	public Disease findDiseaseByName(String name) {  
        return mongoTemplate.findOne(
        	new Query(new Criteria().orOperator(Criteria.where("name").is(name), Criteria.where("aliases").is(name))), 
        	Disease.class, 
        	DISEASE_COLLECTION
        );  
    } 
}
