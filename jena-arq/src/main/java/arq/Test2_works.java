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

import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.query.Query;
import org.apache.jena.util.iterator.Filter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import java.util.*;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFReader;

/**
 *
 * @author Sejal
 */
public class Test2_works {

    private static String QUERY_TO_RUN; // The query to run
    private static String URL_DataCatalogue;
    //Used only if we need to check for 'endpoint' info in Data Catalogue
    private static String ask_endpoint_query = "ASK { ?x <http://example.org/data/endpoint> ?z }";
    private static String ask_liftingrule_query = "ASK { ?x <http://example.org/data/liftingrule> ?z }";
    //List of subject is populated only if list_triples is looked up
    private static Set <Node> list_subjects = new HashSet<Node>();
    private static List list_trip = new ArrayList();
    private static Model DC_model;
    
    public static void GetTriples(Query query){
        try{
            System.out.println("# Populating list of triples and unique subjects in the Input Query.");
            ElementWalker.walk(query.getQueryPattern() , 
                new ElementVisitorBase() {
                    @Override
                    public void visit(ElementPathBlock el) {
                        ListIterator<TriplePath> it = el.getPattern().iterator();
                        while ( it.hasNext() ) {
                            final TriplePath tp = it.next();
                            list_trip.add(tp);
                            list_subjects.add(tp.getSubject());
                    }
                }
            });
        } catch(Exception e){
            System.out.println("Caught an exception");
            e.printStackTrace();
        }
        //return(list_trip);
    }
    
    public static void Parse_DC(Query query){
        System.out.println("# Parsing the Data Catalogue.");
        ElementWalker.walk(query.getQueryPattern(),
            new ElementVisitorBase() {
                public void visit(ElementPathBlock el) {
                    Iterator<Node> iterator = list_subjects.iterator();
                    while(iterator.hasNext()){
                        Node sub_element = iterator.next();
                        System.out.println("### Subject: "+ sub_element);
                        Iterator<TriplePath> triples = el.patternElts();
                        //StringBuilder select_statement = new StringBuilder();
                        StringBuilder ASK_Sub_query = new StringBuilder();
                        StringBuilder SELECT_Sub_query = new StringBuilder();
                        Resource ep = null;
                        Resource lr = null;
                        //Sub_query.append("PREFIX seas: <https://w3id.org/seas/>").append("\n");
                        ASK_Sub_query.append("ASK { ").append("\n");
                        //select_statement.append("SELECT * WHERE { ").append("\n");
                        SELECT_Sub_query.append("SELECT * WHERE { ").append("\n");
                        while (triples.hasNext()) {
                            final TriplePath tp = triples.next();
                            //System.out.println("##Subject: "+sub_element);
                            //System.out.println("Checking subject:"+ tp.getSubject());
                            if(tp.getSubject() == sub_element){
                                //System.out.println("##Subject: "+sub_element);
                                //System.out.println("##Predicate: "+tp.getPredicate());
                                Node predicate = tp.getPredicate();
                                //System.out.println("##Object: "+tp.getObject());
                                Node object = tp.getObject();
                                
                                if(object.isURI() & predicate.isURI()){
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append("<").append(tp.getObject()).append(">");
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append("<").append(tp.getObject()).append(">");
                                } else if(predicate.isURI()) {
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append(tp.getObject());
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append(tp.getObject());
                                } else if(object.isURI()){
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append("<").append(tp.getObject()).append(">");
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append("<").append(tp.getObject()).append(">");
                                } else {
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append(tp.getObject());
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append(tp.getObject());
                                    //Sub_query.append(tp);
                                }
                                ASK_Sub_query.append(".").append("\n");
                                SELECT_Sub_query.append(".").append("\n");
                            }
                        }                         
                        ASK_Sub_query.append("} ");
                        SELECT_Sub_query.append("} ");
                        System.out.println("ASK Subquery formed: \n" + ASK_Sub_query);
                        //System.out.println("SELECT Subquery formed: \n" + SELECT_Sub_query);
                        QUERY_TO_RUN = ASK_Sub_query.toString();
                        Query sub_query = QueryFactory.create(QUERY_TO_RUN);
                        //try{
                            boolean triple_present = Execute_ASK_Subquery(sub_query);
                        //} catch(Exception e){
                            //System.out.println("Caught an exception when executing subquery");
                            //e.printStackTrace();
                        //}
                        System.out.println("Triple present in DC: " + triple_present);
                        System.out.println("Back from executing ASK subquery");
                        if(triple_present){
                            //Check DC for the endpoint and also lifting rule.
                            Query ask_endpoint = QueryFactory.create(ask_endpoint_query);
                            Query ask_liftingrule = QueryFactory.create(ask_liftingrule_query);
                            //Check for endpoint if available.
                            QueryExecution q_endpoint = QueryExecutionFactory.create(ask_endpoint, DC_model);
                            boolean Endpoint_result = q_endpoint.execAsk();
                            System.out.println("Endpoint available: " + Endpoint_result);
                            q_endpoint.close();
                            //Check for lifting rule, if available means the format is UNKNOWN.
                            QueryExecution q_lifting_rule = QueryExecutionFactory.create(ask_liftingrule, DC_model);
                            boolean LiftingRule_result = q_lifting_rule.execAsk();
                            System.out.println("Lifting rule available: " + LiftingRule_result);
                            q_lifting_rule.close();
                                                        
                            //If liftingrule available, then fetch the results from endpoint and then lift it.
                            if(LiftingRule_result && Endpoint_result){
                                StringBuilder Get_EP_LR = new StringBuilder();
                                Get_EP_LR.append("SELECT ?ep ?lr WHERE {").append("\n");
                                Get_EP_LR.append(sub_element).append(" <http://example.org/data/endpoint>").append(" ?ep.").append("\n");
                                Get_EP_LR.append(sub_element).append(" <http://example.org/data/liftingrule>").append(" ?lr.").append("\n");
                                Get_EP_LR.append("}");

                                String Q_Get_EP_LR = Get_EP_LR.toString();
                                Query query_EP_LR = QueryFactory.create(Q_Get_EP_LR);
                                //System.out.println(query_EP_LR);
                                QueryExecution qe_EP_LR = QueryExecutionFactory.create(query_EP_LR, DC_model);
                                ResultSet results_EP_LR = qe_EP_LR.execSelect();
                                //ResultSetFormatter.out(System.out, results_EP_LR, query_EP_LR);
                                while (results_EP_LR.hasNext())
                                {
                                    //System.out.println("Inside the resultset");
                                    QuerySolution binding = results_EP_LR.nextSolution();
                                    ep = (Resource) binding.get("ep");
                                    lr = (Resource) binding.get("lr");
                                    System.out.println("EP URL: "+ ep.getURI());
                                    System.out.println("LR URL: "+ lr.getURI());
                                }
                                
                                //select_statement.append(" SERVICE <").append(ep.toString()).append("> { \n");
                                //SELECT_Sub_query.insert(0, select_statement);
                                //SELECT_Sub_query.append("}");
                                //System.out.println("## FINAL SUB-QUERY FORMED:\n" + SELECT_Sub_query);
                                try {
                                    Model final_model = ModelFactory.createDefaultModel();
                                    final_model = bind_data(ep, lr);
                                    System.out.println("Content lifted.");
                                    String final_QUERY = SELECT_Sub_query.toString();
                                    System.out.println("## FINAL SUB-QUERY FORMED:\n" + final_QUERY);
                                    QueryExecution qe = (QueryExecutionBase) QueryExecutionFactory.create(final_QUERY, final_model);
                                    ResultSet resultSet = qe.execSelect();
                                    ResultSetFormatter.out(System.out, resultSet, query);
                                } catch (IOException ex) {
                                    Logger.getLogger(Test2_works.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                qe_EP_LR.close();
                            } 
                        }
                    }
                }
            }
        );
    }
    
    public static Model bind_data(Resource ep, Resource lr) throws IOException{
        System.out.println("# Unknown format.Hence using the lifting rule available....");
        String URL_ep = ep.toString();
        String URL_lr = lr.toString();
        //SPARQL Generate stuff
        URL url = new URL(URL_ep);
        URLConnection conn = url.openConnection();
        conn.connect();
        //URL contentLiftingRuleUrl = new URL(conn.getHeaderField("Content-Lifting-Rule"));
        URL contentLiftingRuleUrl = new URL(URL_lr);
        String source = IOUtils.toString(conn.getInputStream());
        URLConnection contentLiftingRuleConn = contentLiftingRuleUrl.openConnection();
        contentLiftingRuleConn.connect();
        String liftingRule = IOUtils.toString(contentLiftingRuleConn.getInputStream());
        SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create(liftingRule, SPARQLGenerate.SYNTAX);
        PlanFactory factory = new PlanFactory();
        RootPlan plan = factory.create(query);
        //System.out.println("Reached here # "+ query);
        Model final_model = ModelFactory.createDefaultModel();
        String variable = "source";
        Node arqLiteral = NodeFactory.createLiteral(source);
        //System.out.println("Reached here ## "+ arqLiteral);
        RDFNode jenaLiteral = final_model.asRDFNode(arqLiteral);
        //System.out.println("Reached here ### "+ jenaLiteral);

        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add(variable, jenaLiteral);
        plan.exec(initialBinding, final_model);
        System.out.println("Populated model "+ final_model);
        return(final_model);
        /*
        QueryExecution qe = (QueryExecutionBase) QueryExecutionFactory.create(query, final_model);
        ResultSet resultSet = qe.execSelect();
        ResultSetFormatter.out(System.out, resultSet, query);
        */
    }
    
    public static boolean Execute_ASK_Subquery(Query input_subquery){
        boolean DC_result = false;
        //System.out.println("Inside ExecuteSubquery");
        if(URL_DataCatalogue != null && !URL_DataCatalogue.isEmpty()) {
            QueryExecution qe_dc = QueryExecutionFactory.create(input_subquery, DC_model);
            System.out.println("Checking the DC for endpoints. Sending the subquery...");
            DC_result = qe_dc.execAsk();
            //System.out.println(DC_result);
            qe_dc.close();
        }
        return(DC_result);
    }
    
    public static void main(String[] args) {
        // Input query
        StringBuilder QUERY_getDataCatalog = new StringBuilder();
        QUERY_getDataCatalog.append("PREFIX seas: <https://w3id.org/seas/> ");
        QUERY_getDataCatalog.append("PREFIX sosa: <http://www.w3c.org/ns/sosa/> ");
        QUERY_getDataCatalog.append("PREFIX ssn: <http://www.w3c.org/ns/ssn/> ");
        QUERY_getDataCatalog.append("SELECT ?actuator WHERE { ");
        QUERY_getDataCatalog.append("  SERVICE <http://localhost:8001/data_catalouge.ttl> { ");
        QUERY_getDataCatalog.append("  ?room a seas:Room , sosa:FeatureOfInterest ; ");
        QUERY_getDataCatalog.append("    ssn:hasProperty ?temperature , ?occupancy ; ");
        QUERY_getDataCatalog.append("    seas:onFloor ?floor . ");
        QUERY_getDataCatalog.append("  ?temperature a sosa:ObservableProperty , seas:TemperatureProperty . ");
        QUERY_getDataCatalog.append("  ?occupancy a sosa:ObservableProperty , seas:OccupancyProperty . ");
        QUERY_getDataCatalog.append("  ?temp_obs a sosa:Observation ; ");
        QUERY_getDataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_getDataCatalog.append("    sosa:observedProperty ?temperature ; ");
        QUERY_getDataCatalog.append("    sosa:hasSimpleResult ?temp_value . ");
        QUERY_getDataCatalog.append("  ?occ_obs a sosa:Observation ; ");
        QUERY_getDataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_getDataCatalog.append("    sosa:observedProperty ?occupancy ; ");
        QUERY_getDataCatalog.append("    sosa:hasSimpleResult ?occ_value . ");
        QUERY_getDataCatalog.append("  ?actuator a seas:Heater , sosa:Actuator ; ");
        QUERY_getDataCatalog.append("    sosa:actsOnProperty ?temperature . ");
        QUERY_getDataCatalog.append("  FILTER( ?floor = 2 ");
        QUERY_getDataCatalog.append("    && ?temp_value > 14 ");
        QUERY_getDataCatalog.append("    && ?occ_value = 0 ) }");
        QUERY_getDataCatalog.append("} ");
    
        QUERY_TO_RUN = QUERY_getDataCatalog.toString();
        Query input_query = QueryFactory.create(QUERY_TO_RUN);
        URL_DataCatalogue = "http://localhost:8001/DC_TestExample.ttl";
        DC_model = ModelFactory.createDefaultModel();
        DC_model.read(URL_DataCatalogue);
        //Parsing the input query to get the triples that need to be searched for later
        GetTriples(input_query);
        //Parsing the DC to see the triples that can be found + EP URL + LR URL
        Parse_DC(input_query);
    }
}
