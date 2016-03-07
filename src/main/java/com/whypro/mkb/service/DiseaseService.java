package com.whypro.mkb.service;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
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
	OntologyService ontologyService;
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	OntModel ontModel;
	
	public DiseaseService() {
		this.ontModel = ModelFactory.createOntologyModel();
	}
	
	public Disease findDiseaseByName(String name) {  
        return mongoTemplate.findOne(
        	new Query(new Criteria().orOperator(Criteria.where("name").is(name), Criteria.where("aliases").is(name))), 
        	Disease.class, 
        	DISEASE_COLLECTION
        );
    }
	
	public Disease findDiseaseById(String id) {
		return mongoTemplate.findOne(new Query(Criteria.where("id").is(id)), Disease.class, DISEASE_COLLECTION);
	}
	
	public void createDiseaseOntologyInstance(String id) {
    	ontologyService.readFromFile("E:/Codes/Java/medical-kb/src/main/resources/MKB.owl");
    	ontologyService.loadDepartmentIndividualFromDB();
    	ontologyService.initTerminologyMap();
    	ontologyService.loadDiseaseIndividualFromDB();
    	ontologyService.writeToFile("E:\\Codes\\Java\\medical-kb\\src\\main\\resources\\MKB_out.owl");
    	// ontologyService.printAllClasses();
    	// ontologyService.test();
	}
}
