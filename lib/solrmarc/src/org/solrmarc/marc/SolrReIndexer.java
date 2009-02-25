package org.solrmarc.marc;

import java.io.*;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.*;

//import javax.xml.xpath.*;

//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.index.Term;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.TermQuery;
//import org.apache.solr.core.SolrConfig;
//import org.apache.solr.core.SolrCore;
//import org.apache.solr.search.*;
//import org.apache.solr.update.*;
//import org.apache.solr.util.RefCounted;
import org.marc4j.*;
import org.marc4j.marc.Record;
import org.solrmarc.index.SolrIndexer;

import org.solrmarc.solr.SolrCoreLoader;
import org.solrmarc.solr.SolrCoreProxy;
import org.solrmarc.solr.SolrSearcherProxy;
import org.solrmarc.tools.Utils;

import org.apache.log4j.Logger;


/**
 * Reindex marc records stored in an index
 * @author Robert Haschart
 * @version $Id$
 *
 */
public class SolrReIndexer extends MarcImporter
{
    protected SolrSearcherProxy solrSearcherProxy;
    private String queryForRecordsToUpdate;
    protected String solrFieldContainingEncodedMarcRecord;
    protected boolean doUpdate = true;
    
    // Initialize logging category
    static Logger logger = Logger.getLogger(SolrReIndexer.class.getName());
    
    /**
     * Constructor
     * @param properties path to properties files
     * @param args additional arguments
     * @throws IOException
     */
    public SolrReIndexer(String args[])
    {
        super(args);
        loadLocalProperties(configProps);
        processAdditionalArgs(addnlArgs);
        solrSearcherProxy = new SolrSearcherProxy(solrCoreProxy);
    }

    @Override
    public int handleAll()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    private void loadLocalProperties(Properties props)
    {
        solrFieldContainingEncodedMarcRecord = Utils.getProperty(props, "solr.fieldname");
        queryForRecordsToUpdate = Utils.getProperty(props, "solr.query");
        String up = Utils.getProperty(props, "solr.do_update");
        doUpdate = (up == null) ? true : Boolean.parseBoolean(up);
    }
    
    private void processAdditionalArgs(String[] args) 
    {
        if (queryForRecordsToUpdate == null && args.length > 0)
        {
            queryForRecordsToUpdate = args[0];
        }
        if (solrFieldContainingEncodedMarcRecord == null && args.length > 1)
        {
            solrFieldContainingEncodedMarcRecord = args[1];
        }
    }

    
//    /**
//     * Read matching records from the index
//     * @param queryForRecordsToUpdate
//     */
//    public void readAllMatchingDocs(String queryForRecordsToUpdate)
//    {
//        String queryparts[] = queryForRecordsToUpdate.split(":");
//        if (queryparts.length != 2) 
//        {
//            //System.err.println("Error query must be of the form    field:term");
//        	logger.warn("Error query must be of the form    field:term");
//            return;
//        }
//        Map<String, Object> docMap = readAndIndexDoc(queryparts[0], queryparts[1], doUpdate);  
//    }
//    
    /**
     * Read and index a Solr document
     * @param field Solr field
     * @param term Term string to index
     * @param update flag to update the record 
     * @return Map of the fields
     */
    public Map<String, Object> readAndIndexDoc(String field, String term, boolean update)
    {
        try 
        {
            Object docSetIterator = solrSearcherProxy.getDocSetIterator(field, term);
            int count = 0;
            while (solrSearcherProxy.iteratorHasNext(docSetIterator))
            {
                Object doc = solrSearcherProxy.iteratorGetNext(docSetIterator);
    //            count ++;
    //            if (count == 100 || count == 1000 || count == 10000 || count % 10000 == 0)
    //            {
    //                System. out.println("Done handling "+ count +" record out of "+ totalSize);
    //            }
                    
                Record record = getRecordFromDocument(doc);
                    
                if (record != null)
                {
                    Map<String, Object> docMap = indexer.map(record);
                    addExtraInfoFromDocToMap(doc, docMap);
                    if (update && docMap != null && docMap.size() != 0)
                    {
                        update(docMap);
                    }
                    else
                    {
                        return(docMap);
                    }
                }
            }
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
            //e.printStackTrace();
        }
        return(null);
    }
    
    /**
     * Add information from a document to a map.
     * @param doc
     * @param map
     */
    protected void addExtraInfoFromDocToMap(Object doc, Map<String, Object> docMap)
    {
        addExtraInfoFromDocToMap(doc, docMap, "fund_code_facet");
        addExtraInfoFromDocToMap(doc, docMap, "date_received_facet");   
    }

    /**
     * Add extra information from a Solr Document to a map
     * @param doc Solr Document to pull information from
     * @param map Map to add information to
     * @param keyVal Value to add
     */
    protected void addExtraInfoFromDocToMap(Object doc, Map<String, Object> map, String keyVal)
    {
        String fieldVals[] = null;
        try
        {
            fieldVals = (String[])doc.getClass().getMethod("getValues", String.class).invoke(doc, keyVal);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (fieldVals != null && fieldVals.length > 0)
        {
            for (int i = 0; i < fieldVals.length; i++)
            {
                String fieldVal = fieldVals[i];
                addToMap(map, keyVal, fieldVal);
            }
        }           
    }

//    /**
//     * Return a Solr document from the index
//     * @param s SolrIndexSearcher to search
//     * @param SolrDocumentNum Number of documents to return
//     * @return SolrDocument 
//     * @throws IOException
//     */
//    public Document getDocument(SolrIndexSearcher s, int SolrDocumentNum) throws IOException
//    {
//        Document doc = s.doc(SolrDocumentNum);
//        return(doc);
//    }
    
    /**
     * Retrieve the marc information from the Solr document
     * @param doc SolrDocument from the index
     * @return marc4j Record
     * @throws IOException
     */
    public Record getRecordFromDocument(Object doc) throws IOException
    {
        Object field = null;
        try
        {
            field = doc.getClass().getMethod("getField", String.class).invoke(doc, solrFieldContainingEncodedMarcRecord);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (field == null)
        {
            //System.err.println("field: "+ solrFieldContainingEncodedMarcRecord + " not found in solr document");
        	logger.warn("field: "+ solrFieldContainingEncodedMarcRecord + " not found in solr document");
        }
        String marcRecordStr = null;
        try
        {
            if (field != null) marcRecordStr = (String)field.getClass().getMethod("stringValue").invoke(field);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (marcRecordStr.startsWith("<?xml version"))
        {
            return (getRecordFromXMLString(marcRecordStr));            
        }
        else
        {
            return (getRecordFromRawMarc(marcRecordStr));
        }
    }
        
    /**
     * Extract the marc record from binary marc
     * @param marcRecordStr
     * @return
     */
    private Record getRecordFromRawMarc(String marcRecordStr)
    {
        MarcStreamReader reader;
        boolean tryAgain = false;
        do {
            try {
                tryAgain = false;
                reader = new MarcStreamReader(new ByteArrayInputStream(marcRecordStr.getBytes("UTF8")));
                if (reader.hasNext())
                {
                    Record record = reader.next(); 
                    if (verbose)
                    {
                        System.out.println(record.toString());
                    }
                    return(record);
                }
            }
            catch( MarcException me)
            {
                me.printStackTrace();
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        } while (tryAgain);
        return(null);
    }
    
    // error output
    static BufferedWriter errOut = null;
    
    /**
     * Extract marc record from MarcXML
     * @param marcRecordStr MarcXML string
     * @return marc4j Record
     */
    public Record getRecordFromXMLString(String marcRecordStr)
    {
        MarcXmlReader reader;
        boolean tryAgain = false;
        do {
            try {
                tryAgain = false;
                reader = new MarcXmlReader(new ByteArrayInputStream(marcRecordStr.getBytes("UTF8")));
                if (reader.hasNext())
                {
                    Record record = reader.next(); 
                    if (verbose)
                    {
                        System.out.println(record.toString());
                        System.out.flush();
                    }
                    return(record);
                }
            }
            catch( MarcException me)
            {
                if (doUpdate == false && errOut == null)
                {
                    try
                    {
                        errOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("badRecs.xml"))));
                        errOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><collection xmlns=\"http://www.loc.gov/MARC21/slim\">");
                    }
                    catch (FileNotFoundException e)
                    {
                        // e.printStackTrace();
                    	logger.error(e.getMessage());
                    }
                    catch (IOException e)
                    {
                        // e.printStackTrace();
                    	logger.error(e.getMessage());
                    }
                }
                if (doUpdate == false && errOut != null)
                {
                    String trimmed = marcRecordStr.substring(marcRecordStr.indexOf("<record>"));
                    trimmed = trimmed.replaceFirst("</collection>", "");
                    trimmed = trimmed.replaceAll("><", ">\n<");
                    try
                    {
                        errOut.write(trimmed);
                    }
                    catch (IOException e)
                    {
                        // e.printStackTrace();
                    	logger.error(e.getMessage());
                    }
                }
                if (marcRecordStr.contains("<subfield code=\"&#31;\">"))
                {
                    // rewrite input string and try again.
                    marcRecordStr = marcRecordStr.replaceAll("<subfield code=\"&#31;\">(.)", "<subfield code=\"$1\">");
                    tryAgain = true;
                }
                else if (extractLeader(marcRecordStr).contains("&#")) //.("<leader>[^<>&]*&#[0-9]+;[^<>&]*</leader>"))
                {
                    // rewrite input string and try again.
                    // 07585nam a2200301 a 4500
                    String leader = extractLeader(marcRecordStr).replaceAll("&#[0-9]+;", "0");
                    marcRecordStr = marcRecordStr.replaceAll("<leader>[^<]*</leader>", leader);
                    tryAgain = true;
                }
                else
                {
                    me.printStackTrace();
                    if (verbose) {
                    	//System.out.println("The bad record is: "+ marcRecordStr);
                    	logger.info("The bad record is: "+ marcRecordStr);
                    	logger.error("The bad record is: "+ marcRecordStr);
                    }
                }
            }
            catch (UnsupportedEncodingException e)
            {
                // e.printStackTrace();
            	logger.error(e.getMessage());
            }
        } while (tryAgain);
        return(null);

    }
        
 
    /**
     * Extract the leader from the marc record string
     * @param marcRecordStr marc record as a String
     * @return Leader leader string for the marc record
     */
    private String extractLeader(String marcRecordStr)
    {
        final String leadertag1 = "<leader>";
        final String leadertag2 = "</leader>";
        String leader = null;
        try {
            leader = marcRecordStr.substring(marcRecordStr.indexOf(leadertag1), marcRecordStr.indexOf(leadertag2)+leadertag2.length() );
        }
        catch (IndexOutOfBoundsException e)
        {}
        return leader;
    }

//    private void lookupAndUpdate(String doc_id, String[] fields)
//    {
//        Record record = lookup(doc_id);
//        if (verbose)
//        {
//            System.out.println(record.toString());
//        }
//    }
    
    /**
     * Add a key value pair to a map
     */
    protected void addToMap(Map<String, Object> map, String key, String value)
    {
        if (map.containsKey(key))
        {
            Object prevValue = map.get(key);
            if (prevValue instanceof String)
            {
                if (!prevValue.equals(value))
                {
                    Set<String> result = new LinkedHashSet<String>();
                    result.add((String)prevValue);
                    result.add((String)value);
                    map.put(key, result);
                }
            }
            else if (prevValue instanceof Collection)
            {
                Iterator<String> valIter = ((Collection)prevValue).iterator();
                boolean addit = true;
                while (valIter.hasNext())
                {
                    String collVal = valIter.next();
                    if (collVal.equals(value)) addit = false;
                }
                if (addit) 
                {
                    ((Collection)prevValue).add(value);
                    map.put(key, prevValue);
                }
            }
        }
        else 
        {
            map.put(key, value);
        }
    }

//    /**
//     * find a specific marc record (using its id) in the solr index
//     * @param doc_id ID of the marc record to find
//     * @return if the item is in the index
//     */
//    private Record lookup(String doc_id)
//    {
//        RefCounted<SolrIndexSearcher> rs = solrCore.getSearcher();
//        SolrIndexSearcher s = rs.get();
//        Term t = new Term("id", doc_id);
//        int docNo;
//        Record rec = null;
//        try
//        {
//            docNo = s.getFirstMatch(t);
//            if (docNo > 0)
//            {
//                Document doc = getDocument(s, docNo);
//                rec = getRecordFromDocument(doc);
//            }
//            else
//            {
//            	//TODO: construct this from the properties
//                URL url = new URL("http://solrpowr.lib.virginia.edu:8080/solr/select/?q=id%3A"+doc_id+"&start=0&rows=1");
//                InputStream stream = url.openStream();
//                //The evaluate methods in the XPath and XPathExpression interfaces are used to parse an XML document with XPath expressions. The XPathFactory class is used to create an XPath object. Create an XPathFactory object with the static newInstance method of the XPathFactory class.
//
//                XPathFactory  factory = XPathFactory.newInstance();
//
//                // Create an XPath object from the XPathFactory object with the newXPath method.
//
//                XPath xPath = factory.newXPath();
//
//                // Create and compile an XPath expression with the compile method of the XPath object. As an example, select the title of the article with its date attribute set to January-2004. An attribute in an XPath expression is specified with an @ symbol. For further reference on XPath expressions, see the XPath specification for examples on creating an XPath expression.
//
//                XPathExpression  xPathExpression=
//                    xPath.compile("/response/result/doc/arr[@name='marc_display']/str");
//                
//                InputSource inputSource = new InputSource(stream);
//                String marcRecordStr = xPathExpression.evaluate(inputSource);
//                rec = getRecordFromXMLString(marcRecordStr);
//            }           
//        }
//        catch (IOException e)
//        {
//            // e.printStackTrace();
//        	logger.error(e.getMessage());
//        }
//        catch (XPathExpressionException e)
//        {
//            // e.printStackTrace();
//        	logger.error(e.getMessage());
//        }
//        return(rec);
//    }

    /**
     * Update a document in the Solr index
     * @param map Values of the "new" marc record
     */
    public void update(Map<String, Object> map)
    { 
        try {
            String docStr = solrCoreProxy.addDoc(map, verbose);
            if (verbose)
            {
 //               logger.info(record.toString());
                logger.info(docStr);
            }

        } 
        catch (IOException ioe) 
        {
            //System.err.println("Couldn't add document");
            logger.error("Couldn't add document: " + ioe.getMessage());
            //e.printStackTrace();
 //           logger.error("Control Number " + record.getControlNumber(), ioe);
        }                
    }
    

//    /**
//     * @param args
//     */
//    public static void main(String[] args)
//    {
//        String properties = "import.properties";
//        if(args.length > 0 && args[0].endsWith(".properties"))
//        {
//            properties = args[0];
//            String newArgs[] = new String[args.length-1];
//            System.arraycopy(args, 1, newArgs, 0, args.length-1);
//            args = newArgs;
//        }
//       // System.out.println("Loading properties from " + properties);
//        logger.info("Loading properties from " + properties);
//        
//        SolrReIndexer reader = null;
//        try
//        {
//            reader = new SolrReIndexer(properties, args);
//        }
//        catch (IOException e)
//        {
//            //  e.printStackTrace();
//        	logger.error(e.getMessage());
//            System.exit(1);
//        }
//        
//        reader.readAllMatchingDocs(reader.queryForRecordsToUpdate);
//        
//        reader.finish();
//        if (errOut != null)
//        {
//            try
//            {
//                errOut.write("\n</collection>");
//                errOut.flush();
//
//            }
//            catch (IOException e)
//            {
//                // e.printStackTrace();
//            	logger.error(e.getMessage());
//            }
//        }
//
//    }

}
