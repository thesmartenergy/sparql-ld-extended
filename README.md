# SPARQL-LD Extended
## Towards Federated Query for Web of Things Devices

In this project, we are interested in querying indifferently SPARQL endpoints, RDF documents, and non-RDF document exposed by WoT devices 
in a flexible and extensible way. More precisely, we describe an envisioned end-to-end solution for rewriting a user's query into a 
federated query that targets only the relevant WoT devices.

The core of this solution is an extension of SPARQL-LD where the SPARQL **SERVICE** clause can be used to query also non-RDF sources for which we know some RDF lifting mechanism.

## Querying Documents in Arbitary Formats
In order to query one or more SPARQL Protocol services, one can use the principles of the SPARQL 1.1 the **SERVICE** operator for federated 
SPARQL queries. SPARQL 1.1 Federated Query allows for combining group graph patterns that are to be evaluated over several SPARQL Protocol 
services within a single query. The endpoints of the services to be queried are provided as parameters to the 'SERVICE' operator.

However, for data that is published/available in a RDF format but not necessarily set up through an endpoint, we make use of SPARQL-LD 
to directly access and exploit the RDF graphs. SPARQL-LD has an extended 'SERVICE' definition that tries to fetch and query the RDF triples
that may exist in the given resource at execution time. However, SPARQL-LD does not cater to resources that are do not have a RDF 
representation. Hence we make an extension to SPARQL-LD, enabling the use of non-RDF web documents published by constrained devices. 
Our implementation uses the **Content-Lifting-Rule** HTTP response header field as defined in (http://www.maxime-lefrancois.info/docs/Lefrancois-EGC2017-Interoperabilite.pdf). 
The value of this parameter is an absolute URI that identifies some RDF lifting mechanism. 

Our extension of SPARQL-LD implements the support of lifting rules expressed as SPARQL-Generate queries. This allow us to execute portions 
of a query to the RDF generated from lightweight documents in arbitrary formats exposed by constrained WoT devices.

## Example Query
Consider a WoT-enabled smart office with a room *<room/2>* on floor 2.

Room 2 contains two sensors, an *Occupancy Sensor* and a *Temperature Sensor*. 

We assume that some description of the building and the devices is available in a Data Catalogue. 
Below is a condensed version of the Data Catalogue:*Data_Catalogue.ttl* used for the example query.

```
<room/2> a seas:Room , sosa:FeatureOfInterest ;
  ssn:hasProperty seas:TemperatureProperty , seas:OccupancyProperty ;
  seas:onFloor 2 .

<room/2#temperature> a sosa:ObservableProperty , seas:TemperatureProperty ;
sosa:observedProperty <temperature2> .

<temperature2> a sosa:Observation ;
  sosa:hasFeatureOfInterest <room/2> ;
  sosa:observedProperty seas:temperature ;
  sosa:hasSimpleResult <temp_value> ;
  ex:endpoint <http://localhost:8001/endpoint_TempSensor1.json> ;
  ex:liftingrule <http://localhost:8001/liftingrule_TempSensor.rqg> .

 <room/2#occupancy> a sosa:ObservableProperty , seas:OccupancyProperty ;
 sosa:observedProperty <occupancy2> .

 <occupancy2> a sosa:Observation ;
     sosa:hasFeatureOfInterest <room/2> ;
     sosa:observedProperty ns:Occupancy ;
     sosa:hasSimpleResult <occ_value> ;
     ex:endpoint <http://localhost:8001/endpoint_OccSensor1.json> ;
     ex:liftingrule <http://localhost:8001/liftingrule_OccSensor.rqg> .
```

We want to allow an end user to query the devices by launching a SPARQL query without having to worry about the various data formats 
used by the devices/sensors or even the distributed nature of the data sources.

This query should answer the question: *What are the room that have the property 'temperature' and 'occupancy' and what values do the 
sensors that have these property depict?*

```
SELECT ?room WHERE {
  ?room a seas:Room , sosa:FeatureOfInterest ;
    ssn:hasProperty ?temperature, ?occupancy ;
    seas:onFloor ?floor .
    
  ?temperature a sosa:ObservableProperty, seas:TemperatureProperty .
  ?occupancy a sosa:ObservableProperty, seas:OccupancyProperty .
  
  ?temp_obs a sosa:Observation ;
    sosa:hasFeatureOfInterest ?room ;
    sosa:observedProperty ?temperature ;
    sosa:hasSimpleResult ?temp_value .
    
  ?occ_obs a sosa:Observation ;
    sosa:hasFeatureOfInterest ?room ;
    sosa:observedProperty ?temperature ;
    sosa:hasSimpleResult ?occ_value .
}
```

Our major focus for this project is on proposing the steps to be undertaken, such that it allows us to query the light-weight format used 
by the sensors and the devices to expose their data.

## Source code
We have used the original SPARQL-LD project present here: (https://github.com/fafalios/sparql-ld) as mentioned. <br />
We have however, modified the arq.**SPARQL_LD_EXTENDEDExample** file to include the example mentioned above. <br />
For extending SPARQL-LD, we have created the class: com.hp.hpl.jena.sparql.engine.http.**SparqlLDExtended** <br />

We have also upgraded SPARQL-LD to use **Jena ARQ version 3.3.0** from the originally used *Jena ARQ version 2.13.0*.

The example provided for SPARQL-LD extended outputs the result for the query mentioned above as a federated result stored in a string. 
The project in it's current version, does not always optimise query federation (however, this is included in future works for the project).

## Installation
* Download the source code project provided. 
* When building the sources, avoid running the testcases provided for Jena if error occurs. 
* Try to run the main class "arq.SPARQL_LD_EXTENDEDExample".







