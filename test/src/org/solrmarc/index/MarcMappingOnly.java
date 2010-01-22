package org.solrmarc.index;

import java.util.*;

//import org.apache.log4j.Logger;

import org.marc4j.MarcException;
import org.marc4j.marc.Record;

import org.solrmarc.marc.MarcHandler;

/**
 * Reads in marc records and creates mapping of solr field names to solr field
 * values per configuration files. Only creates the mapping; does not write out
 * to file or to index.
 * 
 * based on org.solrmarc.marc.MarcPrinter by Bob Haschart
 * 
 * @author Naomi Dushay
 * @version $Id$
 */
public class MarcMappingOnly extends MarcHandler
{
    // static Logger logger = Logger.getLogger(MarcMappingTest.class.getName());

    /** name of unique key field in solr document */
    private String idFldName = null;

    /**
     * Constructor
     * @param args - array of Strings:
     *    arg[0] - name of xxx_config.properties file
     *    arg[1] - name of unique key field in solr document
     */
    public MarcMappingOnly()
    {
        super();
    }
    
    /** 
     * processAdditionalArgs - local init for subclasses of MarcHandler
     */
    protected void processAdditionalArgs()
    {
        idFldName = addnlArgs[0];
    }  

    /**
     * read in the file of marc records indicated, looking for the desired
     * record, and returning the mapping of solr field names to values.
     * 
     * @param desiredRecId -
     *            value for solr id field, or pass in a value of null to simply accept 
     *            the first record that occurs in the specified marc file
     * @param mrcFileName -
     *            absolute path of file of marc records (name must end in .mrc
     *            or .marc or .xml)
     * @return a mapping of solr field names to solr field values (as Objects
     *         that are Strings or Collections of Strings)
     */
    public Map<String, Object> getIndexMapForRecord(String desiredRecId, String mrcFileName)
    {
        loadReader("FILE", mrcFileName);
        while (reader != null && reader.hasNext())
        {
            try
            {
                Record record = reader.next();

                Map<String, Object> solrFldName2ValMap = indexer.map(record, errors);
                if (errors != null && includeErrors && errors.hasErrors())
                    solrFldName2ValMap.put("marc_error", errors.getErrors());
                // FIXME:
                if (desiredRecId == null || idFldName == null) return(solrFldName2ValMap);
                
                Object thisRecId = solrFldName2ValMap.get(idFldName);
                if (thisRecId.equals(desiredRecId))
                    return solrFldName2ValMap;
            }
            catch (MarcException me)
            {
                System.err.println("Error reading Marc Record: " + me.getMessage());
            }
        }
        return null;
    }

    @Override
    /**
     * this method is required, though we don't use it here.
     */
    public int handleAll()
    {
        return 0;
    }

}
