package uk.co.flax.biosolr.pdbe.fasta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import uk.ac.ebi.webservices.axis1.stubs.fasta.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.WsResultType;
import uk.co.flax.biosolr.pdbe.fasta.PDb;
import uk.co.flax.biosolr.pdbe.fasta.FastaJob;
import uk.co.flax.biosolr.pdbe.fasta.FastaJobResults;
import uk.co.flax.biosolr.pdbe.fasta.FastaStatus;

public class TestFastaJob {

	private static final String RESULT_PATH = "result2";
	
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
		
		Map<PDb.Id, Map<String, PDb.Alignment>> alignments = results.getAlignments();
    assertEquals(317, alignments.size());

		PDb.Alignment a0 = results.getAlignment("1CZI", "E");
		assertEquals("1CZI_E", a0.getPdbIdChain());

		assertEquals("GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLG" +
      					 "KPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFD" +
      					 "NMMNRHLVAQDLFSVYMDRNGQESMLTLGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGG" +
      					 "CQAILDTGTSKLVGPSSDILNIQQAIGATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQ" +
      					 "GFCTSGFQSENHSQKWILGDVFIREYYSVFDRANNLVGLAKAI", a0.getQuerySequenceString());
		
		assertEquals("GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLG" +
      					 "KPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFD" +
      					 "NMMNRHLVAQDLFSVYMDRNGQESMLTLGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGG" +
      					 "CQAILDTGTSKLVGPSSDILNIQQAIGATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQ" +
      					 "GFCTSGFQSENHSQKWILGDVFIREYYSVFDRANNLVGLAKAI", a0.getReturnSequenceString());
		
    PDb.Alignment a1 = results.getAlignment("3CID", "A");
		assertEquals("3CID_A", a1.getPdbIdChain());
		
		assertEquals("  GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWV---PSIYCKSNACKNHQRFDPRKSST" +
      					 "FQNLGKPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTY--AEFDGILGMAYPSLAS-" +
      					 "EYSI-PVFDNMMNRHLVAQDLFSVYMDRNG----QESMLT-------LGAIDPSYYTGSLHWVPVTVQQY" +
      					 "WQFTVDSVTISG--VVVACE--GGCQAILDTGTSKLVGPS----SDILNIQQAIGATQNQYGEF---DID" +
      					 "CDNLSYMPTVVF---------EINGKMYPLT--PSAY--------TSQDQGFCTSGFQSENHSQKWILGD" +
      					 "VFIREYYSVFDRANNLVGLAKAI                            ", a1.getQuerySequenceString());
		
		assertEquals("GSFVEMVDNLRGKSGQGYYVEMTVGSPPQTLNILVDTGSSNFAVGAAPHPFL-------HRYYQRQLSST" +
      					 "YRDLRKGVYVPYTQGKWEGELGTDLVSIPHGPNVTVRANIAAITESDKFFINGSNWEGILGLAYAEIARP" +
      					 "DDSLEPFFDSLVKQTHVP-NLFSLQLCGAGFPLNQSEVLASVGGSMIIGGIDHSLYTGSLWYTPIRREWY" +
      					 "YEVIIVRVEINGQDLKMDCKEYNYDKSIVDSGTTNLRLPKKVFEAAVKSIKAASSTEKFPDGFWLGEQLV" +
      					 "CWQAGTTPWNIFPVISLYLMGEVTNQSFRITILPQQYLRPVEDVATSQDD--CYK-FAISQSSTGTVMGA" +
      					 "VIMEGFYVVFDRARKRIGFAVSACHVHDEFRTAAVEGPFVTLDMEDCGYNI", a1.getReturnSequenceString());
				
		assertEquals("1CZI,4AUC,1CMS,4CMS,4AA8,3CMS,4AA9,1QRP,1PSN,1PSO," +
		             "1FLH,3UTL,5PEP,1F34,1PSA,1YX9,3PEP,4PEP,3PSG,2PSG," +
		             "1TZS,1AM5,1HTR,1AVF,1SMR,3VCM,2G24,2G1N,2G20,2G1S," +
		             "2G1R,2G26,2G21,2G1Y,2FS4,2G27,2G22,2G1O,2I4Q,1BIL," +
		             "1BIM,3KM4,3GW5,1HRN,3VYD,3VUC,3VSX,4GJ8,3OOT,2BKS," +
                 "2V0Z,4GJA,3OQF,3Q5H,3Q4B,2REN,2V13,1BBS,4GJ5,2IL2," +
                 "3G72,2IKU,2V12,2V16,4GJD,3Q3T,4GJB,2BKT,4GJC,3OQK," +
                 "2V11,2IKO,4GJ9,3VYE,3VSW,4Q1N,4PYV,2V10,4GJ7,1RNE," +
                 "3VYF,3SFC,4GJ6,3OWN,3G70,3D91,3K1W,3G6Z,2X0B,4AMT," +
                 "1G0V,1FMU,1FMX,1DPJ,1DP5,1FQ4,1FQ8,1FQ6,1FQ5,2JXR," +
                 "1FQ7,1QDM,1LYW,1LYB,1LYA,4OD9,4OBZ,4OC6,1B5F,5APR," +
                 "3APR,2APR,6APR,4APR,1UH7,1UH8,1UH9,3QRV,3QS1,1XE5," +
                 "1XE6,2IGY,2IGX,1ME6,1SME,2R9B,1W6I,1XDH,1LF4,1W6H," +
                 "1LF3,1PFZ,2BJU,4CKU,3F9Q,1M43,1LF2,1LEE,2ANL,1QS8," +
                 "1MIQ,3LIZ,1LS5,2NR6,1YG9,4RLD,3OAD,3O9L,3OAG,1MPP," +
                 "2RMP,2ASI,3FV3,3TNE,3FNU,3FNS,3FNT,2H6T,2H6S,3QVI," +
                 "3QVC,2QZW,1ZAP,1EAG,3PVK,3Q70,1APT,2WEC,1BXO,1APV," +
                 "1PPK,3APP,1APU,1BXQ,2WEA,1APW,2WEB,2WED,1PPL,1PPM," +
                 "3EMY,3C9X,1J71,1IBQ,1GVU,3PB5,3PCZ,3PWW,3PRS,2ER7," +
                 "1GVW,4ER1,4APE,3Q6Y,1E82,1EPM,4LAP,1GVT,5ER1,2V00," +
                 "1EPP,3T7P,3LZY,1EPN,2ER9,4L6B,3PLD,2ER6,3PGI,1GVX," +
                 "3ER5,3T7Q,3PBZ,3PBD,4ER4,2JJJ,4KUP,3T6I,1EPQ,1EPL," +
                 "1E5O,1GKT,3PSY,2VS2,3URL,2ER0,4ER2,3PMY,1GVV,3PCW," +
                 "3PLL,3PMU,1EED,1EPO,1OEX,3PM4,5ER2,1E81,4LP9,1EPR," +
                 "1E80,3URJ,1ENT,1OEW,1OD1,3PI0,2JJI,3ER3,3URI,1ER8," +
                 "4LBT,3T7X,4LHH,1WKR,2QZX,4B1C,1IZE,1IZD,2EWY,3ZKS," +
                 "3ZKN,4BEL,3ZKM,4BFB,3ZKQ,3ZKX,3ZL7,3ZLQ,3ZKI,3ZKG," +
                 "4J1C,4J0Z,4J0P,4BFD,3ZMG,4J1F,4J1K,4J1E,4J17,3BUG," +
                 "4J0V,4J1H,3ZOV,3BUF,3BRA,4BEK,4J1I,4J0Y,3BUH,4J0T," +
                 "4B78,4B0Q,4B70,4EWO,4EXG,4K9H,4K8S,4GID,3IXJ,4B1E," +
                 "4B72,2FDP,4B1D,4B77,1XN3,2VKM,1XN2,2G94,1XS7,1SGZ," +
                 "2P4J,3UFL,3CIB,3CIC,4FS4,3U6A,3CID", StringUtils.join(alignments.keySet(), ','));

		List<String> pdbIdChains = new ArrayList<>();
		for (Map<String, PDb.Alignment> aMap : alignments.values()) {
		  for (PDb.Alignment a : aMap.values()) {
		    pdbIdChains.add(a.getPdbIdChain());
		  }
		}
		assertEquals("1CZI_E,4AUC_A,1CMS_A,4CMS_A,4AA8_A,3CMS_A,4AA9_A,1QRP_E,1PSN_A,1PSO_E," +
		             "1FLH_A,3UTL_A,5PEP_A,1F34_A,1PSA_B,1PSA_A,1YX9_A,3PEP_A,4PEP_A,3PSG_A," +
                 "2PSG_A,1TZS_A,1AM5_A,1HTR_B,1AVF_J,1AVF_A,1SMR_E,1SMR_A,1SMR_G,1SMR_C," +
                 "3VCM_B,3VCM_A,2G24_B,2G24_A,2G1N_A,2G1N_B,2G20_A,2G20_B,2G1S_A,2G1S_B," +
                 "2G1R_A,2G1R_B,2G26_B,2G26_A,2G21_B,2G21_A,2G1Y_A,2G1Y_B,2FS4_A,2FS4_B," +
                 "2G27_A,2G27_B,2G22_B,2G22_A,2G1O_B,2G1O_A,2I4Q_A,2I4Q_B,1BIL_A,1BIL_B," +
                 "1BIM_B,1BIM_A,3KM4_B,3KM4_A,3GW5_A,3GW5_B,1HRN_A,1HRN_B,3VYD_A,3VYD_B," +
                 "3VUC_B,3VUC_A,3VSX_B,3VSX_A,4GJ8_B,4GJ8_A,3OOT_A,3OOT_B,2BKS_A,2BKS_B," +
                 "2V0Z_C,2V0Z_O,4GJA_A,4GJA_B,3OQF_A,3OQF_B,3Q5H_B,3Q5H_A,3Q4B_A,3Q4B_B," +
                 "2REN_A,2V13_A,1BBS_A,1BBS_B,4GJ5_A,4GJ5_B,2IL2_A,2IL2_B,3G72_A,3G72_B," +
                 "2IKU_B,2IKU_A,2V12_C,2V12_O,2V16_C,2V16_O,4GJD_B,4GJD_A,3Q3T_A,3Q3T_B," +
                 "4GJB_A,4GJB_B,2BKT_A,2BKT_B,4GJC_B,4GJC_A,3OQK_A,3OQK_B,2V11_O,2V11_C," +
                 "2IKO_A,2IKO_B,4GJ9_B,4GJ9_A,3VYE_A,3VYE_B,3VSW_A,3VSW_B,4Q1N_B,4Q1N_A," +
                 "4PYV_B,4PYV_A,2V10_C,2V10_O,4GJ7_B,4GJ7_A,1RNE_A,3VYF_A,3VYF_B,3SFC_B," +
                 "3SFC_A,4GJ6_A,4GJ6_B,3OWN_B,3OWN_A,3G70_B,3G70_A,3D91_B,3D91_A,3K1W_B," +
                 "3K1W_A,3G6Z_B,3G6Z_A,2X0B_C,2X0B_G,2X0B_E,2X0B_A,4AMT_A,1G0V_A,1FMU_A," +
                 "1FMX_B,1FMX_A,1DPJ_A,1DP5_A,1FQ4_A,1FQ8_A,1FQ6_A,1FQ5_A,2JXR_A,1FQ7_A," +
                 "1QDM_A,1QDM_C,1QDM_B,1LYW_B,1LYW_D,1LYW_F,1LYW_H,1LYW_A,1LYW_E,1LYW_G," +
                 "1LYW_C,1LYB_B,1LYB_D,1LYB_A,1LYB_C,1LYA_B,1LYA_D,1LYA_C,1LYA_A,4OD9_B," +
                 "4OD9_D,4OD9_A,4OD9_C,4OBZ_D,4OBZ_B,4OBZ_C,4OBZ_A,4OC6_B,4OC6_A,1B5F_C," +
                 "1B5F_A,5APR_E,3APR_E,2APR_A,6APR_E,4APR_E,1UH7_A,1UH8_A,1UH9_A,3QRV_A," +
                 "3QRV_B,3QS1_A,3QS1_D,3QS1_B,3QS1_C,1XE5_A,1XE5_B,1XE6_B,1XE6_A,2IGY_A," +
                 "2IGY_B,2IGX_A,1ME6_B,1ME6_A,1SME_A,1SME_B,2R9B_A,2R9B_B,1W6I_A,1W6I_C," +
                 "1XDH_B,1XDH_A,1LF4_A,1W6H_A,1W6H_B,1LF3_A,1PFZ_A,1PFZ_C,1PFZ_D,1PFZ_B," +
                 "2BJU_A,4CKU_E,4CKU_D,4CKU_F,4CKU_B,4CKU_A,4CKU_C,3F9Q_A,1M43_A,1M43_B," +
                 "1LF2_A,1LEE_A,2ANL_A,2ANL_B,1QS8_A,1QS8_B,1MIQ_A,1MIQ_B,3LIZ_A,1LS5_B," +
                 "1LS5_A,2NR6_A,2NR6_B,1YG9_A,4RLD_Entity,3OAD_A,3OAD_C,3OAD_D,3OAD_B,3O9L_A," +
                 "3O9L_C,3O9L_D,3O9L_B,3OAG_A,3OAG_C,3OAG_D,3OAG_B,1MPP_A,2RMP_A,2ASI_A," +
                 "3FV3_H,3FV3_C,3FV3_G,3FV3_D,3FV3_A,3FV3_E,3FV3_B,3FV3_F,3TNE_A,3TNE_B," +
                 "3FNU_D,3FNU_A,3FNU_B,3FNU_C,3FNS_B,3FNS_A,3FNT_A,2H6T_A,2H6S_A,3QVI_D," +
                 "3QVI_B,3QVI_A,3QVI_C,3QVC_A,2QZW_A,2QZW_B,1ZAP_A,1EAG_A,3PVK_A,3Q70_A," +
                 "1APT_E,2WEC_A,1BXO_A,1APV_E,1PPK_E,3APP_A,1APU_E,1BXQ_A,2WEA_A,1APW_E," +
                 "2WEB_A,2WED_A,1PPL_E,1PPM_E,3EMY_A,3C9X_A,1J71_A,1IBQ_B,1IBQ_A,1GVU_A," +
                 "3PB5_A,3PCZ_A,3PWW_A,3PRS_A,2ER7_E,1GVW_A,4ER1_E,4APE_A,3Q6Y_A,1E82_E," +
                 "1EPM_E,4LAP_Entity,1GVT_A,5ER1_E,2V00_A,1EPP_E,3T7P_A,3LZY_A,1EPN_E,2ER9_E," +
                 "4L6B_A,3PLD_A,2ER6_E,3PGI_A,1GVX_A,3ER5_E,3T7Q_A,3PBZ_A,3PBD_A,4ER4_E," +
                 "2JJJ_A,4KUP_A,3T6I_A,1EPQ_E,1EPL_E,1E5O_E,1GKT_A,3PSY_A,2VS2_A,3URL_A," +
                 "2ER0_E,4ER2_E,3PMY_A,1GVV_A,3PCW_A,3PLL_A,3PMU_A,1EED_P,1EPO_E,1OEX_A," +
                 "3PM4_A,5ER2_E,1E81_E,4LP9_A,1EPR_E,1E80_E,3URJ_A,1ENT_E,1OEW_A,1OD1_A," +
                 "3PI0_A,2JJI_A,3ER3_E,3URI_A,1ER8_E,4LBT_A,3T7X_A,4LHH_A,1WKR_A,2QZX_A," +
                 "2QZX_B,4B1C_A,1IZE_A,1IZD_A,2EWY_A,2EWY_C,2EWY_B,2EWY_D,3ZKS_A,3ZKN_B," +
                 "3ZKN_A,4BEL_B,4BEL_A,3ZKM_B,3ZKM_A,4BFB_A,4BFB_B,3ZKQ_A,3ZKX_A,3ZL7_A," +
                 "3ZLQ_A,3ZLQ_B,3ZKI_B,3ZKI_A,3ZKG_A,3ZKG_B,4J1C_A,4J0Z_A,4J0P_A,4BFD_A," +
                 "3ZMG_A,4J1F_A,4J1K_A,4J1E_A,4J17_A,3BUG_A,4J0V_A,4J1H_A,3ZOV_A,3BUF_A," +
                 "3BRA_A,4BEK_A,4J1I_A,4J0Y_A,3BUH_A,4J0T_A,4B78_A,4B0Q_A,4B70_A,4EWO_A," +
                 "4EXG_A,4K9H_A,4K9H_C,4K9H_B,4K8S_C,4K8S_A,4K8S_B,4GID_D,4GID_B,4GID_A," +
                 "4GID_C,3IXJ_C,3IXJ_B,3IXJ_A,4B1E_A,4B72_A,2FDP_A,2FDP_B,2FDP_C,4B1D_A," +
                 "4B77_A,1XN3_C,1XN3_D,1XN3_B,1XN3_A,2VKM_B,2VKM_D,2VKM_C,2VKM_A,1XN2_C," +
                 "1XN2_D,1XN2_A,1XN2_B,2G94_C,2G94_A,2G94_B,2G94_D,1XS7_D,1SGZ_D,1SGZ_C," +
                 "1SGZ_A,1SGZ_B,2P4J_C,2P4J_B,2P4J_D,2P4J_A,3UFL_A,3CIB_A,3CIB_B,3CIC_B," +
                 "3CIC_A,4FS4_B,3U6A_A,3CID_A", StringUtils.join(pdbIdChains, ','));
	}
	
}
