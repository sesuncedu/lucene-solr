package edu.stanford;

import static edu.stanford.StanfordIndexer.*;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * junit4 tests for Stanford University revisions to solrmarc
 * @author Naomi Dushay
 *
 */
public class FormatTests extends BibIndexTest {
	
	private final String testDataFname = "formatTests.mrc";
	// TODO: change this field name to format_facet
	private String fldName = "format";

@Test
	public final void testFldProperties() 
		throws ParserConfigurationException, IOException, SAXException
	{
        createIxInitVars(testDataFname);
        assertStringFieldProperties(fldName, solrCore, sis);
		assertFieldMultiValued(fldName, solrCore);
		assertFieldIndexed(fldName, solrCore);
		assertFieldStored(fldName, solrCore);
		// are values as expected?
		assertEquals("format string incorrect: ", "Book", Format.BOOK.toString());
		assertEquals("format string incorrect: ", "Computer File", Format.COMPUTER_FILE.toString());
		assertEquals("format string incorrect: ", "Conference Proceedings", Format.CONFERENCE_PROCEEDINGS.toString());
		assertEquals("format string incorrect: ", "Journal", Format.JOURNAL.toString());
		assertEquals("format string incorrect: ", "Journal/Periodical", Format.JOURNAL_PERIODICAL.toString());
		assertEquals("format string incorrect: ", "Manuscript/Archive", Format.MANUSCRIPT_ARCHIVE.toString());
		assertEquals("format string incorrect: ", "Map/Globe", Format.MAP_GLOBE.toString());
		assertEquals("format string incorrect: ", "Music - Recording", Format.MUSIC_RECORDING.toString());
		assertEquals("format string incorrect: ", "Music - Score", Format.MUSIC_SCORE.toString());
		assertEquals("format string incorrect: ", "Other", Format.OTHER.toString());
		assertEquals("format string incorrect: ", "Serial Publication", Format.SERIAL_PUBLICATION.toString());
		assertEquals("format string incorrect: ", "Sound Recording", Format.SOUND_RECORDING.toString());
		
		// TODO: field is a facet - should there be solr request with fq param
		//   to test format as a facet ??
	}

	/**
	 * Test assignment of Book format
	 *   includes monographic series
	 */
@Test
	public final void testBookFormat() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
		String fldVal = Format.BOOK.toString();
		
		Set<String> docIds = new HashSet<String>();
		docIds.add("leader06a07m");
		docIds.add("leader06t07a");
		// monographic series
		docIds.add("leader07s00821m");
		docIds.add("5987319");
		docIds.add("5598989");
		docIds.add("223344");  // 006/00 s  and 006/04 m
		docIds.add("5666387");
		docIds.add("666");

		assertFieldValues(fldName, fldVal, docIds, sis);

		assertSearchResults(fldName, fldVal, docIds, sis);

		// formerly believed to be monographic series 
		assertDocHasNoFieldValue("leader07b00600s00821m", fldName, fldVal, sis);
	}


	/**
	 * Test assignment of Journal format
	 */
//no longer using Journal, as of 2008-12-02
//@Test
	public final void testJournalFormat() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        String fldVal = "Journal";

		Set<String> docIds = new HashSet<String>();
		docIds.add("leader07sNo00600821p");
		docIds.add("335577");
		
		assertFieldValues(fldName, fldVal, docIds, sis);

		assertSearchResults(fldName, fldVal, docIds, sis);

		// LCPER in 999w - but Serial Publication
		assertDocHasNoFieldValue("460947", fldName, fldVal, sis);
		// 006/00 s but 006/04 blank  leader/07 b  008/21 p
		assertDocHasNoFieldValue("leader07b00600s00821p", fldName, fldVal, sis);
	}


	/**
	 * Test assignment of Serial Publication format
	 */
//no longer using Serial Publication, as of 2008-12-02
//@Test
	public final void testSerialPubFormat() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        String fldVal = "Serial Publication";
		
		Set<String> docIds = new HashSet<String>();
		docIds.add("leader06a07s"); // leader/07 s 008/21 blank
		docIds.add("4114632");
		docIds.add("123");
		docIds.add("leader07b00600s00821m"); // 006/00 s /04 blank
		docIds.add("leader07b00600s00821p"); // 006/00 s /04 blank 
		docIds.add("460947");  // even though LCPER in 999 w
		docIds.add("446688");  // even though DEWEYPER in 999 w
		
		
		assertFieldValues(fldName, fldVal, docIds, sis);
		
		assertSearchResults(fldName, "\"" + fldVal + "\"", docIds, sis);

		// leader/07s 008/21 d   006/00 s  006/04 d -- other 
		assertDocHasNoFieldValue("112233", fldName, fldVal, sis);
	}


	/**
	 * Test assignment of Journal/Periodical format
	 */
@Test
	public final void testJournalPeriodicalFormat() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        String fldVal = "Journal/Periodical";
		
		Set<String> docIds = new HashSet<String>();
		docIds.add("leader06a07s"); // leader/07 s 008/21 blank
		docIds.add("4114632");
		docIds.add("123");
		docIds.add("leader07b00600s00821m"); // 006/00 s /04 blank
		docIds.add("leader07b00600s00821p"); // 006/00 s /04 blank 
		docIds.add("460947");  // even though LCPER in 999 w
		docIds.add("446688");  // even though DEWEYPER in 999 w
		docIds.add("leader07sNo00600821p");
		docIds.add("335577");
		
		assertFieldValues(fldName, fldVal, docIds, sis);
		
		assertSearchResults(fldName, "\"" + fldVal + "\"", docIds, sis);
	
		// leader/07s 008/21 d   006/00 s  006/04 d -- other 
		assertDocHasNoFieldValue("112233", fldName, fldVal, sis);
	}


	/**
	 * Test assignment of Newspaper format
	 */
@Test
	public final void testNewspaper() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        String fldVal = Format.NEWSPAPER.toString();
		
		Set<String> docIds = new HashSet<String>();
		docIds.add("newspaper");
		docIds.add("leader07sNo00600821n");
		docIds.add("334455");
		
		assertFieldValues(fldName, fldVal, docIds, sis);
		
		assertSearchResults(fldName, fldVal, docIds, sis);
		
		// leader/07b 006/00s 008/21n - serial publication
		assertDocHasNoFieldValue("leader07b00600s00821n", fldName, fldVal, sis);
	}

	/**
	 * Test assignment of Conference Proceedings format
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
@Test
	public final void testConferenceProceedings() 
			throws ParserConfigurationException, SAXException, IOException
	{
        createIxInitVars(testDataFname);
        String fldVal = Format.CONFERENCE_PROCEEDINGS.toString();
		
		Set<String> docIds = new HashSet<String>();
		docIds.add("5666387");
		docIds.add("666");
		
		assertFieldValues(fldName, fldVal, docIds, sis);
		
		assertSearchResults(fldName, "\"" + fldVal + "\"", docIds, sis);
	}


	/**
	 * Test assignment of Other format
	 */
@Test
	public final void testOtherFormat() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        String fldVal = Format.OTHER.toString();

		Set<String> docIds = new HashSet<String>();
		docIds.add("leader06t07b");
		docIds.add("leader06k00833w"); 
		docIds.add("leader06g00833w"); 
		docIds.add("leader06m00826u"); 
		docIds.add("leader07s00600j00821d"); 
		docIds.add("leader07b00600s00821n"); // 006/00 s /04 w
		// instructional kit 
		docIds.add("leader06o"); 
		// object 
		docIds.add("leader06r"); 
		// web site 
		docIds.add("leader07sNo00600821w"); 
		docIds.add("leader07b00600s00821w"); 
		docIds.add("321");
		docIds.add("112233");  // leader/07 s 008/21 d, 006/00 s 006/04 d
		docIds.add("7117119"); // leader/07 s, 006/00 m, 008/21 |
		docIds.add("778899");  // leader/07 s 008/21 d, 006/00 j 006/04 p
		
		assertFieldValues(fldName, fldVal, docIds, sis);
		
		assertSearchResults(fldName, fldVal, docIds, sis);
	}


	/**
	 * Test population of format field (values not in individual test methods)
	 */
@Test
	public final void testRemainingFormats() 
			throws IOException, ParserConfigurationException, SAXException 
	{
        createIxInitVars(testDataFname);
        // map/globe
		assertDocHasFieldValue("leader06e", fldName, "Map/Globe", sis); 
		assertDocHasFieldValue("leader06f", fldName, Format.MAP_GLOBE.toString(), sis); 
		// image
		String imgVal = "Image";
		assertDocHasFieldValue("leader06k00833i", fldName, imgVal, sis); 
		assertDocHasFieldValue("leader06k00833k", fldName, imgVal, sis); 
		assertDocHasFieldValue("leader06k00833p", fldName, imgVal, sis); 
		assertDocHasFieldValue("leader06k00833s", fldName, imgVal, sis); 
		assertDocHasFieldValue("leader06k00833t", fldName, imgVal, sis); 
		// video
		assertDocHasFieldValue("leader06g00833m", fldName, "Video", sis); 
		assertDocHasFieldValue("leader06g00833v", fldName, Format.VIDEO.toString(), sis); 
		// audio - non-music
		assertDocHasFieldValue("leader06i", fldName, "Sound Recording", sis); 
		// music - audio
		assertDocHasFieldValue("leader06j", fldName, "Music - Recording", sis); 
		// music - score
		assertDocHasFieldValue("leader06c", fldName, "Music - Score", sis); 
		assertDocHasFieldValue("leader06d", fldName, Format.MUSIC_SCORE.toString(), sis); 
		assertDocHasFieldValue("245hmicroform", fldName, Format.MUSIC_SCORE.toString(), sis); 
		// manuscript/archive
		assertDocHasFieldValue("leader06b", fldName, "Manuscript/Archive", sis); 
		assertDocHasFieldValue("leader06p", fldName, Format.MANUSCRIPT_ARCHIVE.toString(), sis); 
		// thesis
		assertDocHasFieldValue("502", fldName, "Thesis", sis); 
		// computer file
		assertDocHasFieldValue("leader06m00826a", fldName, "Computer File", sis); 
		// microfilm
		assertDocHasFieldValue("245hmicroform", fldName, "Microformat", sis); 
		
		String scoreVal = "\"" + "Music - Score" + "\"";
		Set<String> docIds = new HashSet<String>();
		docIds.add("leader06c");
		docIds.add("leader06d");
		docIds.add("245hmicroform");
		assertSearchResults(fldName, scoreVal, docIds, sis);		
	}


	/**
	 * test format population based on ALPHANUM field values from 999
	 */
@Test
	public final void testFormatsFrom999()
			throws IOException, ParserConfigurationException, SAXException
	{
		// test formats assigned by strings in ALPHANUM call numbers
		createIxInitVars("callNumberTests.mrc");
		
		// 999 ALPHANUM starting with MFLIM
		assertDocHasFieldValue("1261173", fldName, Format.MICROFORMAT.toString(), sis); 
		// 999 ALPHANUM starting with MCD
		assertDocHasFieldValue("1234673", fldName, Format.MUSIC_RECORDING.toString(), sis); 
	}

}
