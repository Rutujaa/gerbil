package org.aksw.gerbil.relation.extraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.vocabulary.NIF;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Mohit Mahajan
 *
 */
public class RelationExtraction implements IRelationExtraction {


	/**
	 * this method takes a Turtle input file, identifies the entities and their relation and provides an output ttl file defining the relation
	 */
	public void  extractRelation(String inputFile) throws FileNotFoundException {
		String sentence = "";
		String sentenceURI = "";
		boolean hasRelation = false;
		String relation = "";

		Model model = RDFDataMgr.loadModel(inputFile);

		Set<Resource> resourceSet = new LinkedHashSet<Resource>();

		Map<String, List<Integer>> entityMap = new LinkedHashMap<String, List<Integer>>();

		List<String> relatedEntities = new ArrayList<String>();

		resourceSet = getResources(resourceSet, model);

		for (Resource resource : resourceSet) {
			if (null != resource.getProperty(NIF.anchorOf)) {
				String entity = resource.getProperty(NIF.anchorOf).getString();
				Integer beginIndex = resource.getProperty(NIF.beginIndex).getInt();
				Integer endIndex = resource.getProperty(NIF.endIndex).getInt();
				List<Integer> indexList = new ArrayList<Integer>();
				indexList.add(beginIndex);
				indexList.add(endIndex);
				entityMap.put(entity, indexList);
			} else {
				sentence = resource.getProperty(NIF.isString).getString();
				sentenceURI = resource.getURI();
			}
		}

		Iterator<String> entityIterator = entityMap.keySet().iterator();
		String lastEntity = "";
		String entity1 = "";
		String entity2 = "";
		while (entityIterator.hasNext()) {
			if (!lastEntity.isEmpty()) {
				entity1 = lastEntity;
			} else {
				entity1 = entityIterator.next();
			}
			if (entityIterator.hasNext()) {
				entity2 = entityIterator.next();
				lastEntity = entity2;
				System.out.println(entity2 + " : " + entity1);
				System.out.println(sentence);
				for(String relationType:RELATION_DATA_SET){
					hasRelation = sentence.substring(entityMap.get(entity2).get(0), entityMap.get(entity1).get(1))
							.toLowerCase().matches("(.*)" + relationType.toLowerCase() + "(.*)");
					if(hasRelation){
						relation = relationType;
						break;
					}
				}
			}
			if (hasRelation) {
				relatedEntities.add(entity1);
				relatedEntities.add(entity2);
				break;
			}
		}


		if (hasRelation) {
			generateOutput(sentenceURI, relation, relatedEntities);
		}
	}

	/**
	 * extract list of statements from model and iterate over it to get resource list
	 */
	public Set<Resource> getResources(Set<Resource> resourceSet, Model model){
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement(); 
			Resource subject = stmt.getSubject(); 
			Property predicate = stmt.getPredicate(); 
			RDFNode object = stmt.getObject(); 

			resourceSet.add(subject);
		}
		return resourceSet;
	}

	/**
	 * this method generates output file in ttl format
	 */
	public void generateOutput(String sentenceURI, String relation, List<String> relatedEntities) throws FileNotFoundException{
		Model model1 = ModelFactory.createDefaultModel();

		Resource node = model1.createResource(sentenceURI).addProperty(RDF.object, relatedEntities.get(0))
				.addLiteral(RDF.subject, relatedEntities.get(1)).addLiteral(RDF.predicate, relation);

		File outputFile = new File("C:\\Users\\mmahajan\\Desktop\\Benchmarking\\output\\otput.ttl");
		FileOutputStream fos = new FileOutputStream(outputFile);
		model1.write(fos, "TURTLE");
	}

	public static void main(String[] args) throws FileNotFoundException {
		new  RelationExtraction().extractRelation("C:\\Users\\mmahajan\\Desktop\\Benchmarking\\input\\trainer.ttl");
	}


}
