package de.tum.bgu.msm;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;

public class SiloMstmTest {
	private static final Logger log = Logger.getLogger(SiloMstmTest.class) ;
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	public final static String filename_dd = "./test/scenarios/annapolis_reduced/microData_reduced/dd_2001.csv";
	public final static String filename_hh = "./test/scenarios/annapolis_reduced/microData_reduced/hh_2001.csv";
	public final static String filename_jj = "./test/scenarios/annapolis_reduced/microData_reduced/jj_2001.csv";
	public final static String filename_pp = "./test/scenarios/annapolis_reduced/microData_reduced/pp_2001.csv";
	public final static String filename_a0 = "./test/scenarios/annapolis_reduced/testing/accessibility_2000.csv";
	public final static String filename_a1 = "./test/scenarios/annapolis_reduced/testing/accessibility_2001.csv";
	public final static String filename_gi = "./test/scenarios/annapolis_reduced/testing/given_impedance_2000.csv";
	public final static String filename_st = "./test/scenarios/annapolis_reduced/status.csv";


	public static void cleanUp() {
		log.info("cleaning up ...");
		new File(filename_dd).delete() ;
		new File(filename_hh).delete() ;
		new File(filename_jj).delete() ;
		new File(filename_pp).delete() ;
		new File(filename_a0).delete() ;
		new File(filename_a1).delete() ;
		new File(filename_gi).delete() ;
		new File(filename_st).delete() ;
		new File("timeTracker.csv").delete();
		new File("priceUpdate2000.csv").delete();
	}

	
	/**
	 * This test does NOT test MSTM, despite the name: transport.model.years is set to -1, effectively ignoring the transport model.
	 */
	@Test
	public final void testMainReduced() {
		// yyyy test writes in part to same directory as other tests (e.g. .../microData_reduced/...), which is not so great.  kai, aug'16
		
		cleanUp();
		
		String[] args = {"./test/scenarios/annapolis_reduced/javaFiles/siloMstm_reduced.properties"}; 

		try {
			SiloMstm.main( args );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail( "something did not work" ) ;
		}

		//TODO: apparently this is required for some machines, as the test class of utils is not initialized at this point,
		// resulting in exceptions when trying to get output directory 'ana,nico 07/'17
		try {
			utils.initWithoutJUnitForFixture(this.getClass(), this.getClass().getMethod("testMainReduced", null));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}


		
		// The following passes when the test is run alone, but fails when all tests are run in conjunction.  Not sure what it is ...
		// ... but it is certainly not good that multiple test write in the same directories.  "scenarios" should be something like shared input
		// for big files that we don't want to repeat, but it is not a good place for output.
		// If the SILO directory structure is fairly strongly hardcoded, an option would be that tests clean up after themselves.
		// kai, aug'16
		// This is for the time being resolved by "forkmode=always" in the silo pom.xml, meaning that each test 
		// starts a separate JVM.  kai, aug'16
		
		{
			long checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "./dd_2001.csv");
			long checksum_run = CRCChecksum.getCRCFromFile(filename_dd);
			assertEquals("Dwelling files are different",  checksum_ref, checksum_run);
		}{
			long checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "./hh_2001.csv");
			long checksum_run = CRCChecksum.getCRCFromFile(filename_hh);
			assertEquals("Household files are different",  checksum_ref, checksum_run);
		}{
			long checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "./jj_2001.csv");
			long checksum_run = CRCChecksum.getCRCFromFile(filename_jj);
			assertEquals("Job files are different",  checksum_ref, checksum_run);
		}{
			long checksum_ref = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "./pp_2001.csv");
			long checksum_run = CRCChecksum.getCRCFromFile(filename_pp);
			assertEquals("Population files are different",  checksum_ref, checksum_run);
		}

		
		cleanUp() ;
	}


}
