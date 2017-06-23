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
package org.apache.jena.sparql.engine.http;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

public class SparqlLDExtended {

    private static List list_trip = new ArrayList();
    private static Set <Node> list_subjects = new HashSet<Node>();
    private static Model DC_model;
    private static StringBuilder Result_Values = new StringBuilder();
        
    public static String Execute(Query query, Model DataCat_model){
        DC_model = DataCat_model;
        // Parsing the input query to get the triples that need to be searched for later
        GetTriples(query);
        // Parsing the DC to see the triples that can be found + EP URL + LR URL
        Parse_DC(query);
        // FINAL RESULT SET FEDERATED IN A STRING *Later, can be used or modified as needed*
        return Result_Values.toString();
    }
    
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
    }    

    public static void Parse_DC(Query query){
        System.out.println("# Parsing the Data Catalogue.");
        ElementWalker.walk(query.getQueryPattern(),
            new ElementVisitorBase() {
                public void visit(ElementPathBlock el) {
                    Iterator<Node> iterator = list_subjects.iterator();
                    while(iterator.hasNext()){
                        Node sub_element = iterator.next();
                        Iterator<TriplePath> triples = el.patternElts();
                        String ASK_QUERY_TO_RUN;
                        String ASK_QUERY_TO_RUN_EP; 
                        String ASK_QUERY_TO_RUN_EP_LR;
                        StringBuilder ASK_Sub_query = new StringBuilder();
                        StringBuilder SELECT_Sub_query = new StringBuilder();
                        StringBuilder SELECT_EPLR_Sub_query = new StringBuilder();
                        StringBuilder ASK_EP_Query = new StringBuilder();
                        StringBuilder ASK_EP_LR_Query = new StringBuilder();
                        Resource ep = null;
                        Resource lr = null;
                        ASK_Sub_query.append("ASK { ").append("\n");
                        SELECT_Sub_query.append("SELECT * WHERE { ").append("\n");
                        SELECT_EPLR_Sub_query.append("SELECT ?ep ?lr WHERE {").append("\n");
                        while (triples.hasNext()) {
                            final TriplePath tp = triples.next();
                            if(tp.getSubject() == sub_element){
                                Node predicate = tp.getPredicate();
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
                        ASK_QUERY_TO_RUN_EP_LR = ASK_EP_LR_Query.toString();
                        ASK_QUERY_TO_RUN_EP = ASK_EP_Query.toString();
                        ASK_QUERY_TO_RUN = ASK_Sub_query.toString();
                        //Check for EP + LR
                        Query sub_quer_ep_lr = QueryFactory.create(ASK_QUERY_TO_RUN_EP_LR);
                        boolean LR_EP_triple_present = Execute_ASK_Subquery(sub_quer_ep_lr);
                        System.out.println("\n" + "Subject: " + sub_element.toString());
                        System.out.println("LR present in DC: " + LR_EP_triple_present);
                        System.out.println("Back from executing ASK subquery with LR + EP check");
                        boolean EP_triple_present = false;

                        if(LR_EP_triple_present){
                            // Check DC for the endpoint and also lifting rule.
                            System.out.println("EP + LR PRESENT");                                                     
                            StringBuilder Get_EP_LR = new StringBuilder();
                            Get_EP_LR.append("SELECT ?ep ?lr WHERE {").append("\n");
                            Get_EP_LR.append(sub_element).append(" <http://example.org/data/endpoint>").append(" ?ep.").append("\n");
                            Get_EP_LR.append(sub_element).append(" <http://example.org/data/liftingrule>").append(" ?lr.").append("\n");
                            Get_EP_LR.append("}");
                            
                            String Q_Get_EP_LR = SELECT_EPLR_Sub_query.toString();
                            Query query_EP_LR = QueryFactory.create(Q_Get_EP_LR);
                            QueryExecution qe_EP_LR = QueryExecutionFactory.create(query_EP_LR, DC_model);
                            ResultSet results_EP_LR = qe_EP_LR.execSelect();
                            //ResultSetFormatter.out(System.out, results_EP_LR, query_EP_LR);
                            
                            while (results_EP_LR.hasNext())
                            {
                                System.out.println("Inside the resultset");
                                QuerySolution binding = results_EP_LR.nextSolution();
                                ep = (Resource) binding.get("ep");
                                lr = (Resource) binding.get("lr");
                                System.out.println("EP URL: "+ ep.getURI());
                                System.out.println("LR URL: "+ lr.getURI());
                            }
                            Model final_model = ModelFactory.createDefaultModel();
                            try {
                                final_model = bind_data(ep,lr);
                            } catch (IOException ex) {
                                Logger.getLogger(SparqlLDExtended.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println("Content lifted.");
                            String final_QUERY = SELECT_Sub_query.toString();
                            //System.out.println("## FINAL SUB-QUERY FORMED:\n" + final_QUERY);
                            QueryExecution qe = (QueryExecutionBase) QueryExecutionFactory.create(final_QUERY, final_model);
                            ResultSet resultSet = qe.execSelect();
                            Result_Values.append(ResultSetFormatter.asText(resultSet));
                            qe_EP_LR.close();
                        }
                        else if(!LR_EP_triple_present ){
                            System.out.println("LR not present");
                            Query sub_quer_ep = QueryFactory.create(ASK_QUERY_TO_RUN_EP);
                            EP_triple_present = Execute_ASK_Subquery(sub_quer_ep);
                            System.out.println("EP present in DC: " + EP_triple_present);
                            System.out.println("Back from executing ASK subquery with EP Check");
                            // Future Works: Add functionality if end-point is available.
                        }
                        if(!EP_triple_present && !LR_EP_triple_present) {
                            System.out.println("EP not present");
                            Query sub_query = QueryFactory.create(ASK_QUERY_TO_RUN);
                            boolean triple_present = Execute_ASK_Subquery(sub_query);
                            System.out.println("Triple present in DC: " + triple_present);
                            System.out.println("Back from executing ASK subquery");
                            //Get info from Data Catalogue directly.
                            if(triple_present){
                                String Get_info_fromDC = SELECT_Sub_query.toString();
                                Query DC_Select_query = QueryFactory.create(Get_info_fromDC);
                                QueryExecution result_info_DC = QueryExecutionFactory.create(DC_Select_query, DC_model);
                                System.out.println("Sending the SELECT query to DC to get info.");
                                ResultSet results_info_DC = result_info_DC.execSelect();
                                Result_Values.append(ResultSetFormatter.asText(results_info_DC));
                                //ResultSetFormatter.out(System.out, results_info_DC, DC_Select_query);
                                result_info_DC.close();
                            }
                        }
                    }
                }
            }
        );
    }
    
    public static boolean Execute_ASK_Subquery(Query input_subquery){
        boolean DC_result = false;
        if(DC_model != null && !DC_model.isEmpty()) {
            QueryExecution qe_dc = QueryExecutionFactory.create(input_subquery, DC_model);
            System.out.println("Checking the DC for endpoints. Sending the subquery...");
            DC_result = qe_dc.execAsk();
            qe_dc.close();
        }
        return(DC_result);
    }

    public static Model bind_data(Resource ep, Resource lr) throws IOException{
        System.out.println("# Unknown format.Hence using the lifting rule available....");
        String URL_ep = ep.toString();
        String URL_lr = lr.toString();
        URL url = new URL(URL_ep);
        URLConnection conn = url.openConnection();
        conn.connect();
        URL contentLiftingRuleUrl = new URL(URL_lr);
        String source = IOUtils.toString(conn.getInputStream());
        URLConnection contentLiftingRuleConn = contentLiftingRuleUrl.openConnection();
        contentLiftingRuleConn.connect();
        String liftingRule = IOUtils.toString(contentLiftingRuleConn.getInputStream());
        SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create(liftingRule, SPARQLGenerate.SYNTAX);
        PlanFactory factory = new PlanFactory();
        RootPlan plan = factory.create(query);
        Model final_model = ModelFactory.createDefaultModel();
        String variable = "source";
        Node arqLiteral = NodeFactory.createLiteral(source);
        RDFNode jenaLiteral = final_model.asRDFNode(arqLiteral);
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add(variable, jenaLiteral);
        plan.exec(initialBinding, final_model);
        return(final_model);
    }
}