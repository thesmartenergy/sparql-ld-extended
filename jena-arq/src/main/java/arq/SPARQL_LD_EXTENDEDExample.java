/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.jena.sparql.engine.http.SparqlLDExtended;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Examples that exploit the functionality offered by SPARQL-LD.
 * @author Pavlos Fafalios (fafalios@ics.forth.gr, fafalios.pavlos@gmail.com)
 *
 * Examples that exploit the functionality offered by SPARQL-LD extended.
 * @author Sejal Jaiswal (cj.sejal@gmail.com)
 */
public class SPARQL_LD_EXTENDEDExample {

    private static String QUERY_TO_RUN; // The query to run
    private static String URL_DataCatalogue; //URL of the Data Catalogue if available
    private static Model DC_model; //Model to read the data catalogue into
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
    
    // Query 6
        StringBuilder QUERY_DataCatalog = new StringBuilder();
        QUERY_DataCatalog.append("PREFIX seas: <https://w3id.org/seas/> ");
        QUERY_DataCatalog.append("PREFIX sosa: <http://www.w3c.org/ns/sosa/> ");
        QUERY_DataCatalog.append("PREFIX ssn: <http://www.w3c.org/ns/ssn/> ");
        QUERY_DataCatalog.append("PREFIX ns: <http://purl.org/acco/ns#> ");
        QUERY_DataCatalog.append("SELECT ?room WHERE { ");
        QUERY_DataCatalog.append("  SERVICE <http://localhost:8001/data_catalouge.ttl> { ");
        QUERY_DataCatalog.append("  ?room a seas:Room , sosa:FeatureOfInterest ; ");
        QUERY_DataCatalog.append("    ssn:hasProperty seas:TemperatureProperty , seas:OccupancyProperty ; ");
        QUERY_DataCatalog.append("    seas:onFloor ?floor . ");
        QUERY_DataCatalog.append("  ?temperature a sosa:ObservableProperty , seas:TemperatureProperty . ");
        QUERY_DataCatalog.append("  ?occupancy a sosa:ObservableProperty , seas:OccupancyProperty . ");
        QUERY_DataCatalog.append("  ?temp_obs a sosa:Observation ; ");
        QUERY_DataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_DataCatalog.append("    sosa:observedProperty seas:temperature ; ");
        QUERY_DataCatalog.append("    sosa:hasSimpleResult ?temp_value . ");
        QUERY_DataCatalog.append("  ?occ_obs a sosa:Observation ; ");
        QUERY_DataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_DataCatalog.append("    sosa:observedProperty ns:Occupancy ; ");
        QUERY_DataCatalog.append("    sosa:hasSimpleResult ?occ_value . ");
        QUERY_DataCatalog.append("}");
        QUERY_DataCatalog.append("} ");
        
    // Running quer 2
        // SET HERE THE QUERY TO RUN 
        QUERY_TO_RUN = QUERY_DataCatalog.toString();
        
        // SET HERE THE URL OF THE DATA CATALOGUE IF AVAILABLE
        URL_DataCatalogue = "http://localhost:8001/Simple_example_DC.ttl";
        
        // Create a Query object
        Query query = QueryFactory.create(QUERY_TO_RUN);
        
        // if only using SPARQL-LD functionalities and NO Data Catalogue is provided
        if(URL_DataCatalogue == null && URL_DataCatalogue.isEmpty())
        {
            // Execute the query and obtain results
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            ResultSet results = qe.execSelect();

            // Output results	
            if(results != null){
                System.out.println("Printing result");
                ResultSetFormatter.out(System.out, results, query);
            }
            // Free up resources used running the query
            qe.close();
        } // Using extended SPARQL-LD functionalities where Data Catalogue URL is provided
        else if(URL_DataCatalogue != null && !URL_DataCatalogue.isEmpty()){
            // Read Data Catalogue into a model
            DC_model = ModelFactory.createDefaultModel();
            DC_model.read(URL_DataCatalogue);
            // FINAL RESULT SET FEDERATED IN A STRING *Later, can be used or modified as needed*
            String Result = SparqlLDExtended.Execute(query,DC_model );
            System.out.println(Result);
        }
    }
}
