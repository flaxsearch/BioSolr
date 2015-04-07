package uk.co.flax.biosolr.pdbe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.WsResultType;

public class TestFastaJob {

	private static final String RESULT_PATH = "../../../../../result2";
	
	@Test
	public void parse() throws IOException, URISyntaxException {
		byte[] result = Files.readAllBytes(Paths.get(TestFastaJob.class.getResource(RESULT_PATH).toURI()));
		
		JDispatcherService_PortType fasta = mock(JDispatcherService_PortType.class);
		when(fasta.getStatus(null)).thenReturn(FastaStatus.DONE);
		WsResultType[] types = new WsResultType[] { mock(WsResultType.class) };
		when(fasta.getResultTypes(null)).thenReturn(types);
		when(fasta.getResult(null, null, null)).thenReturn(result);
		
		InputParameters params = new InputParameters();
		params.setProgram("ssearch");
		params.setDatabase(new String[] { "pdb" });
		params.setStype("protein");
        params.setSequence("<DUMMY>");
        params.setExplowlim(0.0d);
        params.setExpupperlim(1.0d);
        params.setScores(1000);
        params.setAlignments(1000);
		
		FastaJob job = new FastaJob(fasta, "sameer@ebi.ac.uk", params);
		job.run();
		FastaJobResults results = job.getResults();
		
		// there would be 1000 results if there were no repeats (which would be correct)
		assertEquals(504, results.getNumChains());
		assertEquals(317, results.getNumEntries());
		
		assertEquals(504, results.getAlignments(false).size());
		assertEquals(317, results.getAlignments(true).size());
		
		// we'll test the first and last alignments for unique pdb ids
		// NB getAlignments() returns a LinkedHashMap, so key set is in predictable order
		Map<String, Alignment> alignments = results.getAlignments(true);
		String pdbIdChain0 = null;
		String pdbIdChain1 = null;
		for (String pdbIdChain : alignments.keySet()) {
			if (pdbIdChain0 == null) {
				pdbIdChain0 = pdbIdChain;
			}
			pdbIdChain1 = pdbIdChain;
		}
		Alignment a0 = alignments.get(pdbIdChain0);
		String qs0 = a0.getQuerySequenceString();
		String rs0 = a0.getReturnSequenceString();
		Alignment a1 = alignments.get(pdbIdChain1);
		String qs1 = a1.getQuerySequenceString();
		String rs1 = a1.getReturnSequenceString();
		String pdbIds = StringUtils.join(results.getPdbIds(), ',');
		String pdbIdChains = StringUtils.join(alignments.keySet(), ',');
		
		assertTrue(qs0.length() == rs0.length());
		assertTrue(qs1.length() == rs1.length());
		
		assertEquals("1CZI_E", pdbIdChain0);

		assertEquals("GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLG" +
					 "KPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFD" +
					 "NMMNRHLVAQDLFSVYMDRNGQESMLTLGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGG" +
					 "CQAILDTGTSKLVGPSSDILNIQQAIGATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQ" +
					 "GFCTSGFQSENHSQKWILGDVFIREYYSVFDRANNLVGLAKAI", qs0);
		
		assertEquals("GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLG" +
					 "KPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFD" +
					 "NMMNRHLVAQDLFSVYMDRNGQESMLTLGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGG" +
					 "CQAILDTGTSKLVGPSSDILNIQQAIGATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQ" +
					 "GFCTSGFQSENHSQKWILGDVFIREYYSVFDRANNLVGLAKAI", rs0);
		
		assertEquals("3CID_A", pdbIdChain1);
		
		assertEquals("  GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWV---PSIYCKSNACKNHQRFDPRKSST" +
					 "FQNLGKPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTY--AEFDGILGMAYPSLAS-" +
					 "EYSI-PVFDNMMNRHLVAQDLFSVYMDRNG----QESMLT-------LGAIDPSYYTGSLHWVPVTVQQY" +
					 "WQFTVDSVTISG--VVVACE--GGCQAILDTGTSKLVGPS----SDILNIQQAIGATQNQYGEF---DID" +
					 "CDNLSYMPTVVF---------EINGKMYPLT--PSAY--------TSQDQGFCTSGFQSENHSQKWILGD" +
					 "VFIREYYSVFDRANNLVGLAKAI                            ", qs1);
		
		assertEquals("GSFVEMVDNLRGKSGQGYYVEMTVGSPPQTLNILVDTGSSNFAVGAAPHPFL-------HRYYQRQLSST" +
					 "YRDLRKGVYVPYTQGKWEGELGTDLVSIPHGPNVTVRANIAAITESDKFFINGSNWEGILGLAYAEIARP" +
					 "DDSLEPFFDSLVKQTHVP-NLFSLQLCGAGFPLNQSEVLASVGGSMIIGGIDHSLYTGSLWYTPIRREWY" +
					 "YEVIIVRVEINGQDLKMDCKEYNYDKSIVDSGTTNLRLPKKVFEAAVKSIKAASSTEKFPDGFWLGEQLV" +
					 "CWQAGTTPWNIFPVISLYLMGEVTNQSFRITILPQQYLRPVEDVATSQDD--CYK-FAISQSSTGTVMGA" +
					 "VIMEGFYVVFDRARKRIGFAVSACHVHDEFRTAAVEGPFVTLDMEDCGYNI", rs1);
				
		assertEquals("2QZX,3ZKI,2QZW,1TZS,2G94,1FMX,3ZKG,3ZKQ,2EWY,3ZKS,3ZKM,3ZKN,3ZKX,1ENT," +
					 "3PBD,2IGX,2IGY,1QRP,1ZAP,3ZL7,2BKT,2BKS,1CMS,3PB5,4J0V,4J17,2V12,1PPM," +
					 "2V11,4J0Y,2V13,4J0T,2V10,1PPK,4Q1N,1PPL,3OOT,4J0P,4LP9,4AMT,3BRA,2V0Z," +
					 "2V16,4CMS,3ER3,3ER5,1FMU,2JXR,6APR,2BJU,4AA9,4AA8,3ZMG,4J1K,2VS2,4LAP," +
					 "4J1F,4J1E,2R9B,4J1C,1DP5,4J1I,4J1H,4K9H,1FQ7,1FQ6,1FQ5,4J0Z,1FQ4,3IXJ," +
					 "1FQ8,2IL2,4LBT,1AM5,1DPJ,3QVI,2IKO,3VYD,3VYE,3O9L,1GKT,3ZLQ,3VYF,4CKU," +
					 "1G0V,3Q3T,3QVC,3PRS,3VSW,4GJD,3ZOV,4GJC,3Q4B,4GJB,1HRN,4GJA,1YX9,1BIM," +
					 "1BIL,3SFC,1PSO,1ME6,4GJ9,3PEP,1PSN,4GJ8,4GJ7,3VSX,3UTL,3QRV,1PSA,1YG9," +
					 "3PSG,1UH7,3CMS,1UH9,4EXG,1UH8,2PSG,3PSY,1QDM,3Q5H,1SGZ,1XDH,3F9Q,1HTR," +
					 "4EWO,2I4Q,4RLD,2V00,3OQK,3PCW,2WED,3URL,3UFL,2JJI,3URJ,2WEA,3PCZ,1XE6," +
					 "2WEC,3URI,3OQF,2WEB,1XE5,1M43,2JJJ,1FLH,3Q70,1LS5,1QS8,3QS1,1MIQ,3VCM," +
					 "3Q6Y,1W6I,1W6H,1E82,1E81,1E80,1E5O,1IZD,1IZE,3VUC,3U6A,3PBZ,3PVK,2REN," +
					 "1B5F,4LHH,2APR,3LIZ,3PGI,4L6B,1EED,1WKR,2G27,2G26,2P4J,1GVT,4AUC,1GVU," +
					 "1GVV,2G1Y,1GVW,1GVX,2G20,2G1O,2RMP,2G1N,2G24,2G1S,4KUP,2G1R,2G22,2G21," +
					 "4BFB,4B78,4B77,4B72,2ER7,2ER9,4BFD,3T7X,4B70,4OD9,2ER6,1IBQ,3PWW,2ER0," +
					 "3OWN,4PYV,2VKM,3LZY,1XS7,5ER1,3G6Z,3OAD,1AVF,3FV3,3CIC,3CID,1SME,3CIB," +
					 "4OBZ,3C9X,1LF3,1LF4,4OC6,3PI0,3G72,5APR,3T7P,3K1W,3T7Q,3G70,4BEK,4BEL," +
					 "1J71,5ER2,1LF2,1LEE,3GW5,3FNS,3FNT,3FNU,4GJ6,4GJ5,1SMR,1PFZ,2ASI,3OAG," +
					 "3T6I,5PEP,4GID,4B1E,4APE,2FDP,4ER4,4B1C,4ER1,4B1D,4ER2,4APR,1OEW,1OEX," +
					 "1EAG,1BXQ,1BXO,1APW,1APV,1APU,1APT,2NR6,3PLL,2IKU,1F34,1ER8,3PLD,3BUH," +
					 "3BUG,1MPP,3BUF,2H6T,3TNE,2H6S,1EPP,1EPQ,1EPR,1EPL,1EPM,1EPN,1EPO,3EMY," +
					 "1BBS,1LYW,1XN2,2FS4,1LYB,1OD1,1LYA,1CZI,2ANL,1XN3,2X0B,4K8S,3KM4,3PM4," +
					 "3APP,4PEP,1RNE,3D91,4FS4,4B0Q,3PMY,3PMU,3APR", pdbIds);
		
		assertEquals("3PSY_A", results.getAlignment("3PSY_A").getPdbIdChain());
		
		assertEquals("1CZI_E,4AUC_A,1CMS_A,4CMS_A,4AA8_A,3CMS_A,4AA9_A,1QRP_E,1PSN_A,1PSO_E," +
					 "1FLH_A,3UTL_A,5PEP_A,1F34_A,1PSA_B,1YX9_A,3PEP_A,4PEP_A,3PSG_A,2PSG_A," +
					 "1TZS_A,1AM5_A,1HTR_B,1AVF_J,1SMR_E,3VCM_B,2G24_B,2G1N_A,2G20_A,2G1S_A," +
					 "2G1R_A,2G26_B,2G21_B,2G1Y_A,2FS4_A,2G27_A,2G22_B,2G1O_B,2I4Q_A,1BIL_A," +
					 "1BIM_B,3KM4_B,3GW5_A,1HRN_A,3VYD_A,3VUC_B,3VSX_B,4GJ8_B,3OOT_A,2BKS_A," +
					 "2V0Z_C,4GJA_A,3OQF_A,3Q5H_B,3Q4B_A,2REN_A,2V13_A,1BBS_A,4GJ5_A,2IL2_A," +
					 "3G72_A,2IKU_B,2V12_C,2V16_C,4GJD_B,3Q3T_A,4GJB_A,2BKT_A,4GJC_B,3OQK_A," +
					 "2V11_O,2IKO_A,4GJ9_B,3VYE_A,3VSW_A,4Q1N_B,4PYV_B,2V10_C,4GJ7_B,1RNE_A," +
					 "3VYF_A,3SFC_B,4GJ6_A,3OWN_B,3G70_B,3D91_B,3K1W_B,3G6Z_B,2X0B_C,4AMT_A," +
					 "1G0V_A,1FMU_A,1FMX_B,1DPJ_A,1DP5_A,1FQ4_A,1FQ8_A,1FQ6_A,1FQ5_A,2JXR_A," +
					 "1FQ7_A,1QDM_A,1LYW_B,1LYB_B,1LYA_B,4OD9_B,4OBZ_D,4OC6_B,1B5F_C,5APR_E," +
					 "3APR_E,2APR_A,6APR_E,4APR_E,1UH7_A,1UH8_A,1UH9_A,3QRV_A,3QS1_A,1XE5_A," +
					 "1XE6_B,2IGY_A,2IGX_A,1ME6_B,1SME_A,2R9B_A,1W6I_A,1XDH_B,1LF4_A,1W6H_A," +
					 "1LF3_A,1PFZ_A,2BJU_A,4CKU_E,3F9Q_A,1M43_A,1LF2_A,1LEE_A,2ANL_A,1QS8_A," +
					 "1MIQ_A,3LIZ_A,1LS5_B,2NR6_A,1YG9_A,4RLD_Entity,3OAD_A,3O9L_A,3OAG_A," +
					 "1MPP_A,2RMP_A,2ASI_A,3FV3_H,3TNE_A,3FNU_D,3FNS_B,3FNT_A,2H6T_A,2H6S_A," +
					 "3QVI_D,3QVC_A,2QZW_A,1ZAP_A,1EAG_A,3PVK_A,3Q70_A,1APT_E,2WEC_A,1BXO_A," +
					 "1APV_E,1PPK_E,3APP_A,1APU_E,1BXQ_A,2WEA_A,1APW_E,2WEB_A,2WED_A,1PPL_E," +
					 "1PPM_E,3EMY_A,3C9X_A,1J71_A,1IBQ_B,1GVU_A,3PB5_A,3PCZ_A,3PWW_A,3PRS_A," +
					 "2ER7_E,1GVW_A,4ER1_E,4APE_A,3Q6Y_A,1E82_E,1EPM_E,4LAP_Entity,1GVT_A," +
					 "5ER1_E,2V00_A,1EPP_E,3T7P_A,3LZY_A,1EPN_E,2ER9_E,4L6B_A,3PLD_A,2ER6_E," +
					 "3PGI_A,1GVX_A,3ER5_E,3T7Q_A,3PBZ_A,3PBD_A,4ER4_E,2JJJ_A,4KUP_A,3T6I_A," +
					 "1EPQ_E,1EPL_E,1E5O_E,1GKT_A,3PSY_A,2VS2_A,3URL_A,2ER0_E,4ER2_E,3PMY_A," +
					 "1GVV_A,3PCW_A,3PLL_A,3PMU_A,1EED_P,1EPO_E,1OEX_A,3PM4_A,5ER2_E,1E81_E," +
					 "4LP9_A,1EPR_E,1E80_E,3URJ_A,1ENT_E,1OEW_A,1OD1_A,3PI0_A,2JJI_A,3ER3_E," +
					 "3URI_A,1ER8_E,4LBT_A,3T7X_A,4LHH_A,1WKR_A,2QZX_A,4B1C_A,1IZE_A,1IZD_A," +
					 "2EWY_A,3ZKS_A,3ZKN_B,4BEL_B,3ZKM_B,4BFB_A,3ZKQ_A,3ZKX_A,3ZL7_A,3ZLQ_A," +
					 "3ZKI_B,3ZKG_A,4J1C_A,4J0Z_A,4J0P_A,4BFD_A,3ZMG_A,4J1F_A,4J1K_A,4J1E_A," +
					 "4J17_A,3BUG_A,4J0V_A,4J1H_A,3ZOV_A,3BUF_A,3BRA_A,4BEK_A,4J1I_A,4J0Y_A," +
					 "3BUH_A,4J0T_A,4B78_A,4B0Q_A,4B70_A,4EWO_A,4EXG_A,4K9H_A,4K8S_C,4GID_D," +
					 "3IXJ_C,4B1E_A,4B72_A,2FDP_A,4B1D_A,4B77_A,1XN3_C,2VKM_B,1XN2_C,2G94_C," +
					 "1XS7_D,1SGZ_D,2P4J_C,3UFL_A,3CIB_A,3CIC_B,4FS4_B,3U6A_A,3CID_A", pdbIdChains);
	}
	
}
