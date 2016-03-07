package com.whypro.mkb.service;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import org.wltea.analyzer.lucene.IKAnalyzer;


import com.whypro.mkb.pojo.Department;
import com.whypro.mkb.pojo.Disease;
import com.whypro.mkb.pojo.Examination;
import com.whypro.mkb.pojo.Medication;
import com.whypro.mkb.pojo.Surgery;
import com.whypro.mkb.pojo.Symptom;

@Service 
class OntologyService {
	
	// private static String OWL_FILE = "file:./src/main/resources/MKB.owl";
	OntModel ontModel;
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	private static String DISEASE_COLLECTION = "disease";
	private static String DEPARTMENT_COLLECTION = "department";
	private static String SYMPTOM_COLLECTION = "symptom";
	private static String EXAMINATION_COLLECTION = "examination";
	private static String SURGERY_COLLECTION = "surgery";
	private static String MEDICATION_COLLECTION = "medication";
	
	private static String NS_URI_PREFIX = "MKB";
	
	private static Map<String, Map<String, Object>> terminologyMap;
	private static List<String> terminologyList;
	
	public OntologyService() {
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	}

	public void readFromFile(String filename) {
		ontModel.read("file:"+filename);
	}
	
	public void writeToFile(String filename) {
		try {
			FileOutputStream writerStream = new FileOutputStream(filename);
			ontModel.writeAll(writerStream, "RDF/XML-ABBREV");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public OntClass getOntClassByName(String prefix, String className) {
		String baseURI = ontModel.getNsPrefixURI(prefix);
		return ontModel.getOntClass(baseURI+className);
	}
	
	public void test() {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		OntClass a = ontModel.getOntClass(baseURI+"药物");
		OntClass b = ontModel.getOntClass(baseURI+"食物");
		Individual cat = a.createIndividual(baseURI+"猫");
		Property eat = ontModel.getProperty(baseURI+"吃");
		cat.addProperty(eat, baseURI+"熊");
		b.createIndividual(baseURI+"熊");
		
		String file = "E:\\Codes\\Java\\medical-kb\\src\\main\\resources\\MKB_out.owl";
		FileOutputStream writerStream;
		try {
			writerStream = new FileOutputStream(file);
			ontModel.writeAll(writerStream, "RDF/XML-ABBREV");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadDepartmentIndividualFromDB() {
		// 从 MongoDB 数据库中读取科室信息，然后将其转化为实例加入本体模型
		
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		OntClass c = ontModel.getOntClass(baseURI+"科室");
		
		List<Department> departments = mongoTemplate.findAll(Department.class, DEPARTMENT_COLLECTION);
		for (Department department : departments) {
			Individual childIndividual = null;
			// 循环直到没有父科室为止
			while (true) {
				String departmentName = department.getName();
				// 不优雅的过滤方式
				if (departmentName.endsWith("科") && !departmentName.equals("生活百科")) {
					// System.out.println(departmentName);
					// 判断个体是否已存在
					if (ontModel.getIndividual(baseURI+departmentName) == null) {
						Individual individual = c.createIndividual(baseURI+departmentName);
						System.out.println(departmentName);
						if (childIndividual != null) {
							individual.addProperty(ontModel.getProperty(baseURI+"包含科室"), childIndividual);
							childIndividual.addProperty(ontModel.getProperty(baseURI+"属于科室"), individual);
						}
						String parentDepartmentName = department.getParent();
						// 是否存在父科室
						if (parentDepartmentName != null) {
							Department parentDepartment = mongoTemplate.findOne(
								new Query(Criteria.where("name").is(parentDepartmentName)), 
								Department.class, 
								DEPARTMENT_COLLECTION
							);
							if (parentDepartment != null) {
								childIndividual = individual;
								department = parentDepartment;
							} else {
								break;
							}
						} else {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
		}
	}
	
	public void initTerminologyMap() {
		terminologyMap = new HashMap<String, Map<String, Object>>();
		terminologyList = new ArrayList<String>();
		
		List<Disease> diseases = mongoTemplate.findAll(Disease.class, DISEASE_COLLECTION);
		Map<String, Object> diseaseTerminologyMap = new HashMap<String, Object>();
		for (Disease disease : diseases) {
			List<String> names = new ArrayList<String>();
			names.add(disease.getName());
			if (disease.getAliases() != null) {
				names.addAll(disease.getAliases());
			}
			for (String name : names) {
				diseaseTerminologyMap.put(name, disease);
			}
		}
		terminologyMap.put("disease", diseaseTerminologyMap);
		terminologyList.addAll(terminologyMap.get("disease").keySet());
		
		List<Symptom> symptoms = mongoTemplate.findAll(Symptom.class, SYMPTOM_COLLECTION);
		Map<String, Object> symptomTerminologyMap = new HashMap<String, Object>();
		for (Symptom symptom : symptoms) {
			symptomTerminologyMap.put(symptom.getName(), symptom);
		}
		terminologyMap.put("symptom", symptomTerminologyMap);
		terminologyList.addAll(terminologyMap.get("symptom").keySet());
		
		List<Examination> examinations = mongoTemplate.findAll(Examination.class, EXAMINATION_COLLECTION);
		Map<String, Object> examinationTerminologyMap = new HashMap<String, Object>();
		for (Examination examination : examinations) {
			examinationTerminologyMap.put(examination.getName(), examination);
		}
		terminologyMap.put("examination", examinationTerminologyMap);
		terminologyList.addAll(terminologyMap.get("examination").keySet());
		
		List<Surgery> surgeries = mongoTemplate.findAll(Surgery.class, SURGERY_COLLECTION);
		Map<String, Object> surgeryTerminologyMap = new HashMap<String, Object>();
		for (Surgery surgery : surgeries) {
			surgeryTerminologyMap.put(surgery.getName(), surgery);
		}
		terminologyMap.put("surgery", surgeryTerminologyMap);
		terminologyList.addAll(terminologyMap.get("surgery").keySet());
		
		List<Medication> medications = mongoTemplate.findAll(Medication.class, MEDICATION_COLLECTION);
		Map<String, Object> medicationTerminologyMap = new HashMap<String, Object>();
		for (Medication medication : medications) {
			if (medication.getGenericName() != null) {
				medicationTerminologyMap.put(medication.getGenericName(), medication);
			}
			if (medication.getTradeName() != null) {
				medicationTerminologyMap.put(medication.getTradeName(), medication);
			}
		}
		terminologyMap.put("medication", medicationTerminologyMap);
		terminologyList.addAll(terminologyMap.get("medication").keySet());
		
		try {
			String file = "E:\\Codes\\Java\\medical-kb\\src\\main\\webapp\\WEB-INF\\classes\\terminology.dic";
			FileOutputStream writerStream = new FileOutputStream(file);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(writerStream, "UTF-8"));
			for (String key : terminologyList) {
				if (key != null) {
					writer.write(key+"\n");
				}
			}
			writer.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void createDiseaseIndividual(Disease disease) {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		// System.out.println(baseURI);
		
		OntClass diseaseOntClass = ontModel.getOntClass(baseURI+"疾病");
		OntClass symptomOntClass = ontModel.getOntClass(baseURI+"症状");
		OntClass examinationOntClass = ontModel.getOntClass(baseURI+"检查");
		OntClass surgeryOntClass = ontModel.getOntClass(baseURI+"手术");
		OntClass medicationOntClass = ontModel.getOntClass(baseURI+"药物");		
		
		Property surgeryProperty = ontModel.getProperty(baseURI+"进行手术");
		Property surgeryPropertyReverse = ontModel.getProperty(baseURI+"手术适应症");
		Property medicationProperty = ontModel.getProperty(baseURI+"可应用药物");
		Property medicationPropertyReverse = ontModel.getProperty(baseURI+"可治疗疾病");
		Property symptomProperty = ontModel.getProperty(baseURI+"包含症状");
		Property symptomPropertyReverse = ontModel.getProperty(baseURI+"属于疾病");
		Property examinationProperty = ontModel.getProperty(baseURI+"进行检查");
		Property examinationPropertyReverse = ontModel.getProperty(baseURI+"检查适应症");
		Property departmentProperty = ontModel.getProperty(baseURI+"参考科室");
		Property identificationProperty = ontModel.getProperty(baseURI+"鉴别疾病");
		Property complicationProperty = ontModel.getProperty(baseURI+"并发疾病");
		
		System.out.println(disease.getName());
		Individual individual = diseaseOntClass.createIndividual(baseURI+disease.getName());
		if (disease.getAliases() != null) {
			for (String alias : disease.getAliases()) {
				Individual sameIndividual = diseaseOntClass.createIndividual(baseURI+alias);
				individual.addSameAs(sameIndividual);
				sameIndividual.addSameAs(individual);
			}
		}
		
		// 症状
		String rawSymptom = disease.getSymptom();
		if (rawSymptom != null) {
			for (String terminology : terminologyMap.get("symptom").keySet()) {
				if (rawSymptom.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual symptomIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (symptomIndividual != null) {
						if (symptomIndividual.hasOntClass(symptomOntClass)) {
							individual.addProperty(symptomProperty, symptomIndividual);
							symptomIndividual.addProperty(symptomPropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Symptom symptom = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Symptom.class, 
							SYMPTOM_COLLECTION
						);
						if (symptom != null) {
							// symptomIndividual = createSymptomIndividual();
							symptomIndividual = symptomOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(symptomProperty, symptomIndividual);
							symptomIndividual.addProperty(symptomPropertyReverse, individual);
						}
					}
				}
			}
		}
		
		// 检查
		String rawExamination = disease.getExamination();
		if (rawExamination != null) {
			for (String terminology : terminologyMap.get("examination").keySet()) {
				if (rawExamination.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual examinationIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (examinationIndividual != null) {
						if (examinationIndividual.hasOntClass(examinationOntClass)) {
							individual.addProperty(examinationProperty, examinationIndividual);
							examinationIndividual.addProperty(examinationPropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Examination examination = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Examination.class, 
							EXAMINATION_COLLECTION
						);
						if (examination != null) {
							// symptomIndividual = createSymptomIndividual();
							examinationIndividual = examinationOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(examinationProperty, examinationIndividual);
							examinationIndividual.addProperty(examinationPropertyReverse, individual);
						}
					}
				}
			}
		}
		
		// 手术和药物
		String rawTreat = disease.getTreat();
		if (rawTreat != null) {
			for (String terminology : terminologyMap.get("surgery").keySet()) {
				if (rawTreat.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual surgeryIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (surgeryIndividual != null) {
						if (surgeryIndividual.hasOntClass(surgeryOntClass)) {
							individual.addProperty(surgeryProperty, surgeryIndividual);
							surgeryIndividual.addProperty(surgeryPropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Surgery surgery = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Surgery.class, 
							SURGERY_COLLECTION
						);
						if (surgery != null) {
							// symptomIndividual = createSymptomIndividual();
							surgeryIndividual = surgeryOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(surgeryProperty, surgeryIndividual);
							surgeryIndividual.addProperty(surgeryPropertyReverse, individual);
						}
					}
				}
			}
			
			for (String terminology : terminologyMap.get("medication").keySet()) {
				if (rawTreat.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual medicationIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (medicationIndividual != null) {
						if (medicationIndividual.hasOntClass(medicationIndividual)) {
							individual.addProperty(medicationProperty, medicationIndividual);
							medicationIndividual.addProperty(medicationPropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Medication medication = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Medication.class, 
							MEDICATION_COLLECTION
						);
						if (medication != null) {
							// symptomIndividual = createSymptomIndividual();
							medicationIndividual = medicationOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(medicationProperty, medicationIndividual);
							medicationIndividual.addProperty(medicationPropertyReverse, individual);
						}
					}
				}
			}
		}
		
		// 鉴别
		String rawIdentification = disease.getIdentification();
		if (rawIdentification != null) {
			for (String terminology : terminologyMap.get("disease").keySet()) {
				if (rawIdentification.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual diseaseIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (diseaseIndividual != null) {
						if (diseaseIndividual.hasOntClass(diseaseOntClass)) {
							individual.addProperty(identificationProperty, diseaseIndividual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Disease idenficationDisease = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Disease.class, 
							DISEASE_COLLECTION
						);
						if (idenficationDisease != null) {
							// symptomIndividual = createSymptomIndividual();
							diseaseIndividual = diseaseOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(identificationProperty, diseaseIndividual);
						}
					}
				}
			}
		}
		// 并发
		String rawComplication = disease.getComplication();
		if (rawComplication != null) {
			for (String terminology : terminologyMap.get("disease").keySet()) {
				if (rawComplication.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual diseaseIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (diseaseIndividual != null) {
						if (diseaseIndividual.hasOntClass(diseaseOntClass)) {
							individual.addProperty(complicationProperty, diseaseIndividual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Disease complicationDisease = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Disease.class, 
							DISEASE_COLLECTION
						);
						if (complicationDisease != null) {
							// symptomIndividual = createSymptomIndividual();
							diseaseIndividual = diseaseOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(complicationProperty, diseaseIndividual);
						}
					}
				}
			}
		}
	}
	
	public void createSymptomIndividual(Symptom symptom) {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		
		OntClass diseaseOntClass = ontModel.getOntClass(baseURI+"疾病");
		OntClass examinationOntClass = ontModel.getOntClass(baseURI+"检查");
		
		
		Property examinationProperty = ontModel.getProperty(baseURI+"进行检查");
		Property examinationPropertyReverse = ontModel.getProperty(baseURI+"检查适应症");
		// Property identificationProperty = ontModel.getProperty(baseURI+"鉴别症状");
		
		System.out.println(symptom.getName());
		Individual individual = diseaseOntClass.createIndividual(baseURI+symptom.getName());
		
		// 检查
		String rawExamination = symptom.getExamination();
		if (rawExamination != null) {
			for (String terminology : terminologyMap.get("examination").keySet()) {
				if (rawExamination.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual examinationIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (examinationIndividual != null) {
						if (examinationIndividual.hasOntClass(examinationOntClass)) {
							individual.addProperty(examinationProperty, examinationIndividual);
							examinationIndividual.addProperty(examinationPropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Examination examination = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Examination.class, 
							EXAMINATION_COLLECTION
						);
						if (examination != null) {
							// symptomIndividual = createSymptomIndividual();
							examinationIndividual = examinationOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(examinationProperty, examinationIndividual);
							examinationIndividual.addProperty(examinationPropertyReverse, individual);
						}
					}
				}
			}
		}
	}
	
	public void createExaminationIndividual(Examination examination) {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		
		//OntClass diseaseOntClass = ontModel.getOntClass(baseURI+"疾病");
		//OntClass examinationOntClass = ontModel.getOntClass(baseURI+"检查");
		
		//Property examinationProperty = ontModel.getProperty(baseURI+"进行检查");
		//Property examinationPropertyReverse = ontModel.getProperty(baseURI+"检查适应症");
		
		System.out.println(examination.getName());
		//Individual individual = examinationOntClass.createIndividual(baseURI+examination.getName());
	}
	
	public void createSurgeryIndividual(Surgery surgery) {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		
		OntClass surgeryOntClass = ontModel.getOntClass(baseURI+"手术");
		OntClass diseaseOntClass = ontModel.getOntClass(baseURI+"疾病");
		
		Property diseaseProperty = ontModel.getProperty(baseURI+"手术适应症");
		Property diseasePropertyReverse = ontModel.getProperty(baseURI+"进行手术");
		
		System.out.println(surgery.getName());
		Individual individual = surgeryOntClass.createIndividual(baseURI+surgery.getName());
		
		// 适应症
		String rawIndication = surgery.getSymptom();
		if (rawIndication != null) {
			for (String terminology : terminologyMap.get("disease").keySet()) {
				if (rawIndication.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual diseaseIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (diseaseIndividual != null) {
						if (diseaseIndividual.hasOntClass(diseaseOntClass)) {
							individual.addProperty(diseaseProperty, diseaseIndividual);
							diseaseIndividual.addProperty(diseasePropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Disease disease = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Disease.class, 
							DISEASE_COLLECTION
						);
						if (disease != null) {
							// symptomIndividual = createSymptomIndividual();
							diseaseIndividual = diseaseOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(diseaseProperty, diseaseIndividual);
							diseaseIndividual.addProperty(diseasePropertyReverse, individual);
						}
					}
				}
			}
		}
	}
	
	public void createMedicationIndividual(Medication medication) {
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		
		OntClass medicationOntClass = ontModel.getOntClass(baseURI+"药物");
		OntClass diseaseOntClass = ontModel.getOntClass(baseURI+"疾病");
		
		Property diseaseProperty = ontModel.getProperty(baseURI+"可治疗疾病");
		Property diseasePropertyReverse = ontModel.getProperty(baseURI+"可应用药物");
		
		System.out.println(medication.getGenericName());
		Individual individual = null;
		if (medication.getGenericName() != null) {
			individual = medicationOntClass.createIndividual(baseURI+medication.getGenericName());
		}
		Individual sameIndividual = null;
		if (medication.getTradeName() != null) {
			sameIndividual = diseaseOntClass.createIndividual(baseURI+medication.getTradeName());
		}
		if (individual != null && sameIndividual != null) {
			individual.addSameAs(sameIndividual);
			sameIndividual.addSameAs(individual);
		}
		
		// 适应症
		String rawDisease = medication.getDisease();
		if (rawDisease != null) {
			for (String terminology : terminologyMap.get("disease").keySet()) {
				if (rawDisease.contains(terminology)) {
					// 在本体模型中查询是否包含该疾病实例
					Individual diseaseIndividual = ontModel.getIndividual(baseURI+terminology);
					// 存在，添加关联
					if (diseaseIndividual != null) {
						if (diseaseIndividual.hasOntClass(diseaseOntClass)) {
							individual.addProperty(diseaseProperty, diseaseIndividual);
							diseaseIndividual.addProperty(diseasePropertyReverse, individual);
						}
					}
					// 不存在，创建
					else {
						// 从数据库中查询
						Disease disease = mongoTemplate.findOne(
							new Query(Criteria.where("name").is(terminology)), 
							Disease.class, 
							DISEASE_COLLECTION
						);
						if (disease != null) {
							// symptomIndividual = createSymptomIndividual();
							diseaseIndividual = diseaseOntClass.createIndividual(baseURI+terminology);
							individual.addProperty(diseaseProperty, diseaseIndividual);
							diseaseIndividual.addProperty(diseasePropertyReverse, individual);
						}
					}
				}
			}
		}
	}
	
		
	public void loadDiseaseIndividualFromDB() {
		// 从 MongoDB 数据库中读取疾病信息，然后将其转化为实例加入本体模型
		
		String baseURI = ontModel.getNsPrefixURI(NS_URI_PREFIX);
		OntClass c = ontModel.getOntClass(baseURI+"疾病");
				
		List<Disease> diseases = mongoTemplate.findAll(Disease.class, DISEASE_COLLECTION).subList(0, 100);
		for (Disease disease : diseases) {
			createDiseaseIndividual(disease);
		}
		List<Symptom> symptoms = mongoTemplate.findAll(Symptom.class, SYMPTOM_COLLECTION).subList(0, 100);
		for (Symptom symptom : symptoms) {
			createSymptomIndividual(symptom);
		}
		
		List<Examination> examinations = mongoTemplate.findAll(Examination.class, EXAMINATION_COLLECTION).subList(0, 100);
		for (Examination examination : examinations) {
			createExaminationIndividual(examination);
		}
		
		List<Surgery> surgeries = mongoTemplate.findAll(Surgery.class, SURGERY_COLLECTION).subList(0, 100);
		for (Surgery surgery : surgeries) {
			createSurgeryIndividual(surgery);
		}
		
		List<Medication> medications = mongoTemplate.findAll(Medication.class, MEDICATION_COLLECTION).subList(0, 100);
		for (Medication medication : medications) {
			createMedicationIndividual(medication);
		}
		
		
	}
	
	
	
	public void printAllClasses() {
		for (Iterator<OntClass> i = ontModel.listClasses(); i.hasNext(); ) {  
            OntClass c = i.next();
            
            if (!c.isAnon()) {  //测试c是否匿名  
                System.out.print("Class");  
                System.out.println(c.getModel().getGraph().getPrefixMapping().shortForm(c.getURI()));  
                  
                if (c.getLocalName().equals("ConsumableThing")) {  
                    System.out.println("  URI@" + c.getURI());  
                    System.out.println("Animal's EquivalentClass is " + c.getEquivalentClass());  
                    System.out.println("[Comments:" + c.getEquivalentClass().getComment("EN")  + "]");  
                }
                for (ExtendedIterator<?> j = c.listInstances(); j.hasNext(); ) {
                	Individual individual = (Individual) j.next();  
                	System.out.println(individual.getLocalName());
                }
            }
		}
	}
	
	
	
}

