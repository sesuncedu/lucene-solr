package org.blacklight;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;
import org.solrmarc.tools.CallNumUtils;
import org.solrmarc.tools.StringNaturalCompare;
import org.solrmarc.tools.Utils;


/**
 * Custom functions for indexing marc files for the Blacklight project
 * @author Robert Haschart
 * @version $Id: BlacklightIndexer.java 219 2008-10-28 19:14:39Z rh9ec@virginia.edu $
 *
 */

public class BlacklightIndexer extends SolrIndexer
{
    /**
     * Main program instantiation for doing the indexing
     * @param args
     */

    /**
     * Default constructor
     * @param propertiesMapFile
     * @throws ParseException 
     * @throws IOException 
     * @throws FileNotFoundException 
     * @throws Exception
     */
    Map<String, String> addnlShadowedIds = null;
    String extraIdsFilename = "AllShadowedIds.txt";
    Set<String> callNumberFieldList = null;
    Set<String> callNumberFieldListNo050 = null;
    Map<String, Set<String>> callNumberClusterMap = null;
    Map<String, Set<String>> callNumberClusterMapNo050 = null;
    Comparator<String> normedComparator = null;
    String bestSingleCallNumber = null;
    
    public BlacklightIndexer(final String propertiesMapFile, final String propertyPaths[])
    {
        super(propertiesMapFile, propertyPaths);
        normedComparator = new Comparator<String>() 
        {
            public int compare(String s1, String s2)
            {
                String s1Norm = s1.replaceAll("[. ]", "");
                String s2Norm = s2.replaceAll("[. ]", "");
                return s1Norm.compareToIgnoreCase(s2Norm);
            }
        };
    }
    
    /**
     * Add a record to the Solr index
     * @param record
     */
//    public void indexRecord(DocumentBuilder builder, Record record)
//    {        
//        addField(builder, "id", getFirstFieldVal(record, "001"));
//        addFields(builder, "author_text", record, "100a:110a:111a:130a");
//        addFields(builder, "published_text", getFieldList(record, "260a"));
//        addFields(builder, "material_type_text", getFieldList(record, "300a"));
//        addFields(builder, "notes_text", getFieldList(record, "500a:505a"));
//        addFields(builder, "uniform_title_text", getFieldList(record, "240a"));
//        addField(builder, "marc_display", writeRaw(record));
//        addField(builder, "marc_text", writeXml(record));
//        addField(builder, "title_text", getFieldVals(record, "245a:245b", " "));
//        addField(builder, "call_number_facet", getFirstFieldVal(record, "999a:090a:050a"));
//        addField(builder, "year_multisort_i", getDate(record));
//                
//        addField(builder, "source_facet", "Library Catalog");
//        addFields(builder, "subject_era_facet", getFieldList(record, "650d:650y:651y:655y"));
//        addFields(builder, "topic_form_genre_facet", getFieldList(record, "650a:650b:650x:655a"));
//        addFields(builder, "subject_geographic_facet", getFieldList(record, "650c:650z:651a:651x:651z:655z"));
//        addField(builder, "broad_formats_facet", getBroadFormat(record));
//        addField(builder, "format_facet", getFormat(record));
//        addFields(builder, "language_facet", getLanguage(record));
//        addFields(builder, "location_facet", getLocation(record));
//        addFields(builder, "library_facet", getLibrary(record));
//        addFields(builder, "instrument_facet", getInstrument(record));
//        addFields(builder, "recording_type_facet", getRecordingType(record));
//        addFields(builder, "recordings_and_scores_facet", getRecordingAndScore(record));
//        addFields(builder, "recording_format_facet", getRecordingFormat(record));
//        addFields(builder, "music_catagory_facet", getMusicCatagory(record));
//            
//    }


    /**
     * Return a collection of recordings and scores from a record
     * @param record
     * @return A collection of recordings and/or scores
     */
    public Set<String> getRecordingAndScore(final Record record)
    {
        Set<String> result = new LinkedHashSet<String>();
        String leader = record.getLeader().toString();
        String leaderChar = leader.substring(6, 7).toUpperCase();
                
        if("C".equals(leaderChar) || "D".equals(leaderChar))
        {
            result.add("Scores");
            result.add("Recordings and/or Scores");
        }
        
        if("J".equals(leaderChar))
        {
            result.add("Recordings");
            result.add("Recordings and/or Scores");
        }
        
        return result;
    }
 
    /**
     * Return a collection of recording formats from a record
     * @param record
     * @return Collection of recording formats
     */
    public Set<String> getRecordingFormat(final Record record)
    {
        Set<String> result = new LinkedHashSet<String>();
        String leader = record.getLeader().toString();
        String leaderChar = leader.substring(6, 7).toUpperCase();
        Set<String> titleH = getSubfieldDataAsSet(record, "245", "h", null);
                
        if("J".equals(leaderChar) || "I".equals(leaderChar) || 
                (Utils.setItemContains(titleH, "videorecording")))
        {
            Set<String> form = getSubfieldDataAsSet(record, "999", "t", null);
            return(form);
        }
        return(result);
    }

    /**
     * This routine can be overridden in a sub-class to perform some processing that need to be done once 
     * for each record, and which may be needed by several indexing specifications, especially custom methods.
     * The default version does nothing.
     * 
     * @param record -  The MARC record that is being indexed.
     */
    protected void perRecordInit(Record record)
    {
        String fieldSpec = "999awi';'";
        
        callNumberFieldListNo050 = getCallNumberFieldSetNo050(record, fieldSpec);
        callNumberFieldList = getCallNumberFieldSet(record, callNumberFieldListNo050);
        callNumberClusterMapNo050 =  getCallNumbersCleanedConflated(callNumberFieldListNo050, true);
        callNumberClusterMap =  getCallNumbersCleanedConflated(callNumberFieldList, true);
        bestSingleCallNumber = getBestSingleCallNumber(callNumberClusterMap);
    }
    
    private String getBestSingleCallNumber(Map<String, Set<String>>resultNormed)
    {
        if (resultNormed == null || resultNormed.size() == 0) {
            return(null);
        }
        String[] bestSet =  getBestCallNumberSubset(resultNormed);
        if (bestSet.length == 0) return(null);
        String result = bestSet[0];
        String resultParts[] = result.split(":", 2);
        if (resultParts[0].equals("LC"))
        {
            result = resultParts[0]+":"+resultParts[1].trim().replaceAll("[^A-Za-z0-9.]", " ").replaceAll("\\s\\s+", " ")
                        .replaceAll("\\s?\\.\\s?", ".");
        }        
        return(result);
    }
    
    private String[] getBestCallNumberSubset(Map<String, Set<String>>resultNormed)
    {
        if (resultNormed == null || resultNormed.size() == 0) {
            return(null);
        }
        int maxEntries = 0;
        //String maxEntriesKey = null;
        Set<String> maxEntrySet = null;
        int maxLCEntries = 0;
        //String maxLCEntriesKey = null;
        Set<String> maxLCEntrySet = null;
        Set<String> keys = resultNormed.keySet();
        for (String key : keys)
        {
            Set<String> values = resultNormed.get(key);
            if (values.size() > maxEntries)
            {
                maxEntries = values.size();
                //maxEntriesKey = key;
                maxEntrySet = values;
            }
            String firstNum = values.iterator().next();
            String parts[] = firstNum.split(":", 2);
            if (parts[0].equals("LC") || 
                ( parts[0].equals("") && CallNumUtils.isValidLC(parts[1])) && values.size() > maxLCEntries)
            {
                maxLCEntries = values.size();
                //maxLCEntriesKey = key;
                maxLCEntrySet = values;
            }
        }
        if (maxLCEntrySet == null)
        {
            maxLCEntrySet = maxEntrySet;
        }
        String valueArr[] = maxLCEntrySet.toArray(new String[0]);
        Comparator<String> comp = new StringNaturalCompare();
        Arrays.sort(valueArr, comp);
        return(valueArr);
    }
    
    /**
     * Since there are several routines that grab and process LC Call Numbers for a given record, 
     * this code is called once per record to gather the list of call numbers, rather than creating that 
     * list within each implementation of the custom indexing functions.
     * 
     * @param record -  The MARC record that is being indexed.
     */
    private Set<String> getCallNumberFieldSetNo050(final Record record, String fieldSpec)
    {
        boolean processExtraShadowedIds = fieldSpec.contains("';'");
    
        List<?> fields999 = record.getVariableFields("999");
        //Set<String> fieldList = getFieldList(record, fieldSpec);
        if (fields999.isEmpty())  {
            return(null);
        }
        Set<String> fieldList = new LinkedHashSet<String>();
        if (processExtraShadowedIds)
        {
            loadExtraShadowedIds(extraIdsFilename);
            String extraString = addnlShadowedIds.get(record.getControlNumber());
          
            for (Object field : fields999)
            {
                DataField df = ((DataField)(field));
                String barCode = (df.getSubfield('i') != null) ? df.getSubfield('i').getData() : "";
                String numberScheme = (df.getSubfield('w') != null) ? df.getSubfield('w').getData() : "";
                if (numberScheme.equals("MONO-SER") || numberScheme.equals("LCPER"))  numberScheme = "LC";
                String callNumber = (df.getSubfield('a') != null) ? df.getSubfield('a').getData() : "";
                if (extraString == null || extraString.equals("") || !extraString.contains("|" + barCode + "|"))
                {
                    if (numberScheme.length() > 0 && callNumber.length() > 0) 
                        fieldList.add(numberScheme + ":" + callNumber);
                }
            }
        }
        return(fieldList);
    }
    /**
     * Since there are several routines that grab and process LC Call Numbers for a given record, 
     * this code is called once per record to gather the list of call numbers, rather than creating that 
     * list within each implementation of the custom indexing functions.
     * 
     * @param record -  The MARC record that is being indexed.
     */
    private Set<String> getCallNumberFieldSet(final Record record, Set<String> startingFieldList)
    {
        Set<String> fieldList = new LinkedHashSet<String>();
        if (startingFieldList != null) 
        {
            fieldList.addAll(startingFieldList);
        }

        // discard LC numbers that aren't valid according to the CallNumUtil routine
        boolean hasLCNumber = false;
        for (String field : fieldList)
        {
            String fieldParts[] = field.split(":", 2);
            if (fieldParts[0].equals("LC") || 
                (fieldParts[0].equals("") && CallNumUtils.isValidLC(field)))
            {
                hasLCNumber = true;
                break;
            }
        }
        // if there are no 999 fields with valid LC Call Numbers then look in the 050ab field
        if (!hasLCNumber)
        {
            Set<String> fList2 = getFieldList(record, "050ab");
            for (String field : fList2)
            {
                if (CallNumUtils.isValidLC(field))
                {
                    fieldList.add("LC:"+field);
                }
            }
        }
        return(fieldList);
    }

    /**
     * Extract a set of cleaned call numbers from a record
     * @param record
     * @return Clean call number
     */
    private Map<String, Set<String>> getCallNumbersCleanedConflated(Set<String> fieldList, boolean expectColon)
    {
        Map<String, Set<String>> resultNormed = new TreeMap<String, Set<String>>();
        if (fieldList == null || fieldList.size() == 0)  return(null);
        for (String callNumPlus : fieldList)
        {
            String parts[] = callNumPlus.split(":", 2);
            String prefix = null;
            String callNumPart = null;
            if (!expectColon || parts.length == 1)
            {
                prefix = "";
                callNumPart = parts[0];
            }
            else
            {
                prefix = parts[0]+":";
                callNumPart = parts[1];
            }
            String val = callNumPart.trim().replaceAll("\\s\\s+", " ").replaceAll("\\s?\\.\\s?", ".");
            String nVal = val.replaceAll("^([A-Z][A-Z]?[A-Z]?) ([0-9])", "$1$2");
            if (!nVal.equals(val))
            {
                val = nVal;
            }
            String key = val.substring(0, Math.min(val.length(), 5)).toUpperCase();
            val = prefix+val;
            if (resultNormed.containsKey(key))
            {
                Set<String> set = resultNormed.get(key);
                set.add(val);
                resultNormed.put(key, set);
            }
            else
            {
                Set<String> set = new TreeSet<String>(normedComparator);
                set.add(val);
                resultNormed.put(key, set);
            }
        }
        return(resultNormed);
    }
 
   /**
     * Extract call number prefix from a record
     * @param record
     * @return Call number prefix
     * @deprecated
     */
    public String getCallNumberPrefix(final Record record, String mapName, String part)
    {
        try
        {
            mapName = loadTranslationMap(null, mapName);
        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String useRecord = getFirstFieldVal(record, "050a:090a");
        if (useRecord == null || useRecord.matches("[0-9].*"))  
        {
            return(null);
        }
        
        String val = getFirstFieldVal(record, "999a");
        String result = null;
        if (val == null || val.length() == 0) { 
            return(null);
            }
        
        String vals[] = val.split("[^A-Za-z]+", 2);
        String prefix = vals[0];
        
        if (vals.length == 0 || vals[0] == null || vals[0].length() == 0 ||  vals[0].length() > 3 || !vals[0].toUpperCase().equals(vals[0])) 
        {
            return(null);
        }
        else
        {
            while (result == null && prefix.length() > 0)
            {
                result = Utils.remap(prefix, findMap(mapName), false);
                if (result == null)
                {
                    prefix = prefix.substring(0, prefix.length()-1);
                }
            }
        }
        int partNum = Utils.isNumber(part) ? Integer.parseInt(part) : 0;
        if (result == null) return(result);
        if (partNum == 0) return(prefix + " - " + result.replaceAll("[|]", " - "));
        String resultParts[] = result.split("[|]");
        if (partNum-1 >= resultParts.length) return(null);
        return(prefix.substring(0,1) + " - " + resultParts[partNum-1]);
    }
 
 
    /**
     * Extract call number prefix from a record
     * @param record
     * @return Call number prefix
     */
    public String getCallNumberPrefixNew(final Record record, String mapName, String part)
    {
        try
        {
            mapName = loadTranslationMap(null, mapName);
        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        String useRecord = getFirstFieldVal(record, "050a:090a");
//        if (useRecord == null || useRecord.matches("[0-9].*"))  
//        {
//            return(null);
//        }
        
        String val = bestSingleCallNumber;
        String result = null;
        if (val == null || val.length() == 0)
        { 
            return(null);
        }
        String valParts[] = val.split(":", 2);
        if (!valParts[0].equals("LC"))
        {
            return(null);
        }
        String vals[] = valParts[1].split("[^A-Za-z]+", 2);
        String prefix = vals[0];
        
        if (vals.length == 0 || vals[0] == null || vals[0].length() == 0 ||  vals[0].length() > 3 || !vals[0].toUpperCase().equals(vals[0])) 
        {
            return(null);
        }
        else
        {
            while (result == null && prefix.length() > 0)
            {
                result = Utils.remap(prefix, findMap(mapName), false);
                if (result == null && prefix.length() == 2)
                {
                    break;
                }
                if (result == null)
                {
                    prefix = prefix.substring(0, prefix.length()-1);
                }
            }
        }
        if (mapName.equals("callnumber_map"))
        {
            int partNum = Utils.isNumber(part) ? Integer.parseInt(part) : 0;
            if (result == null) return(result);
            if (partNum == 0) return(prefix + " - " + result.replaceAll("[|]", " - "));
            String resultParts[] = result.split("[|]");
            if (partNum-1 >= resultParts.length) return(null);
            return(prefix.substring(0,1) + " - " + resultParts[partNum-1]);
        }
        else // deatiled call number map
        {
            if (result == null) return(result);
            if (result.startsWith("{"))
            {
                String shelfKey = CallNumUtils.getLCShelfkey(valParts[1], record.getControlNumberField().getData());
                String keyDigits = shelfKey.substring(4, 8);
                String ranges[] = result.replaceAll("[{]", "").split("[}]");
                for (String range : ranges)
                {
                    String rangeParts[] = range.split("[-=]", 3);
                    if (keyDigits.compareTo(rangeParts[0])>= 0 && keyDigits.compareTo(rangeParts[1])<= 0 )
                    {
                        return(prefix + rangeParts[0].replaceFirst("^0+", "") + "-" + 
                                prefix + rangeParts[1].replaceFirst("^0+", "") + " - " + rangeParts[2]);
                    }
                }
                return(null);
            }
            else if (result.startsWith(prefix.substring(0,1)) && result.matches("["+prefix.substring(0,1)+"][A-Z]-["+prefix.substring(0,1)+"][A-Z] - .*"))
            {
                return(result);                
            }
            else 
            {
                return(prefix + " - " + result);
            }
               
        }
    }

    /**
     * Get the specified subfields from the specified MARC field, returned as
     *  a set of strings to become lucene document field values
     * @param record
     * @param fldTag - the field name, e.g. 245
     * @param subfldsStr - the string containing the desired subfields
     * @returns the result set of strings 
     */
   /* @SuppressWarnings("unchecked")
    protected static Set<String> getSubfieldDataAsSet(Record record, String fldTag, String subfldsStr, String separator)
    {
        Set<String> resultSet = new LinkedHashSet<String>();

        // Process Leader
        if (fldTag.equals("000"))
        {
        	resultSet.add(record.getLeader().toString());
            return resultSet;
        }
        
        // Loop through Data and Control Fields
        int iTag = new Integer(fldTag).intValue();
        List<VariableField> varFlds = record.getVariableFields(fldTag);
        for (VariableField vf : varFlds)
        {
            if (iTag > 9 && subfldsStr != null) 
            {
                // DataField
                DataField dfield = (DataField) vf;

                if (subfldsStr.length() > 1 || separator != null) 
                {
                    // Allow automatic concatenation of grouped subfields
                    StringBuffer buffer = new StringBuffer("");
                    List<Subfield> subFlds = dfield.getSubfields();
                    for (Subfield sf : subFlds) 
                    {
                        String subfldsStrLC = subfldsStr.toLowerCase();
                        int index = subfldsStrLC.indexOf(sf.getCode());
                        if (index != -1)
                        {
	                        if (buffer.length() > 0)  
	                        {
	                            if (Character.isUpperCase(subfldsStr.charAt(index)))
	                            {
	                                resultSet.add(buffer.toString());
	                                buffer = new StringBuffer("");
	                            }
	                            else 
	                            {
	                                buffer.append(separator != null ? separator : " ");
	                            }
	                        }
                            buffer.append(sf.getData().trim());
                        }
                    }                        
	                if (buffer.length() > 0) 
	                	resultSet.add(buffer.toString());
                } 
                else 
                {
	                // get all instances of the single subfield
	                List<Subfield> subFlds = dfield.getSubfields(subfldsStr.charAt(0));
	                for (Subfield sf : subFlds)                         
	                {
	                    resultSet.add(sf.getData().trim());
	                }
                }
            }
            else 
            {
                // Control Field
                resultSet.add(((ControlField) vf).getData().trim());
            }
        }
        return resultSet;
    } */
    
    /* 
     * Extract a single cleaned call number from a record
    * @param record
    * @return Clean call number
    */
   public String getCallNumberCleanedNew(final Record record, String sortable)
   {
       boolean sortableFlag = (sortable != null && ( sortable.equals("sortable") || sortable.equals("true")));
       String result = bestSingleCallNumber;
       if (result == null) return(result);
       String resultParts[] = result.split(":", 2);
       if (sortableFlag && ( resultParts[0].equals("LC") || (resultParts[0].equals("") && CallNumUtils.isValidLC(resultParts[1]))))
           result = CallNumUtils.getLCShelfkey(resultParts[1], record.getControlNumberField().getData());
       return(result);

   }
 
   public Set<String> getCallNumbersCleanedNewNo050(final Record record, String conflatePrefixes)
   {
       return(getCallNumbersCleanedNew(record, conflatePrefixes, this.callNumberFieldListNo050, this.callNumberClusterMapNo050));
   }

   
   public Set<String> getCallNumbersCleanedNew(final Record record, String conflatePrefixes)
   {
       return(getCallNumbersCleanedNew(record, conflatePrefixes, this.callNumberFieldList, this.callNumberClusterMap));
   }

   /**
    * Extract a set of cleaned call numbers from a record
    * @param record
    * @return Clean call number
    */
    public Set<String> getCallNumbersCleanedNew(final Record record, String conflatePrefixes, 
                                                Set<String> localCallNumberFieldList, 
                                                Map<String, Set<String>> localCallNumberClusterMap)
    {
        boolean conflate = !conflatePrefixes.equalsIgnoreCase("false");
        
        if (!conflate)
        {
            Set<String> fieldList = localCallNumberFieldList;
            if (fieldList == null || fieldList.isEmpty())  
            {
                return(null);
            }

            Comparator<String> comp = new StringNaturalCompare();
            Set<String> resultNormed = new TreeSet<String>(comp);
            for (String field : fieldList)
            {
                String fieldParts[] = field.split(":", 2);
                String callNum = fieldParts[1];
                String val = callNum.trim().replaceAll("\\s\\s+", " ").replaceAll("\\s?\\.\\s?", ".");
                String nVal = val.replaceAll("^([A-Z][A-Z]?[A-Z]?) ([0-9])", "$1$2");
                if (!nVal.equals(val))
                {
                    val = nVal;
                }
                resultNormed.add(val);
            }
            return resultNormed;
        }
        else
        {
            Map<String, Set<String>> resultNormed = localCallNumberClusterMap;
            if (resultNormed == null || resultNormed.size() == 0) return(null);
            Set<String> keys = resultNormed.keySet();
            Set<String> results = new TreeSet<String>(normedComparator);
            for (String key : keys)
            {
                Set<String> values = resultNormed.get(key);
                String valueArr[] = values.toArray(new String[0]);
                for (int i = 0; i < valueArr.length; i++)
                {
                    valueArr[i] = getCallNum(valueArr[i]);
                }
                if (valueArr.length == 1)
                {
                    results.add(valueArr[0]);
                }
                else
                {
                    String prefix = valueArr[0];
                    for (int i = 1; i < valueArr.length; i++)
                    {
                        prefix = getCommonPrefix(prefix, valueArr[i], normedComparator);
                    }
                    if (prefix.lastIndexOf(' ') != -1)
                    {
                        prefix = prefix.substring(0, prefix.lastIndexOf(' '));
                    }
                    StringBuffer sb = new StringBuffer(prefix);
                    String sep = " ";
                    for (int i = 0; i < valueArr.length; i++)
                    {
                        valueArr[i] = valueArr[i].substring(prefix.length());
                    }
                    Comparator<String> comp = new StringNaturalCompare();
                    Arrays.sort(valueArr, comp);
                    for (int i = 0; i < valueArr.length; i++)
                    {
                        if (valueArr[i].length() > 0) 
                        {
                            sb.append(sep+valueArr[i]);
                            sep = ",";
                        }
                    }
                    if (sb.length() > 100 || valueArr.length > 10)
                    {
                        results.add(prefix + " (" + valueArr.length + " volumes)");
                    }
                    else
                    {
                        results.add(sb.toString());
                    }
                }
            }
            return (results);
        }
    }
    
    private String getCallNum(final String callNum)
    {
        String callNumParts[] = callNum.split(":", 2);
        return (callNumParts[1]);
    }

/** 
    * Extract a single cleaned call number from a record
    * @param record
    * @return Clean call number
    * @deprecated
    */
   public String getCallNumberCleaned(final Record record, String fieldSpec, String sortable)
   {
       Set<String> fieldList = getFieldList(record, fieldSpec);
       if (fieldList.isEmpty())  {
           return(null);
       }
       Map<String, Set<String>> resultNormed = getCallNumbersCleanedConflated(fieldList, false);
       if (resultNormed == null || resultNormed.size() == 0) {
           return(null);
       }
       boolean sortableFlag = (sortable != null && ( sortable.equals("sortable") || sortable.equals("true")));
       int maxEntries = 0;
       String maxEntriesKey = null;
       Set<String> maxEntrySet = null;
       Set<String> keys = resultNormed.keySet();
       for (String key : keys)
       {
           Set<String> values = resultNormed.get(key);
           if (values.size() > maxEntries)
           {
               maxEntries = values.size();
               maxEntriesKey = key;
               maxEntrySet = values;
           }
       }
       String valueArr[] = maxEntrySet.toArray(new String[0]);
       Comparator<String> comp = new StringNaturalCompare();
       Arrays.sort(valueArr, comp);
       String result = valueArr[0];
       result = result.trim().replaceAll(":", " ").replaceAll("\\s\\s+", " ")
                             .replaceAll("\\s?\\.\\s?", ".").replaceAll("[(][0-9]* volumes[)]", "");
       if (sortableFlag) 
           result = CallNumUtils.getLCShelfkey(result, null);
       return(result);

   }
   
   /**
    * Extract a set of cleaned call numbers from a record
    * @param record
    * @return Clean call number
    * @deprecated
    */
    public Set<String> getCallNumbersCleaned(final Record record, String fieldSpec, String conflatePrefixes)
    {
        boolean conflate = !conflatePrefixes.equalsIgnoreCase("false");
        boolean processExtraShadowedIds = fieldSpec.contains("';'");

        //int conflateThreshhold = conflate ? Integer.parseInt(conflatePrefixes) : 0;
        Set<String> fieldList = getFieldList(record, fieldSpec);
        if (fieldList.isEmpty())  {
            return(null);
        }
        if (processExtraShadowedIds)
        {
            loadExtraShadowedIds(extraIdsFilename);
            Set<String> newFieldList = new LinkedHashSet<String>();
            String extraString = addnlShadowedIds.get(record.getControlNumber());
          
            for (String field : fieldList)
            {
                String fieldparts[] = field.split(";");
                if (fieldparts.length != 2) continue;
                if (extraString == null || extraString.equals("") || !extraString.contains("|" + fieldparts[1] + "|"))
                {
                    newFieldList.add(fieldparts[0]);
                }
            }
            fieldList = newFieldList;
        }
        if (!conflate)
        {
            Comparator<String> comp = new StringNaturalCompare();
            Set<String> resultNormed = new TreeSet<String>(comp);
            for (String callNum : fieldList)
            {
                String val = callNum.trim().replaceAll("\\s\\s+", " ").replaceAll("\\s?\\.\\s?", ".");
                String nVal = val.replaceAll("^([A-Z][A-Z]?[A-Z]?) ([0-9])", "$1$2");
                if (!nVal.equals(val))
                {
                    val = nVal;
                }
                resultNormed.add(val);
            }
            return resultNormed;
        }
        else
        {
            Comparator<String> normedComparator = new Comparator<String>() 
            {
                public int compare(String s1, String s2)
                {
                    String s1Norm = s1.replaceAll("[. ]", "");
                    String s2Norm = s2.replaceAll("[. ]", "");
                    return s1Norm.compareToIgnoreCase(s2Norm);
                }
            };

            Map<String, Set<String>> resultNormed = getCallNumbersCleanedConflated(fieldList, false);
            Set<String> keys = resultNormed.keySet();
            Set<String> results = new TreeSet<String>(normedComparator);
            for (String key : keys)
            {
                Set<String> values = resultNormed.get(key);
                String valueArr[] = values.toArray(new String[0]);
                if (valueArr.length == 1)
                {
                    results.add(valueArr[0]);
                }
                else
                {
                    String prefix = valueArr[0];
                    for (int i = 1; i < valueArr.length; i++)
                    {
                        prefix = getCommonPrefix(prefix, valueArr[i], normedComparator);
                    }
                    if (prefix.lastIndexOf(' ') != -1)
                    {
                        prefix = prefix.substring(0, prefix.lastIndexOf(' '));
                    }
                    StringBuffer sb = new StringBuffer(prefix);
                    String sep = " ";
                    for (int i = 0; i < valueArr.length; i++)
                    {
                        valueArr[i] = valueArr[i].substring(prefix.length());
                    }
                    Comparator<String> comp = new StringNaturalCompare();
                    Arrays.sort(valueArr, comp);
                    for (int i = 0; i < valueArr.length; i++)
                    {
                        if (valueArr[i].length() > 0) 
                        {
                            sb.append(sep+valueArr[i]);
                            sep = ",";
                        }
                    }
                    if (sb.length() > 100 || valueArr.length > 10)
                    {
                        results.add(prefix + " (" + valueArr.length + " volumes)");
                    }
                    else
                    {
                        results.add(sb.toString());
                    }
                }
            }
            return (results);
        }
    }
 
    final static String stateTable[][] = 
    {{"Alabama", "Ala."}, {"Alaska", "Alaska"}, {"Arizona", "Ariz."}, {"Arkansas", "Ark."}, {"California", "Calif."}, 
     {"Colorado", "Colo."}, {"Connecticut", "Conn."}, {"Delaware", "Del."}, {"Florida", "Fla."}, {"Georgia", "Ga."}, 
     {"Hawaii", "Hawaii"}, {"Idaho", "Idaho"}, {"Illinois", "Ill."}, {"Indiana", "Ind."}, {"Iowa", "Iowa"},  
     {"Kansas", "Kan."},{"Kentucky", "Ky."}, {"Louisiana", "La."}, {"Maine", "Maine"}, {"Maryland", "Md."}, 
     {"Massachusetts", "Mass."}, {"Michigan", "Mich."}, {"Minnesota", "Minn."}, {"Mississippi", "Miss."}, {"Missouri", "Mo."},
     {"Montana", "Mont."}, {"Nebraska", "Neb."}, {"Nevada", "Nev."}, {"New Hampshire", "N.H."}, {"New Jersey", "N.J."},  
     {"New Mexico", "N.M."},{"New York", "N.Y."}, {"North Carolina", "N.C."}, {"North Dakota", "N.D."}, {"Ohio", "Ohio"}, 
     {"Oklahoma", "Okla."}, {"Oregon", "Or."}, {"Pennsylvania", "Pa."}, {"Rhode Island", "R.I."}, {"South Carolina", "S.C."},  
     {"South Dakota", "S.D."},{"Tennessee", "Tenn."}, {"Texas", "Tex."}, {"Utah", "Utah"}, {"Vermont", "Vt."}, 
     {"Virginia", "Va."}, {"Washington", "Wash."}, {"West Virginia", "W. Va."}, {"Wisconsin", "Wis."}, {"Wyoming", "Wyo."}, 
     {"New York (State)", "N.Y."}, {"District of Columbia", "D.C."}, {"Puerto Rico", "P.R."}, {"Virgin Islands", "V.I."}, 
     {"Alberta", "Alta."}, {"British Columbia", "B.C."}, {"Manitoba", "Man."}, {"Newfoundland and Labrador", "N.L."}, 
     {"New Brunswick", "N.B."}, {"Northwest Territories", "N.W.T."}, {"Nova Scotia", "N.S."}, {"Nunavut", "Nunavut"}, 
     {"Ontario", "Ont."}, {"Prince Edward Island", "P.E.I."}, {"Quebec", "Que'bec"}, {"Saskatoon", "Sask."}, {"Yukon", "Yukon"},
     {"Australian Capital Territory", "A.C.T."}, {"New South Wales", "N.S.W."}, {"Northern Territory", "N.T."}, {"Queensland", "Qld."}, 
     {"South Australia", "S. Aust."}, {"Tasmania", "Tas."}, {"Victoria", "Vic."}, {"Western Australia", "W.A." }};
    
    static Map<String, String> stateMap = null;
    
    private String getStateNameAbbrev(String stateName)
    {
        if (stateMap == null)
        {
            stateMap = new LinkedHashMap<String, String>();
            for (int i = 0; i < stateTable.length; i++)
            {
                stateMap.put(stateTable[i][0], stateTable[i][1]);
            }
        }
        return(stateMap.get(stateName));
    }

    final static String locationTypeNames[] = {"State", "Republic", "Principality", "Province", "Township", "County", "Town",
                                               "Judicial district", "Prefecture", "Region", "District", "Dept.", "Kingdom", 
                                               "Canton", "City", "Division", "Duchy", "Emirate", "Government", "Country",
                                               /* India: */ "Princely State", "Presidency", "Tahsil", "Taluka", "Tehsil", "Thana",
                                               /* China: */ "Sheng",
                                               /* Denmark: */  "Amt", "Herred",
                                               /* Thailand: */  "Amphoe",
                                               /* France: */ "Comte´",
                                               /* South/Central America: */ "Corregimiento", "Distrito Federal", "Intendancy", "Partido", 
                                               /* Religious: */ "Diocese", "diocese", "Archdiocese", "Archdeaconry", "Ecclesiastical principality",
                                               /* Poland: */ "Voivodeship", "Powiat",
                                               /* Germany:*/ "Landkreis", "Kreis", "Bezirk", "Electorate", "Grafschaft",
                                               /* Czech: */ "Okres", 
                                               /* Russia: */ "Oblast'", "Oblast", "Kray", 
                                               /* Hungary: */ "Comitat",
                                               /* Romania: */ "Judet",
                                               /* Indonesia: */ "Kabupaten", 
                                               /* Former: */ "Ancient city", "Ancient sanctuary", "Ancient site", "Extinct city", "Concentration camp", "Colony", "Site",
                                               /* Descriptive: */ "Peninsula", "Coast", "Cape", "Harbor", "Island", "Lake", "Oasis", "Tribal area" };
    static Set<String> locationTypeNameSet = null;

    private static boolean isLocationTypeWord(String name)
    {
        if (locationTypeNameSet == null)
        {
            locationTypeNameSet = new LinkedHashSet<String>();
            for (String locType : locationTypeNames)
            {
                locationTypeNameSet.add(locType);
            }
        }
        if (locationTypeNameSet.contains(name))  return(true);
        return(false);
    }
    
    private boolean isEqualsOrContains(String string1, String string2)
    {
        if (string1.equals(string2))  return(true);
        if (string1.contains(" and "+ string2)) return(true);
        if (string1.contains(string2 + " and ")) return(true);
        if (string1.contains(", "+ string2)) return(true);
        String tmp = getStateNameAbbrev(string2);
        if (tmp != null && tmp.equals(string1)) 
            return(true);
        return(false);
    }

    private Set<String> getSet650z(Record record)
    {
        Set<String> result = new LinkedHashSet<String>();
        List<VariableField> fields = (List<VariableField>)record.getVariableFields("650");
        for (VariableField f : fields)
        {
            DataField df = (DataField)f;
            List<Subfield> sfs = (List<Subfield>)df.getSubfields();
            boolean prevWasZ = false;
            StringBuffer part = new StringBuffer();
            boolean hadPrevZ = false;
            int zCount = 0;
            for (Subfield sf : sfs)
            {
                if (sf.getCode() == 'z')
                {
                    zCount++;
                    if (zCount > 2)
                    {
                        prevWasZ = true;
                    }
                    if (prevWasZ)
                    {
                        String data = Utils.cleanData(sf.getData());
                        if (data.equals("South America") || data.equals("Central America") || data.equals("United States"))
                        {
                            //part.insert(0, data+"#");
                        }
                        else
                        {
                            part.append("#");
                            part.append(Utils.cleanData(sf.getData()));
                        }
                    }
                    else
                    {
                        if (hadPrevZ)
                        {
                            hadPrevZ = true;
                        }
                        part.append(Utils.cleanData(sf.getData()));
                    }
                    prevWasZ = true;
                    hadPrevZ = true;
                }
                else
                {
                    zCount = 0;
                    if (prevWasZ)
                    {
                        result.add(part.toString());
                        part.setLength(0);
                        prevWasZ = false;
                    }
                }
            }
            if (prevWasZ)
            {
                result.add(part.toString());
                part.setLength(0);
                prevWasZ = false;
            }
        }
        return(result);   
    }
    
    public Set<String> getRegionFacet(final Record record)
    {
        Set<String> result = removeTrailingPunct(record, "651a");
        Set<String> sub650z = getSet650z(record);
        for (String fields650 : sub650z)
        {
            String parts[] = fields650.split("#");
            parts[0] = Utils.cleanData(parts[0]);
            result.add(parts[0]);
            for (int i = 1; i < parts.length; i++)
            {
                if (i == 2)  // 650  0$aEthnology$zRussia (Federation)$zSiberia$xResearch$zPoland$xHistory$vCongresses.  0$aLabor movement$zBrazil$zNatal$zDurban.
                {
                //  things to decide :  $z Colombia $z Zipaquira $z South America    or $z Germany $z Berlin $z Tiergarten or $z Nicaragua $z Rivas (Dept.)$z Central America  or  $z Italy $z Sicily  $z Camarina (Extinct city)
                    parts[0] = parts[1].replaceAll("((\\p{L}\\p{M}*|\\.|[- ])+(\\p{L}\\p{M}*|\\.))[ ]?\\(((\\p{L}\\p{M}*|\\.|[- ])+).*", "$1, $4");
                    i = 2;  
                }
                parts[i] = Utils.cleanData(parts[i]);
                String abbrev = getStateNameAbbrev(parts[0]);
                if (abbrev != null)
                {
                    parts[i] = parts[i] + " (" + abbrev + ")";
                }
                else if (parts[i].endsWith(")"))
                {
                    if (!parts[i].contains("("))
                    {
                        parts[i] = parts[i].substring(0, parts[i].length()-1) + " (" + parts[0] + ")";
                    }
                    else if (parts[i].matches(".*[ ]?\\((\\p{L}\\p{M}*|\\.|[- ])+\\)"))
                    {
                        String subparts[] = parts[i].split("[ ]?\\(", 2);
                        if (subparts.length > 1)
                        {
                            subparts[1] = subparts[1].substring(0, subparts[1].length()-1);
                            if (!subparts[1].equals(parts[0]) && isLocationTypeWord(subparts[1]))
                            {
                                parts[i] = subparts[0] + " (" + parts[0] + " : " + subparts[1] + ")";
                            }
                            else if (!isEqualsOrContains(subparts[1], parts[0]))
                            {
                                parts[i] = parts[i].substring(0, parts[i].length()-1) + ", " + parts[0] + ")";
                            }
                            //else leave parts[i] as is.
                            else
                            {
                                parts[i] = parts[i];
                            }
                        }
                        else
                        {
                            parts[i] = parts[i].substring(0, parts[i].length()-1) + ", " + parts[0] + ")";
                        }
                    }
                    else  //  things to decide :  $z Germany $z Blah (Something : District) or $z Italy $z Satricum (Lazio : Extinct city)
                    {
                        if (parts[i].matches("(\\p{L}\\p{M}*|\\.|[- ])+[ ]?\\((\\p{L}\\p{M}*|\\.|[- ])+ : (\\p{L}\\p{M}*|\\.|[- ])+\\)"))
                        {    
                        // equivalent of, but with expanded character sets to include unicode accented letters and accent marks : 
                        //    parts[i] = parts[i].replaceFirst("([-A-Za-z ]+[A-Za-z])[ ]?\\(([-A-Za-z ]+) : ([-A-Za-z ]+)\\)", 
                        //                                     "$1 ($2, "+parts[0]+" : $3)");
                            parts[i] = parts[i].replaceFirst("((\\p{L}\\p{M}*|\\.|[- ])+(\\p{L}\\p{M}*|\\.))[ ]?\\(((\\p{L}\\p{M}*|\\.|[- ])+) : ((\\p{L}\\p{M}*|\\.|[- ])+)\\)", 
                                                             "$1 ($4, "+parts[0]+" : $6)");
                        }
                        else
                            parts[i] = parts[i];
                    }
                }
                else 
                {
                    parts[i] = parts[i] + " (" + parts[0] + ")";
                }                
                result.add(parts[i]);
            }
        }
        return(result);
    }

    private String buildParsableURLString(DataField df, String defaultLabel)
    {
        String label = (df.getSubfield('z') != null) ? df.getSubfield('z').getData() : defaultLabel;
        String url = df.getSubfield('u').getData(); 
        String result = url + "||" + label;
        return(result);
    }
    
    public Set<String> getLabelledURL(final Record record, String defaultLabel)
    {
        Set<String> resultSet = new LinkedHashSet<String>();
        Set<String> backupResultSet = new LinkedHashSet<String>();
        List<?> urlFields = record.getVariableFields("856");
        for (Object field : urlFields)
        {
            if (field instanceof DataField)
            {
                DataField dField = (DataField)field;
                if (dField.getIndicator1() == '4' && dField.getIndicator2() == '0')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        resultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
                if (dField.getIndicator1() == '4' && dField.getIndicator2() == '1')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        resultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
                if (dField.getIndicator1() == '4' && dField.getIndicator2() == ' ')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        resultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
                if (dField.getIndicator1() == ' ' && dField.getIndicator2() == '0')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        backupResultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
                if (dField.getIndicator1() == ' ' && dField.getIndicator2() == '1')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        backupResultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
                if (dField.getIndicator1() == ' ' && dField.getIndicator2() == ' ')
                {
                    if (dField.getSubfield('u') != null) 
                    {
                        backupResultSet.add(buildParsableURLString(dField, defaultLabel));
                    }
                }
            }
        }
        if (resultSet.size() == 0 && backupResultSet.size() != 0)
        {
            return(backupResultSet);
        }
        return(resultSet);
    }
    
    private String getCommonPrefix(String string1, String string2, Comparator comp)
    {
        int l1 = string1.length();
        int l2 = string2.length();
        int l = Math.min(l1, l2);
        int prefixLen = l;
        for (int i = 0; i < l; i++)
        {
            if (comp.compare(string1.substring(i, i+1), string2.substring(i, i+1))!= 0)
            {
                prefixLen = i;
                break;
            }
        }
        return(string1.substring(0, prefixLen));
    }

    /**
     * Extract the OCLC number from a record
     * @param record
     * @return OCLC number
     */
    public String getOclcNum(final Record record)
    {
        Set<String> set = getFieldList(record, "035a");
        
        if (set.isEmpty())  {
            return(null);
        }
        
        Iterator<String> iter = set.iterator();
        
        while (iter.hasNext())
        {
            String value = (String)iter.next();
            if (value.contains("(OCoLC)"))  
            {
                value = value.replaceAll("\\(OCoLC\\)", "");
                return(value);
            }
        }
        return null;
    }
    
    public Set<String> getCombinedFormatNew(final Record record)
    {    
        // part1_format_facet = 000[6]:007[0], format_maps.properties(broad_format), first
        // part2_format_facet = 999t, format_maps.properties(format)

        String mapName1 = loadTranslationMap(null, "format_maps.properties(broad_format)");
        String mapName1a = loadTranslationMap(null, "format_maps.properties(broad_format_electronic)");
        String mapName2 = loadTranslationMap(null, "format_maps.properties(format_007)");
        String mapName3 = loadTranslationMap(null, "format_maps.properties(format)");

        Set<String> result = getFieldList(record, "999t");
        result = Utils.remap(result, findMap(mapName3), false);

        Set<String> f245h = getFieldList(record, "245h");
        if (Utils.setItemContains(f245h, "cartographic material"))
        {
            result.add("Map");
        }
        Set<String> urls = getFieldList(record, "856u");
        Set<String> format_007_raw = getFieldList(record, "007[0-1]");
        if (Utils.setItemContains(format_007_raw, "cr") || Utils.setItemContains(result, "Online"))
        {
            String other007 = null;
            String broadFormat = getFirstFieldVal(record, null, "000[6-7]");
            if (format_007_raw.size() >= 1)
            {
                for (String str007 : format_007_raw)
                {
                    if (!str007.equals("cr"))
                    {
                        other007 = str007;
                        break;
                    }
                }
            }
            if (other007 != null && other007.startsWith("v")) 
            {
                result.add(Utils.remap("v", findMap(mapName1a), true)); // Streaming Video
                result.add(Utils.remap("v", findMap(mapName2), true));  // Video
            }
            else if (broadFormat.equals("am")) 
            {
                result.add(Utils.remap("am", findMap(mapName1a), true)); // eBook
                result.add(Utils.remap("a", findMap(mapName1), true));  // Book
            }
            else if (broadFormat.equals("as"))
            {
                result.add(Utils.remap("as", findMap(mapName1a), true)); // Online
                result.add(Utils.remap("as", findMap(mapName1), true));  // Journal/Magazine
            }
            else if (broadFormat.startsWith("m"))
            {
                result.add(Utils.remap("m", findMap(mapName1), true));
            }
        }
        else if (Utils.setItemContains(urls, "serialssolutions"))
        {
            String serialsFormat = Utils.remap("as", findMap(mapName1), true);
            if (serialsFormat != null) result.add(serialsFormat);
        }
        else
        {
            String format_007 = getFirstFieldVal(record, mapName2, "007[0]");
            if (format_007 != null) 
            {
                result.add(format_007);
            }
            else 
            {
                String broadFormat = getFirstFieldVal(record, mapName1, "000[6-7]:000[6]");
                if (broadFormat != null) result.add(broadFormat);
            }
        }
        return(result);
    }
    
    public Set<String> getCombinedFormatNew2(final Record record)
    {    
        // part1_format_facet = 000[6]:007[0], format_maps.properties(broad_format), first
        // part2_format_facet = 999t, format_maps.properties(format)

        String mapName1 = loadTranslationMap(null, "format_maps.properties(broad_format)");
        String mapName1a = loadTranslationMap(null, "format_maps.properties(broad_format_electronic)");
        String mapName2 = loadTranslationMap(null, "format_maps.properties(format_007)");
        String mapName3 = loadTranslationMap(null, "format_maps.properties(format)");

        Set<String> result = getFieldList(record, "999t");
        result = Utils.remap(result, findMap(mapName3), false);

        Set<String> f245h = getFieldList(record, "245h");
        if (Utils.setItemContains(f245h, "cartographic material"))
        {
            result.add("Map");
        }
        Set<String> urls = getFieldList(record, "856u");
        Set<String> format_007_raw = getFieldList(record, "007[0-1]");
        if (Utils.setItemContains(format_007_raw, "cr") || Utils.setItemContains(result, "Online"))
        {
            String other007 = null;
            String broadFormat = getFirstFieldVal(record, null, "000[6-7]");
            if (format_007_raw.size() >= 1)
            {
                for (String str007 : format_007_raw)
                {
                    if (!str007.equals("cr"))
                    {
                        other007 = str007;
                        break;
                    }
                }
            }
            if (other007 != null && other007.startsWith("v")) 
            {
                result.add(Utils.remap("v", findMap(mapName1a), true)); // Streaming Video
                result.add(Utils.remap("v", findMap(mapName2), true));  // Video
            }
            else if (broadFormat.equals("am")) 
            {
                result.add(Utils.remap("am", findMap(mapName1a), true)); // eBook
                result.add(Utils.remap("a", findMap(mapName1), true));  // Book
            }
            else if (broadFormat.equals("as"))
            {
                result.add(Utils.remap("as", findMap(mapName1a), true)); // Online
                result.add(Utils.remap("as", findMap(mapName1), true));  // Journal/Magazine
            }
            else if (broadFormat.startsWith("m"))
            {
                result.add(Utils.remap("m", findMap(mapName1), true));
            }
        }
        else if (Utils.setItemContains(urls, "serialssolutions"))
        {
            String serialsFormat = Utils.remap("as", findMap(mapName1), true);
            if (serialsFormat != null) result.add(serialsFormat);
        }
        else
        {
            String format_007 = getFirstFieldVal(record, mapName2, "007[0]");
            String broadFormat = getFirstFieldVal(record, mapName1, "000[6-7]:000[6]");
                if (format_007 != null) result.add(format_007);
                if (broadFormat != null) result.add(broadFormat);
           //     if (broadFormat != null && format_007 != null) System.out.println("format diff for item: "+ record.getControlNumber()+" : format_007 = "+format_007+ "  broadFormat = " + broadFormat);
        }
        return(result);
    }

    public Set<String> getCombinedFormat(final Record record)
    {    
    	// part1_format_facet = 000[6]:007[0], format_maps.properties(broad_format), first
    	// part2_format_facet = 999t, format_maps.properties(format)

	    String mapName1 = loadTranslationMap(null, "format_maps.properties(broad_format)");
	    String mapName2 = loadTranslationMap(null, "format_maps.properties(format_007)");
	    String mapName3 = loadTranslationMap(null, "format_maps.properties(format)");

        Set<String> result = getFieldList(record, "999t");
        result = Utils.remap(result, findMap(mapName3), false);

        Set<String> urls = getFieldList(record, "856u");
	    if (Utils.setItemContains(urls, "serialssolutions"))
	    {
            String serialsFormat = Utils.remap("as", findMap(mapName1), true);
            if (serialsFormat != null) result.add(serialsFormat);
	    }
	    else
	    {
	        String format_007 = getFirstFieldVal(record, mapName2, "007[0]");
            if (format_007 != null) 
            {
                result.add(format_007);
            }
            else 
            {
                String broadFormat = getFirstFieldVal(record, mapName1, "000[6-7]:000[6]");
                if (broadFormat != null) result.add(broadFormat);
            }
	    }
        return(result);
    }
    
//    public Set<String> getLocationWithShadowing(final Record record, String propertiesMap)
//    {
//        String mapName = loadTranslationMap(null, propertiesMap);
//
//        Set<String> fields = getFieldList(record, "999kl';'");
//        Set<String> result = new LinkedHashSet<String>();
//        for (String field : fields)
//        {
//            String fparts[] = field.split(";");
//            if (fparts.length == 1)
//            {
//                String mappedFpart = Utils.remap(fparts[0], findMap(mapName), true);
//                if (mappedFpart != null) result.add(mappedFpart);
//            }
//            else if (fparts.length == 2)
//            {
//                String mappedFpart1 = Utils.remap(fparts[0], findMap(mapName), true);
//                String mappedFpart2 = Utils.remap(fparts[1], findMap(mapName), true);
//                if (mappedFpart1 != null && mappedFpart1.equals("-") && mappedFpart2 != null)
//                {
//                    result.add(mappedFpart2);
//                }
//                else if (mappedFpart1 != null  && mappedFpart2 != null)
//                {
//                    result.add(mappedFpart1);
//                    result.add(mappedFpart2);
//                }
//            }
//        }
//        return(result);        
//    }
    private void loadExtraShadowedIds(String filename)
    {
        if (addnlShadowedIds == null)
        {
            addnlShadowedIds = new LinkedHashMap<String, String>();
            InputStream addnlIdsStream = Utils.getPropertyFileInputStream(null, filename);
            BufferedReader addnlIdsReader = new BufferedReader(new InputStreamReader(addnlIdsStream));
            String line;
            try
            {
                while ((line = addnlIdsReader.readLine()) != null)
                {
                    String linepts[] = line.split("\\|");
                    if (linepts.length == 1) 
                    {
                        addnlShadowedIds.put(linepts[0], "");
                    }
                    else
                    {
                        String existing = addnlShadowedIds.get(linepts[0]);
                        if (existing == null) addnlShadowedIds.put(linepts[0], "|" + linepts[1] + "|"); 
                        else if (existing.equals("")) continue;
                        else addnlShadowedIds.put(linepts[0], existing + linepts[1] + "|");
                    }
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public Set<String>getCustomLibrary(final Record record, String visibilityMap, String libraryMap)
    {
        Set<String> resultSet = new LinkedHashSet<String>();
        List<?> fields999 = record.getVariableFields("999");
        String visMapName = loadTranslationMap(null, visibilityMap);
        String libMapName = loadTranslationMap(null, libraryMap);
        for ( DataField field : (List<DataField>)fields999 )
        {
            Subfield curLocF = field.getSubfield('k');
            Subfield homeLocF = field.getSubfield('l');
            Subfield libF = field.getSubfield('m');
            String lib = (libF != null ? libF.getData() : null);
            String mappedLib = Utils.remap(lib, findMap(libMapName), true);
            if (mappedLib == null || resultSet.contains(mappedLib))  continue;
            String curLoc = (curLocF != null ? curLocF.getData() : null);
            String homeLoc = (homeLocF != null ? homeLocF.getData() : null);
            String mappedHomeVis = Utils.remap(homeLoc, findMap(visMapName), true);
            if (mappedHomeVis.equals("HIDDEN") )
            {
                continue;
            }
            if (curLoc != null)
            {
                String mappedCurVis = Utils.remap(curLoc, findMap(visMapName), true);
                if (mappedCurVis.equals("HIDDEN")) continue; // this copy of the item is Hidden, go no further
            }
            resultSet.add(mappedLib);
        }
//        for ( DataField field : (List<DataField>)fields999 )
//        {
//            Subfield curLocF = field.getSubfield('k');
//            Subfield homeLocF = field.getSubfield('l');
//            Subfield libF = field.getSubfield('m');
//            String lib = (libF != null ? libF.getData() : null);
//            String mappedLib = Utils.remap(lib, findMap(libMapName), true);
//            if (mappedLib != null && !resultSet.contains(mappedLib))  
//            {
//                resultSet.add("NOT "+mappedLib);
//            }
//        }
        return(resultSet);
    }
    
    public Set<String>getCustomLocation(final Record record, String locationMap, String visibilityMap, String libraryMap)
    {
        Set<String> resultSet = new LinkedHashSet<String>();
        List<?> fields999 = record.getVariableFields("999");
        String locMapName = loadTranslationMap(null, locationMap);
        String visMapName = loadTranslationMap(null, visibilityMap);
        String libMapName = loadTranslationMap(null, libraryMap);
        for ( DataField field : (List<DataField>)fields999 )
        {
            Subfield curLocF = field.getSubfield('k');
            Subfield homeLocF = field.getSubfield('l');
            Subfield libF = field.getSubfield('m');
            String curLoc = (curLocF != null ? curLocF.getData() : null);
            String homeLoc = (homeLocF != null ? homeLocF.getData() : null);
            String lib = (libF != null ? libF.getData() : null);
            String mappedHomeVis = Utils.remap(homeLoc, findMap(visMapName), true);
            String mappedHomeLoc = Utils.remap(homeLoc, findMap(locMapName), true);
            if (mappedHomeVis.equals("VISIBLE") && mappedHomeLoc == null)
            {
                String combinedLocMapped = Utils.remap(homeLoc + "__" + lib, findMap(locMapName), true);
                if (combinedLocMapped != null) mappedHomeLoc = combinedLocMapped;
            }
            String mappedLib = Utils.remap(lib, findMap(libMapName), true);
            if (curLoc != null)
            {
                String mappedCurLoc = Utils.remap(curLoc, findMap(locMapName), true);
                String mappedCurVis = Utils.remap(curLoc, findMap(visMapName), true);
                if (mappedCurVis.equals("HIDDEN")) continue; // this copy of the item is Hidden, go no further
                if (mappedCurLoc != null) 
                {
                    if (mappedCurLoc.contains("$m"))
                    {
          //              mappedCurLoc.replaceAll("$l", mappedHomeLoc);
                        mappedCurLoc = mappedCurLoc.replaceAll("[$]m", mappedLib);
                    }
                    resultSet.add(mappedCurLoc);
                    continue;   // Used
                }
            }
            if (mappedHomeVis.equals("HIDDEN"))  continue; // this copy of the item is Hidden, go no further
            if (mappedHomeLoc != null && mappedHomeLoc.contains("$"))
            {
                mappedHomeLoc.replaceAll("$m", mappedLib);
            }
            if (mappedHomeLoc != null) resultSet.add(mappedHomeLoc);
        }
        return(resultSet);
    }
    
    public Set<String> getCustomLanguage(final Record record, String propertiesMap)
    {
        Set<String> resultSet = new LinkedHashSet<String>();
        String mapName = loadTranslationMap(null, propertiesMap);
        String primaryLanguage = getFirstFieldVal(record, mapName, "008[35-37]");
        Set<String> otherLanguages = getFieldList(record, "041a:041d");
        otherLanguages = Utils.remap(otherLanguages, findMap(mapName), true);
        Set<String> translatedFrom = getFieldList(record, "041h");
        translatedFrom = Utils.remap(translatedFrom, findMap(mapName), true);
        Set<String> subtitleLanguage = getFieldList(record, "041b");
        subtitleLanguage = Utils.remap(subtitleLanguage, findMap(mapName), true);
        Set<String> format = getCombinedFormat(record);
        boolean isBook = Utils.setItemContains(format, "Book") || Utils.setItemContains(format, "Journal");
        boolean isDVD = Utils.setItemContains(format, "DVD") ;
        Set<String> notesFields = getFieldList(record, "500a");
        boolean isTranslated = Utils.setItemContains(notesFields, "[Tt]ranslat((ed)|(ion))");
        if (primaryLanguage != null)  resultSet.add(primaryLanguage);
        if (primaryLanguage != null && Utils.setItemContains(otherLanguages, primaryLanguage))
        {
            otherLanguages.remove(primaryLanguage);
        }
        if (isBook && isTranslated && otherLanguages.size() == 1 && translatedFrom.size() == 0)
        {
            copySetWithSuffix(resultSet, otherLanguages, " (translated from)");
        }
        else 
        {
            if (isDVD)
                copySetWithSuffix(resultSet, otherLanguages, " (dubbed in)");
            else
                copySetWithSuffix(resultSet, otherLanguages, " (also in)");
            
            if (primaryLanguage != null && Utils.setItemContains(translatedFrom, primaryLanguage))
            {
                translatedFrom.remove(primaryLanguage);
            }
            copySetWithSuffix(resultSet, translatedFrom, " (translated from)");
        }
        copySetWithSuffix(resultSet, subtitleLanguage, (isBook ? " (summary in)" : " (subtitles in)") );
        return(resultSet);
    }
    
    private void copySetWithSuffix(Set<String> resultSet, Set<String> languageList, String suffix)
    {
        for (String language : languageList)
        {
            String toAdd = language + suffix;
            resultSet.add(toAdd);
        }  
    }

    public String getShadowedLocation(final Record record, String propertiesMap, String returnHidden, String processExtra)
    {
        boolean processExtraShadowedIds = processExtra.startsWith("extraIds");
        if (processExtraShadowedIds)
        {
            loadExtraShadowedIds(extraIdsFilename);
        }
        boolean returnHiddenRecs = returnHidden.startsWith("return");
        String mapName = loadTranslationMap(null, propertiesMap);
        
        Set<String> fields = getFieldList(record, "999ikl';'");
        boolean visible = false;
        String extraString = null;
        if (processExtraShadowedIds && addnlShadowedIds.containsKey(record.getControlNumber()))
        {
            extraString = addnlShadowedIds.get(record.getControlNumber());
        }  
        if ("".equals(extraString))  visible = false;
        else
        {
            for (String field : fields)
                {
                    String fparts[] = field.split(";");
                    if (extraString != null && extraString.contains("|" + fparts[0] + "|"))
                    {
                        // this holding is marked as Hidden via the addnlShadowedIds data file
                        // so simply continue, and unless another non-Hidden holding is found the 
                        // record will be not visible.
                        continue;
                    }
                    else if (fparts.length == 2)
                    {
                        String mappedFpart = Utils.remap(fparts[1], findMap(mapName), true);
                        if (mappedFpart.equals("VISIBLE"))  visible = true;
                    }
                    else if (fparts.length == 3)
                    {
                        String mappedFpart1 = Utils.remap(fparts[1], findMap(mapName), true);
                        String mappedFpart2 = Utils.remap(fparts[2], findMap(mapName), true);
                        if (mappedFpart1.equals("VISIBLE") && mappedFpart2.equals("VISIBLE"))
                        {
                            visible = true;
                        }
                    }
                }
        }
        String result = (visible ? "VISIBLE" : "HIDDEN"); 
        if (!visible && !returnHiddenRecs)
        {
            return(null);
        }
        return(result);
    }
    
    public String getPublicationDate(final Record record)
    {
        String field008 = getFirstFieldVal(record, "008");
        String pubDateFull = getFieldVals(record, "260c", ", ");
        String pubDateJustDigits = pubDateFull.replaceAll("[^0-9]", "");       
        String pubDate260c = getDate(record);
        if (field008 == null || field008.length() < 16) 
        {
            return(pubDate260c);
        }
        String field008_d1 = field008.substring(7, 11);
        String field008_d2 = field008.substring(11, 15);
        String retVal = null;
        char dateType = field008.charAt(6);
        if (dateType == 'r' && field008_d2.equals(pubDate260c)) retVal = field008_d2;
        else if (field008_d1.equals(pubDate260c))               retVal = field008_d1;
        else if (field008_d2.equals(pubDate260c))               retVal = field008_d2;
        else if (pubDateJustDigits.length() == 4 && pubDate260c != null &&
                 pubDate260c.matches("(20|19|18|17|16|15)[0-9][0-9]"))
                                                                retVal = pubDate260c;
        else if (field008_d1.matches("(20|1[98765432])[0-9][0-9]"))        
                                                                retVal = field008_d1;
        else if (field008_d2.matches("(20|1[98765432])[0-9][0-9]"))        
                                                                retVal = field008_d2;
        else                                                    retVal = pubDate260c;
        return(retVal);
    }
    
    /**
     * returns the publication date groupings from a record, if it is present
     * @param record
     * @return Set of Strings containing the publication date groupings associated
     *   with the publish date
     */
    public Set<String> getPubDateGroups(final Record record, String Mapfilename)
    {
        Set<String> resultSet = new LinkedHashSet<String>();
        int cYearInt = Calendar.getInstance().get(Calendar.YEAR); 
        String mapName = loadTranslationMap(null, Mapfilename);
        
        // get the pub date, with decimals assigned for inclusion in ranges
        String publicationDate =  getPublicationDate(record);
        if (publicationDate != null)
        {
            int year = Integer.parseInt(publicationDate);
            // "this year" and "last three years" are for 4 digits only
            if ( year >= (cYearInt - 1))   resultSet.add("thisyear");
            if ( year >= (cYearInt - 2))   resultSet.add("lasttwoyears");
            if ( year >= (cYearInt - 3))   resultSet.add("lastthreeyears");
            if ( year >= (cYearInt - 5))   resultSet.add("lastfiveyears");
            if ( year >= (cYearInt - 10))  resultSet.add("lasttenyears");
            if ( year >= (cYearInt - 20))  resultSet.add("lasttwentyyears");
            if ( year >= (cYearInt - 50))  resultSet.add("last50years");
            if (year < (cYearInt - 50) && (year > -1.0))
                resultSet.add("morethan50years");
        }
        resultSet = Utils.remap(resultSet, findMap(mapName), true);
        return resultSet;   
    }

}
