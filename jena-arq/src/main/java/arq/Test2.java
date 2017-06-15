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
public class Test2{

    private static String QUERY_TO_RUN;
    private static String QUERY_TO_RUN_EP; 
    private static String QUERY_TO_RUN_EP_LR;// The query to run
    private static String URL_DataCatalogue;
    private static StringBuilder ask_endpoint_query = new StringBuilder();
    private static StringBuilder ask_liftingrule_query = new StringBuilder();
    private static StringBuilder Result_Values = new StringBuilder();
    //List of subject is populated only if list_triples is looked up
    private static Set <Node> list_subjects = new HashSet<Node>();
    private static List list_trip = new ArrayList();
    private static Model DC_model;
    private static ResultSet Federate_ResultSet;
    
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
                            //System.out.println(list_trip);
                            list_subjects.add(tp.getSubject());
                            //System.out.println(list_trip);
                    }
                }
            });
        } catch(Exception e){
            System.out.println("Caught an exception");
            e.printStackTrace();
        }
        //return(list_trip);
    }
    
    public static void Check_EP_LR(Node sub_element){
        ask_endpoint_query = new StringBuilder();
        ask_liftingrule_query= new StringBuilder();
        ask_endpoint_query.append("ASK { ").append(sub_element.toString()).append(" <http://example.org/data/endpoint> ?z }");
        System.out.println("#ENDPOINT ASK: " + ask_endpoint_query.toString() +"\n");
        ask_liftingrule_query.append("ASK { ").append(sub_element.toString()).append(" <http://example.org/data/liftingrule> ?z }");
        System.out.println("#LiftingRule ASK: " + ask_liftingrule_query.toString() +"\n");
        return;
    }
        
    public static void Parse_DC(Query query){
        //System.out.println("### Subjects: "+ list_subjects);
        System.out.println("# Parsing the Data Catalogue.");
        ElementWalker.walk(query.getQueryPattern(),
            new ElementVisitorBase() {
                public void visit(ElementPathBlock el) {
                    Iterator<Node> iterator = list_subjects.iterator();
                    while(iterator.hasNext()){
                        Node sub_element = iterator.next();
                        //System.out.println("### Subject: "+ sub_element);
                        Iterator<TriplePath> triples = el.patternElts();
                        //StringBuilder select_statement = new StringBuilder();
                        StringBuilder ASK_Sub_query = new StringBuilder();
                        StringBuilder SELECT_Sub_query = new StringBuilder();
                        StringBuilder SELECT_EPLR_Sub_query = new StringBuilder();
                        StringBuilder ASK_EP_Query = new StringBuilder();
                        StringBuilder ASK_EP_LR_Query = new StringBuilder();
                        Resource ep = null;
                        Resource lr = null;
                        ASK_Sub_query.append("ASK { ").append("\n");
                        //select_statement.append("SELECT * WHERE { ").append("\n");
                        SELECT_Sub_query.append("SELECT * WHERE { ").append("\n");
                        SELECT_EPLR_Sub_query.append("SELECT ?ep ?lr WHERE {").append("\n");
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
                                    SELECT_EPLR_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append("<").append(tp.getObject()).append(">");
                                } else if(predicate.isURI()) {
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append(tp.getObject());
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append(tp.getObject());
                                    SELECT_EPLR_Sub_query.append(tp.getSubject()).append(" ").append("<").append(tp.getPredicate()).append(">").append(" ").append(tp.getObject());
                                } else if(object.isURI()){
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append("<").append(tp.getObject()).append(">");
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append("<").append(tp.getObject()).append(">");
                                    SELECT_EPLR_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append("<").append(tp.getObject()).append(">");
                                } else {
                                    ASK_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append(tp.getObject());
                                    SELECT_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append(tp.getObject());
                                    SELECT_EPLR_Sub_query.append(tp.getSubject()).append(" ").append(tp.getPredicate()).append(" ").append(tp.getObject());
                                }
                                //Check_EP_LR(tp.getSubject());
                                ASK_Sub_query.append(".").append("\n");
                                SELECT_Sub_query.append(".").append("\n");
                                SELECT_EPLR_Sub_query.append(".").append("\n");
                            }
                        }
                        ASK_EP_Query.append(ASK_Sub_query).append(sub_element.toString()).append(" <http://example.org/data/endpoint> ?ep .").append("\n");
                        ASK_EP_LR_Query.append(ASK_EP_Query).append(sub_element.toString()).append(" <http://example.org/data/liftingrule> ?lr .").append("\n");
                        SELECT_EPLR_Sub_query.append(sub_element.toString()).append(" <http://example.org/data/endpoint> ?ep .").append("\n");
                        SELECT_EPLR_Sub_query.append(sub_element.toString()).append(" <http://example.org/data/liftingrule> ?lr .").append("\n").append("}");
                        ASK_EP_LR_Query.append("}");
                        ASK_EP_Query.append("}");
                        ASK_Sub_query.append("} ");
                        SELECT_Sub_query.append("} ");
                        System.out.println("\n #Subject: " + sub_element);
                        System.out.println("SELECT Subquery with EP + LR formed: \n" + SELECT_EPLR_Sub_query);
                        QUERY_TO_RUN_EP_LR = ASK_EP_LR_Query.toString();
                        QUERY_TO_RUN_EP = ASK_EP_Query.toString();
                        QUERY_TO_RUN = ASK_Sub_query.toString();
                        //Check for EP + LR
                        Query sub_quer_ep_lr = QueryFactory.create(QUERY_TO_RUN_EP_LR);
                        boolean LR_EP_triple_present = Execute_ASK_Subquery(sub_quer_ep_lr);
                        System.out.println("LR present in DC: " + LR_EP_triple_present);
                        System.out.println("Back from executing ASK subquery with LR + EP check");
                        boolean EP_triple_present = false;
                        
                        if(LR_EP_triple_present){
                            //Check DC for the endpoint and also lifting rule.
                            System.out.println("EP + LR PRESENT");                                                     
                            StringBuilder Get_EP_LR = new StringBuilder();
                            Get_EP_LR.append("SELECT ?ep ?lr WHERE {").append("\n");
                            Get_EP_LR.append(sub_element).append(" <http://example.org/data/endpoint>").append(" ?ep.").append("\n");
                            Get_EP_LR.append(sub_element).append(" <http://example.org/data/liftingrule>").append(" ?lr.").append("\n");
                            Get_EP_LR.append("}");

                            //String Q_Get_EP_LR = Get_EP_LR.toString();
                            //Query query_EP_LR = QueryFactory.create(Q_Get_EP_LR);
                            String Q_Get_EP_LR = SELECT_EPLR_Sub_query.toString();
                            Query query_EP_LR = QueryFactory.create(Q_Get_EP_LR);
                            System.out.println(query_EP_LR);
                            QueryExecution qe_EP_LR = QueryExecutionFactory.create(query_EP_LR, DC_model);
                            ResultSet results_EP_LR = qe_EP_LR.execSelect();
                            //ResultSetFormatter.out(System.out, results_EP_LR, query_EP_LR);
                            while (results_EP_LR.hasNext())
                            {
                                System.out.println("Inside the resultset");
                                //QuerySolution binding = Federate_ResultSet.nextSolution();
                                QuerySolution binding = results_EP_LR.nextSolution();
                                ep = (Resource) binding.get("ep");
                                lr = (Resource) binding.get("lr");
                                System.out.println("EP URL: "+ ep.getURI());
                                System.out.println("LR URL: "+ lr.getURI());
                            }
                            try {
                                Model final_model = ModelFactory.createDefaultModel();
                                final_model = bind_data(ep, lr);
                                System.out.println("Content lifted.");
                                String final_QUERY = SELECT_Sub_query.toString();
                                System.out.println("## FINAL SUB-QUERY FORMED:\n" + final_QUERY);
                                QueryExecution qe = (QueryExecutionBase) QueryExecutionFactory.create(final_QUERY, final_model);
                                ResultSet resultSet = qe.execSelect();
                                //Federate_ResultSet = qe.execSelect();
                                Result_Values.append(ResultSetFormatter.asText(resultSet));
                                ResultSetFormatter.out(System.out, resultSet, query);
                            } catch (IOException ex) {
                                Logger.getLogger(Test2_works.class.getName()).log(Level.SEVERE, null, ex);
                            } 
                            qe_EP_LR.close();
                        }
                        else if(!LR_EP_triple_present ){
                            System.out.println("LR not present");
                            Query sub_quer_ep = QueryFactory.create(QUERY_TO_RUN_EP);
                            EP_triple_present = Execute_ASK_Subquery(sub_quer_ep);
                            System.out.println("EP present in DC: " + EP_triple_present);
                            System.out.println("Back from executing ASK subquery with EP Check");
                            // TO DO: put what to do if only EP is available.
                        }
                        
                        if(!EP_triple_present && !LR_EP_triple_present) {
                            System.out.println("EP not present");
                            Query sub_query = QueryFactory.create(QUERY_TO_RUN);
                            boolean triple_present = Execute_ASK_Subquery(sub_query);
                            System.out.println("Triple present in DC: " + triple_present);
                            System.out.println("Back from executing ASK subquery");
                            //Get info from Data Catalogue directly.
                            if(triple_present){
                                String Get_info_fromDC = SELECT_Sub_query.toString();
                                Query DC_Select_query = QueryFactory.create(Get_info_fromDC);
                                QueryExecution result_info_DC = QueryExecutionFactory.create(DC_Select_query, DC_model);
                                System.out.println("Sending the SELECT query to DC to get info.");
                                //ResultSet results_info_DC = result_info_DC.execSelect();
                                Federate_ResultSet = result_info_DC.execSelect();
                                Result_Values.append(ResultSetFormatter.asText(Federate_ResultSet));
                                ResultSetFormatter.out(System.out, Federate_ResultSet, DC_Select_query);
                                //ResultSetFormatter.out(System.out, results_info_DC, DC_Select_query);
                                result_info_DC.close();
                            }
                        }
                    }
                }
            }
        );
        System.out.println(Result_Values);
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
    
    public static void Ex_DataCatalogue(Query input_query){
        DC_model = ModelFactory.createDefaultModel();
        DC_model.read(URL_DataCatalogue);
        //Parsing the input query to get the triples that need to be searched for later
        GetTriples(input_query);
        //Parsing the DC to see the triples that can be found + EP URL + LR URL
        Parse_DC(input_query);
    }
    
    public static void main(String[] args) {
        // Input query
        StringBuilder QUERY_getDataCatalog = new StringBuilder();
        QUERY_getDataCatalog.append("PREFIX seas: <https://w3id.org/seas/> ");
        QUERY_getDataCatalog.append("PREFIX sosa: <http://www.w3c.org/ns/sosa/> ");
        QUERY_getDataCatalog.append("PREFIX ssn: <http://www.w3c.org/ns/ssn/> ");
        QUERY_getDataCatalog.append("PREFIX ns: <http://purl.org/acco/ns#> ");
        QUERY_getDataCatalog.append("SELECT ?room WHERE { ");
        QUERY_getDataCatalog.append("  SERVICE <http://localhost:8001/data_catalouge.ttl> { ");
        QUERY_getDataCatalog.append("  ?room a seas:Room , sosa:FeatureOfInterest ; ");
        QUERY_getDataCatalog.append("    ssn:hasProperty seas:TemperatureProperty , seas:OccupancyProperty ; ");
        QUERY_getDataCatalog.append("    seas:onFloor ?floor . ");
        QUERY_getDataCatalog.append("  ?temperature a sosa:ObservableProperty , seas:TemperatureProperty . ");
        QUERY_getDataCatalog.append("  ?occupancy a sosa:ObservableProperty , seas:OccupancyProperty . ");
        QUERY_getDataCatalog.append("  ?temp_obs a sosa:Observation ; ");
        QUERY_getDataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_getDataCatalog.append("    sosa:observedProperty seas:temperature ; ");
        QUERY_getDataCatalog.append("    sosa:hasSimpleResult ?temp_value . ");
        QUERY_getDataCatalog.append("  ?occ_obs a sosa:Observation ; ");
        QUERY_getDataCatalog.append("    sosa:hasFeatureOfInterest ?room ; ");
        QUERY_getDataCatalog.append("    sosa:observedProperty ns:Occupancy ; ");
        QUERY_getDataCatalog.append("    sosa:hasSimpleResult ?occ_value . ");
        //QUERY_getDataCatalog.append("  ?actuator a sosa:Actuator ; ");
        //QUERY_getDataCatalog.append("    sosa:actsOnProperty ?temperature . ");
        QUERY_getDataCatalog.append("}");
        QUERY_getDataCatalog.append("} ");
    
        QUERY_TO_RUN = QUERY_getDataCatalog.toString();
        Query input_query = QueryFactory.create(QUERY_TO_RUN);
        URL_DataCatalogue = "http://localhost:8001/Simple_example_DC.ttl";
        DC_model = ModelFactory.createDefaultModel();
        DC_model.read(URL_DataCatalogue);
        //Parsing the input query to get the triples that need to be searched for later
        GetTriples(input_query);
        //Parsing the DC to see the triples that can be found + EP URL + LR URL
        Parse_DC(input_query);
    }
}
