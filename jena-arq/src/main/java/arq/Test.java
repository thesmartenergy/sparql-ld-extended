/*
 * Copyright 2017 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package arq;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
/**
 *
 * @author Sejal
 */
public class Test {
    private static String QUERY_TO_RUN; // The query to run
    private static String URL_DataCatalogue = "";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Create an empty in-memory model and populate it with the DBpedia triples describing Yellowfin tuna
        Model model = ModelFactory.createDefaultModel();
        model.read("http://dbpedia.org/data/Yellowfin_tuna.n3");

    // Query 1
        StringBuilder QUERY_getLocalTriples = new StringBuilder();
        QUERY_getLocalTriples.append("SELECT * WHERE { ?x ?y ?z }");

    // Query 2
        StringBuilder QUERY_getRDFa = new StringBuilder();
        QUERY_getRDFa.append("SELECT * WHERE { ");
        QUERY_getRDFa.append(" SERVICE <http://users.ics.forth.gr/~fafalios> { ");
        QUERY_getRDFa.append("  ?x ?y ?z } ");
        QUERY_getRDFa.append("}");
    
    // Query 3
        StringBuilder QUERY_getServiceTriples = new StringBuilder();
        QUERY_getServiceTriples.append("PREFIX oa: <http://www.w3.org/ns/oa#> ");
        QUERY_getServiceTriples.append("PREFIX oae: <http://www.ics.forth.gr/isl/oae/core#> ");
        QUERY_getServiceTriples.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
        QUERY_getServiceTriples.append("PREFIX foaf: <http://xmlns.com/foaf/0.1/> ");
        QUERY_getServiceTriples.append("PREFIX dbpedia: <http://dbpedia.org/resource/> ");
        QUERY_getServiceTriples.append("PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> ");
        QUERY_getServiceTriples.append("SELECT DISTINCT ?detectedEntity ?categoryName (count(?position) as ?NumOfOccurrences) ");
        QUERY_getServiceTriples.append("WHERE { ");
        QUERY_getServiceTriples.append("  SERVICE <http://dbpedia.org/resource/Thunnus> { ");
        QUERY_getServiceTriples.append("    dbpedia:Thunnus dbpedia-owl:wikiPageExternalLink ?page } ");
        QUERY_getServiceTriples.append("  VALUES ?templ { <http://139.91.183.72/x-link-marine/api?categories=fish;country&url=PAGE> } BIND(REPLACE(str(?templ), 'PAGE', str(?page), 'i') as ?x) BIND(URI(?x) as ?serv) ");
        QUERY_getServiceTriples.append("  SERVICE ?serv { ");
        QUERY_getServiceTriples.append("    ?annot oa:hasBody ?ent . ");
        QUERY_getServiceTriples.append("    ?ent oae:regardsEntityName ?detectedEntity ; ");
        QUERY_getServiceTriples.append("         oae:position ?position ; ");
        QUERY_getServiceTriples.append("         oae:belongsTo ?category . ?category rdfs:label ?categoryName } ");
        QUERY_getServiceTriples.append("} GROUP BY ?detectedEntity ?categoryName ORDER BY DESC(?NumOfOccurrences)");

    // Query 4    
        StringBuilder QUERY_rdfa_derefUri = new StringBuilder();
        QUERY_rdfa_derefUri.append("PREFIX foaf: <http://xmlns.com/foaf/0.1/> ");
        QUERY_rdfa_derefUri.append("SELECT DISTINCT ?authorName (count(DISTINCT ?paper2) as ?numOfPapers) ");
        QUERY_rdfa_derefUri.append("WHERE { ");
        QUERY_rdfa_derefUri.append("  SERVICE <http://users.ics.forth.gr/~fafalios/> { ");
        QUERY_rdfa_derefUri.append("    ?paper1 <http://purl.org/dc/terms/creator> ?author ");
        QUERY_rdfa_derefUri.append("    FILTER(?author != <http://dblp.l3s.de/d2r/resource/authors/Pavlos_Fafalios>) } ");
        QUERY_rdfa_derefUri.append("  SERVICE ?author { ");
        QUERY_rdfa_derefUri.append("    ?author foaf:name ?authorName . ");
        QUERY_rdfa_derefUri.append("    ?paper2 <http://purl.org/dc/elements/1.1/creator> ?author } ");
        QUERY_rdfa_derefUri.append("} GROUP BY ?authorName ORDER BY DESC(?numOfPapers)");

    // Query 5
        StringBuilder QUERY_rdfa_derefUri_endpoint = new StringBuilder();
        QUERY_rdfa_derefUri_endpoint.append("PREFIX foaf: <http://xmlns.com/foaf/0.1/> ");
        QUERY_rdfa_derefUri_endpoint.append("SELECT DISTINCT ?authorName (count(DISTINCT ?paper) AS ?numOfPapers) (count(DISTINCT ?series) AS ?numOfDiffConfs) WHERE { ");
        QUERY_rdfa_derefUri_endpoint.append("  SERVICE <http://users.ics.forth.gr/~fafalios> { ");
        QUERY_rdfa_derefUri_endpoint.append("    ?p <http://purl.org/dc/terms/creator> ?authorURI } ");
        QUERY_rdfa_derefUri_endpoint.append("  SERVICE ?authorURI { ");
        QUERY_rdfa_derefUri_endpoint.append("    ?author foaf:name ?authorName . ");
        QUERY_rdfa_derefUri_endpoint.append("    ?paper <http://purl.org/dc/elements/1.1/creator> ?authorURI } ");
        QUERY_rdfa_derefUri_endpoint.append("  SERVICE <http://dblp.l3s.de/d2r/sparql> { ");
        QUERY_rdfa_derefUri_endpoint.append("    ?p2 <http://purl.org/dc/elements/1.1/creator> ?authorURI . ");
        QUERY_rdfa_derefUri_endpoint.append("    ?p2 <http://swrc.ontoware.org/ontology#series> ?series } ");
        QUERY_rdfa_derefUri_endpoint.append("} GROUP BY ?authorName ORDER BY DESC(?numOfPapers)");

        //Query 6 (Extended SPARQL-LD)
        StringBuilder QUERY_getDataCatalog = new StringBuilder();
        QUERY_getDataCatalog.append("PREFIX seas: <https://w3id.org/seas/> ");
        QUERY_getDataCatalog.append("PREFIX sosa: <http://www.w3c.org/ns/sosa/> ");
        QUERY_getDataCatalog.append("PREFIX ssn: <http://www.w3c.org/ns/ssn/> ");
        QUERY_getDataCatalog.append("SELECT ?temp_value ?occ_value WHERE { ");
        QUERY_getDataCatalog.append("  SERVICE <http://localhost:8001/data_catalouge.ttl> { ");
        QUERY_getDataCatalog.append("  ?room a seas:Room , sosa:FeatureOfInterest ; ");
        QUERY_getDataCatalog.append("    ssn:hasProperty ?temperature , ?occupancy ; ");
        QUERY_getDataCatalog.append("    seas:onFloor ?floor .");
        QUERY_getDataCatalog.append("  ?temperature sosa:hasSimpleResult ?temp_value .");
        QUERY_getDataCatalog.append("  ?occupancy sosa:hasSimpleResult ?occ_value .");
        QUERY_getDataCatalog.append("  FILTER( ?floor = 2 ");
        QUERY_getDataCatalog.append("    && ?temp_value > 14 ");
        QUERY_getDataCatalog.append("    && ?occ_value = 0 ) }");
        QUERY_getDataCatalog.append("} ");
        
        // Running query 2
        // SET HERE THE QUERY TO RUN 
        QUERY_TO_RUN = QUERY_getDataCatalog.toString();
        URL_DataCatalogue = "http://localhost:8001/DC_TestExample.ttl";
        // Create a Query object
        Query query = QueryFactory.create(QUERY_TO_RUN);
        
        if(URL_DataCatalogue != null && !URL_DataCatalogue.isEmpty()){
            Test2_works.Ex_DataCatalogue(query);
        }
        else{
            // Execute the query and obtain results
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            ResultSet results = qe.execSelect();
            // Output query results	
            ResultSetFormatter.out(System.out, results, query);
            // Free up resources used running the query
            qe.close();
        }
    }    
}
