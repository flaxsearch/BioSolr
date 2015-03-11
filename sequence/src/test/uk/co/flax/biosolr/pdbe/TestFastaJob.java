package uk.co.flax.biosolr.pdbe;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
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
		
		assertEquals(1000, results.getNumChains());
		assertEquals(317, results.getNumEntries());
		
		List<String> order = results.getResultOrder();
		Map<String, Alignment> alignments = results.getAlignments();

		String pdbIdChain = order.get(0);
		String sequence = alignments.get(pdbIdChain).getReturnSequenceString();
		assertEquals("1CZI_E", pdbIdChain);
		
		assertEquals("GEVASVPLTNYLDSQYFGKIYLGTPPQEFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLG" +
					 "KPLSIHYGTGSMQGILGYDTVTVSNIVDIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFD" +
					 "NMMNRHLVAQDLFSVYMDRNGQESMLTLGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGG" +
					 "CQAILDTGTSKLVGPSSDILNIQQAIGATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQ" +
					 "GFCTSGFQSENHSQKWILGDVFIREYYSVFDRANNLVGLAKAIGEVASVPLTNYLDSQYFGKIYLGTPPQ" +
					 "EFTVLFDTGSSDFWVPSIYCKSNACKNHQRFDPRKSSTFQNLGKPLSIHYGTGSMQGILGYDTVTVSNIV" +
					 "DIQQTVGLSTQEPGDVFTYAEFDGILGMAYPSLASEYSIPVFDNMMNRHLVAQDLFSVYMDRNGQESMLT" +
					 "LGAIDPSYYTGSLHWVPVTVQQYWQFTVDSVTISGVVVACEGGCQAILDTGTSKLVGPSSDILNIQQAIG" +
					 "ATQNQYGEFDIDCDNLSYMPTVVFEINGKMYPLTPSAYTSQDQGFCTSGFQSENHSQKWILGDVFIREYY" +
					 "SVFDRANNLVGLAKAI", sequence);
		
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
					 "3APP,4PEP,1RNE,3D91,4FS4,4B0Q,3PMY,3PMU,3APR", results.getPdbIdCodes());
		
		String idChains = StringUtils.join(results.getResultOrder(), ',');
		assertEquals("1CZI_E,1CZI_E,4AUC_A,1CMS_A,4CMS_A,4CMS_A,1CMS_A,4AUC_A,4AA8_A,4AA8_A," +
					 "3CMS_A,3CMS_A,4AA9_A,4AA9_A,1QRP_E,1PSN_A,1PSO_E,1PSO_E,1FLH_A,1PSN_A," +
					 "1FLH_A,3UTL_A,1QRP_E,3UTL_A,5PEP_A,5PEP_A,1F34_A,1PSA_B,1PSA_B,1YX9_A," +
					 "1YX9_A,1F34_A,1PSA_A,1PSA_A,3PEP_A,4PEP_A,4PEP_A,3PEP_A,3PSG_A,3PSG_A," +
					 "2PSG_A,2PSG_A,1TZS_A,1TZS_A,1AM5_A,1AM5_A,1HTR_B,1AVF_J,1AVF_A,1HTR_B," +
					 "1AVF_J,1AVF_A,1SMR_E,1SMR_A,1SMR_G,1SMR_C,1SMR_E,1SMR_C,1SMR_G,1SMR_A," +
					 "3VCM_B,3VCM_A,3VCM_A,3VCM_B,2G24_B,2G1N_A,2G20_A,2G1S_A,2G1R_A,2G26_B," +
					 "2G21_B,2G26_B,2G1Y_A,2G1S_A,2FS4_A,2G1N_A,2G1S_B,2G21_A,2FS4_B,2G20_A," +
					 "2G27_A,2G1Y_A,2G1S_B,2G1N_B,2G21_A,2FS4_A,2FS4_B,2G22_B,2G20_B,2G1O_B," +
					 "2G24_A,2G1Y_B,2G1R_B,2G22_B,2G1O_A,2G27_B,2G27_A,2G22_A,2G26_A,2G1O_B," +
					 "2G24_A,2G27_B,2G1N_B,2G1R_B,2G1Y_B,2G21_B,2G26_A,2G22_A,2G1R_A,2G20_B," +
					 "2G24_B,2G1O_A,2I4Q_A,2I4Q_A,2I4Q_B,2I4Q_B,1BIL_A,1BIM_B,3KM4_B,3GW5_A," +
					 "3KM4_B,3KM4_A,1HRN_A,1HRN_B,1BIL_B,3GW5_B,1BIL_B,1BIL_A,1HRN_B,1BIM_B," +
					 "3GW5_A,3KM4_A,1BIM_A,1BIM_A,1HRN_A,3GW5_B,3VYD_A,3VUC_B,3VSX_B,4GJ8_B," +
					 "3OOT_A,3VSX_A,2BKS_A,2V0Z_C,2V0Z_C,4GJA_A,3OQF_A,3Q5H_B,3Q4B_A,2BKS_A," +
					 "2REN_A,3VYD_B,2V13_A,1BBS_A,4GJ5_A,3OOT_B,2IL2_A,3OQF_B,3G72_A,2IKU_B," +
					 "2IKU_B,2V12_C,3Q4B_B,2V16_C,4GJD_B,3Q3T_A,4GJB_A,2BKT_A,4GJC_B,3OQK_A," +
					 "2V11_O,4GJA_B,2IKO_A,2IL2_A,4GJB_B,2V16_O,2BKT_A,4GJ5_B,3OOT_B,4GJ9_B," +
					 "2V11_C,4GJD_B,3VYD_A,2V13_A,3G72_B,4GJD_A,4GJB_A,2IKU_A,3VYE_A,3VSW_A," +
					 "3Q4B_A,4Q1N_B,2IKO_A,3VSW_B,2IKO_B,4PYV_B,3OQK_B,3Q3T_B,3OQK_B,2BKS_B," +
					 "3VUC_A,2IL2_B,1BBS_B,2REN_A,4PYV_A,4GJ9_A,2V16_C,3OQF_B,2V10_C,4GJA_A," +
					 "4GJA_B,3OOT_A,1BBS_A,3VYE_A,4GJ9_B,4GJC_B,2BKS_B,3VYE_B,4GJ7_B,4GJC_A," +
					 "2IKU_A,1RNE_A,3Q5H_A,3VYF_A,3VUC_A,3SFC_B,4GJ6_A,4GJ5_A,4GJ8_B,4GJ9_A," +
					 "2V12_O,2V0Z_O,4GJ8_A,4Q1N_A,3SFC_B,3OQF_A,4GJB_B,3VYE_B,3G72_A,2IKO_B," +
					 "4GJD_A,3VYD_B,4PYV_A,2V0Z_O,4GJ7_A,3Q3T_A,2V10_C,4GJC_A,2V16_O,4Q1N_A," +
					 "3SFC_A,3VYF_B,4GJ7_A,2IL2_B,3Q5H_A,3VSX_A,2V12_O,4GJ5_B,1BBS_B,4PYV_B," +
					 "3VYF_A,3OQK_A,3VSW_A,2V11_C,4GJ6_B,3Q3T_B,2BKT_B,1RNE_A,3VUC_B,2V10_O," +
					 "4GJ7_B,3SFC_A,3VSX_B,2V11_O,3Q5H_B,3Q4B_B,2V12_C,2BKT_B,2V10_O,3G72_B," +
					 "3VSW_B,4GJ6_B,4GJ6_A,4Q1N_B,4GJ8_A,3VYF_B,3OWN_B,3G70_B,3OWN_A,3OWN_A," +
					 "3G70_A,3D91_B,3K1W_B,3G70_B,3D91_B,3D91_A,3G70_A,3G6Z_B,3G6Z_B,3K1W_A," +
					 "3G6Z_A,3K1W_A,3K1W_B,3OWN_B,3G6Z_A,3D91_A,2X0B_C,2X0B_G,2X0B_E,2X0B_E," +
					 "4AMT_A,2X0B_A,2X0B_C,4AMT_A,2X0B_A,2X0B_G,1G0V_A,1G0V_A,1FMU_A,1FMX_B," +
					 "1DPJ_A,1FMX_A,1FMU_A,1DP5_A,1FMX_A,1DP5_A,1FMX_B,1DPJ_A,1FQ4_A,1FQ8_A," +
					 "1FQ6_A,1FQ5_A,2JXR_A,2JXR_A,1FQ6_A,1FQ8_A,1FQ7_A,1FQ5_A,1FQ4_A,1FQ7_A," +
					 "1QDM_A,1QDM_C,1QDM_A,1QDM_B,1QDM_C,1QDM_B,1LYW_B,1LYB_B,1LYB_D,1LYW_D," +
					 "1LYB_D,1LYA_B,1LYA_B,1LYA_D,1LYW_B,1LYW_F,1LYW_F,1LYW_H,1LYW_D,1LYW_H," +
					 "1LYA_D,1LYB_B,4OD9_B,4OBZ_D,4OD9_D,4OD9_D,4OBZ_B,4OD9_B,4OC6_B,4OBZ_B," +
					 "4OC6_B,4OBZ_D,1B5F_C,1B5F_C,1B5F_A,1B5F_A,5APR_E,3APR_E,2APR_A,2APR_A," +
					 "3APR_E,5APR_E,6APR_E,6APR_E,4APR_E,4APR_E,1UH7_A,1UH7_A,1UH8_A,1UH9_A," +
					 "1UH9_A,1UH8_A,3QRV_A,3QS1_A,3QS1_D,3QRV_B,3QS1_B,3QS1_C,3QS1_B,3QS1_D," +
					 "3QS1_A,3QRV_B,3QS1_C,3QRV_A,1XE5_A,1XE6_B,2IGY_A,2IGX_A,1XE5_A,1ME6_B," +
					 "2IGX_A,1ME6_A,1XE6_A,1SME_A,2R9B_A,2R9B_B,1XE6_A,1SME_B,1XE6_B,2IGY_A," +
					 "1ME6_A,2IGY_B,2IGY_B,2R9B_B,2R9B_A,1XE5_B,1SME_A,1XE5_B,1SME_B,1ME6_B," +
					 "1W6I_A,1XDH_B,1W6I_A,1LF4_A,1W6I_C,1W6H_A,1W6H_B,1W6H_B,1LF3_A,1W6H_A," +
			 		 "1XDH_B,1LF4_A,1LF3_A,1XDH_A,1XDH_A,1W6I_C,1PFZ_A,1PFZ_C,1PFZ_D,1PFZ_B," +
					 "1PFZ_C,1PFZ_A,1PFZ_D,1PFZ_B,2BJU_A,2BJU_A,4CKU_E,4CKU_D,4CKU_F,4CKU_B," +
			 		 "4CKU_D,4CKU_B,3F9Q_A,4CKU_A,3F9Q_A,4CKU_E,4CKU_C,4CKU_C,4CKU_F,4CKU_A," +
					 "1M43_A,1M43_B,1LF2_A,1LEE_A,1LF2_A,1LEE_A,1M43_A,1M43_B,2ANL_A,2ANL_B," +
			 		 "2ANL_A,2ANL_B,1QS8_A,1QS8_A,1QS8_B,1QS8_B,1MIQ_A,1MIQ_B,1MIQ_B,1MIQ_A," +
					 "3LIZ_A,3LIZ_A,1LS5_B,1LS5_A,1LS5_A,1LS5_B,2NR6_A,1YG9_A,4RLD_Entity,1YG9_A," +
			 		 "2NR6_B,2NR6_A,2NR6_B,3OAD_A,3OAD_A,3O9L_A,3OAG_A,3OAG_A,3OAD_C,3O9L_C," +
					 "3OAG_C,3O9L_C,3OAG_C,3O9L_A,3OAD_C,1MPP_A,1MPP_A,2RMP_A,2ASI_A,2RMP_A," +
			 		 "2ASI_A,3FV3_H,3FV3_C,3FV3_G,3FV3_D,3FV3_A,3FV3_E,3FV3_B,3FV3_F,3FV3_H," +
					 "3TNE_A,3FV3_D,3FV3_E,3FV3_F,3FV3_B,3FV3_G,3FV3_C,3FV3_A,3TNE_A,3TNE_B," +
			 		 "3TNE_B,3FNU_D,3FNU_D,3FNS_B,3FNU_A,3FNU_A,3FNT_A,3FNS_A,3FNU_B,3FNU_B," +
					 "3FNT_A,3FNS_B,3FNS_A,3FNU_C,3FNU_C,2H6T_A,2H6T_A,2H6S_A,2H6S_A,3QVI_D," +
			 		 "3QVC_A,3QVC_A,3QVI_B,3QVI_A,3QVI_C,3QVI_C,3QVI_B,3QVI_A,3QVI_D,2QZW_A," +
					 "2QZW_B,2QZW_B,2QZW_A,1ZAP_A,1ZAP_A,1EAG_A,3PVK_A,3Q70_A,3PVK_A,3Q70_A," +
			 		 "1EAG_A,1APT_E,2WEC_A,1BXO_A,1APV_E,1PPK_E,3APP_A,1APU_E,1BXQ_A,2WEA_A," +
					 "1APW_E,3APP_A,2WEB_A,2WED_A,1APT_E,1PPL_E,1APW_E,1APU_E,2WEB_A,2WED_A," +
			 		 "1PPM_E,1APV_E,2WEC_A,1PPK_E,1PPM_E,1BXQ_A,1BXO_A,2WEA_A,1PPL_E,3EMY_A," +
					 "3EMY_A,3C9X_A,3C9X_A,1J71_A,1J71_A,1IBQ_B,1IBQ_A,1IBQ_B,1IBQ_A,1GVU_A," +
			 		 "3PB5_A,3PCZ_A,3PWW_A,3PRS_A,2ER7_E,1GVW_A,4ER1_E,4APE_A,3Q6Y_A,1E82_E," +
					 "1EPM_E,4LAP_Entity,1GVT_A,5ER1_E,2V00_A,1EPP_E,3T7P_A,3LZY_A,1EPN_E,2ER9_E," +
			 		 "4L6B_A,3PLD_A,4ER1_E,2V00_A,3PCZ_A,2ER6_E,3PGI_A,1GVX_A,3ER5_E,3T7Q_A," +
					 "3PBZ_A,3PBD_A,4ER4_E,2JJJ_A,4KUP_A,3T6I_A,1EPQ_E,5ER1_E,1EPL_E,1EPM_E," +
			 		 "1EPN_E,1EPL_E,2ER9_E,1GVX_A,1E5O_E,1GKT_A,3PSY_A,2VS2_A,3PRS_A,3URL_A," +
					 "2ER0_E,4ER2_E,3PMY_A,1GVV_A,3PCW_A,3PLL_A,3PMU_A,1EED_P,1GKT_A,1EPO_E," +
			 		 "1OEX_A,3URL_A,3PM4_A,5ER2_E,1E81_E,2ER6_E,3T7Q_A,4LP9_A,1EPO_E,3LZY_A," +
					 "1E82_E,3PCW_A,4ER4_E,1EPR_E,2VS2_A,1E80_E,3URJ_A,1GVW_A,3PB5_A,1GVT_A," +
			 		 "1ENT_E,3PLL_A,1OEW_A,3PMU_A,1OD1_A,1OEW_A,3PI0_A,1GVU_A,3PBZ_A,1EED_P," +
					 "3PSY_A,2ER7_E,4ER2_E,1EPP_E,2JJI_A,3T6I_A,3ER3_E,3PMY_A,3URI_A,3PGI_A," +
					 "3T7P_A,1ER8_E,3PM4_A,4LBT_A,3T7X_A,1OD1_A,2JJJ_A,1E80_E,4LBT_A,1EPQ_E," +
					 "1EPR_E,4LP9_A,3Q6Y_A,5ER2_E,3URI_A,4L6B_A,4APE_A,1E81_E,3T7X_A,4LHH_A," +
					 "3ER5_E,3PBD_A,3ER3_E,3URJ_A,3PWW_A,1ER8_E,2JJI_A,2ER0_E,1OEX_A,4LHH_A," +
					 "3PLD_A,1GVV_A,3PI0_A,4KUP_A,1E5O_E,1ENT_E,1WKR_A,1WKR_A,2QZX_A,2QZX_A," +
					 "2QZX_B,2QZX_B,1LYA_C,1LYB_A,1LYW_A,1LYB_C,1LYW_E,1LYW_G,1LYW_A,1LYA_C," +
					 "1LYB_A,1LYA_A,1LYB_C,1LYW_C,1LYW_G,1LYA_A,1LYW_E,1LYW_C,4OD9_A,4OBZ_C," +
					 "4OC6_A,4OD9_A,4OBZ_A,4OBZ_C,4OD9_C,4OD9_C,4OBZ_A,4OC6_A,3OAD_D,3O9L_D," +
					 "3O9L_B,3OAG_D,3OAG_B,3OAD_B,3OAD_B,3OAD_D,3OAG_D,3OAG_B,3O9L_D,3O9L_B," +
					 "4B1C_A,4B1C_A,1IZE_A,1IZD_A,1IZD_A,1IZE_A,2EWY_A,2EWY_C,2EWY_B,2EWY_C," +
					 "2EWY_D,2EWY_D,2EWY_A,2EWY_B,3ZKS_A,3ZKN_B,4BEL_B,4BEL_A,3ZKM_B,4BFB_A," +
					 "4BEL_B,3ZKM_B,3ZKQ_A,4BFB_B,4BEL_A,4BFB_B,3ZKM_A,4BFB_A,3ZKS_A,3ZKN_A," +
					 "3ZKN_A,3ZKM_A,3ZKQ_A,3ZKN_B,3ZKX_A,3ZL7_A,3ZLQ_A,3ZKI_B,3ZLQ_B,3ZKG_A," +
					 "3ZKI_B,3ZLQ_A,3ZL7_A,3ZKG_B,3ZKX_A,3ZKG_A,3ZKI_A,3ZKG_B,3ZKI_A,3ZLQ_B," +
					 "4J1C_A,4J0Z_A,4J0P_A,4BFD_A,3ZMG_A,4J1F_A,4J1K_A,4J1E_A,4J17_A,3BUG_A," +
					 "4J0P_A,4J0Z_A,4J0V_A,4J0V_A,4J1H_A,3ZOV_A,4J1C_A,3BUF_A,3BUF_A,4J1F_A," +
					 "3BRA_A,4BEK_A,3BRA_A,3BUG_A,4J1H_A,4J1I_A,4J0Y_A,4BEK_A,4BFD_A,3ZMG_A," +
					 "3BUH_A,3BUH_A,4J0Y_A,4J0T_A,3ZOV_A,4J0T_A,4J17_A,4J1I_A,4J1E_A,4J1K_A," +
					 "4B78_A,4B78_A,4B0Q_A,4B0Q_A,4B70_A,4B70_A,4EWO_A,4EXG_A,4EXG_A,4EWO_A," +
					 "4K9H_A,4K8S_C,4K8S_A,4GID_D,3IXJ_C,4K8S_A,4B1E_A,4B72_A,4K8S_C,4GID_B," +
					 "4K8S_B,2FDP_A,4K9H_A,4GID_A,4K9H_C,2FDP_B,4B1D_A,4B1E_A,4GID_B,4K9H_B," +
					 "4GID_D,4B77_A,4B77_A,4B1D_A,3IXJ_B,4K9H_C,4K8S_B,3IXJ_A,4GID_C,4GID_C," +
					 "2FDP_C,4B72_A,2FDP_A,4K9H_B,4GID_A,3IXJ_C,2FDP_C,3IXJ_B,2FDP_B,3IXJ_A," +
					 "1XN3_C,2VKM_B,1XN2_C,2G94_C,1XS7_D,1SGZ_D,1XN2_D,2VKM_B,2P4J_C,2G94_A," +
					 "1SGZ_C,2VKM_D,1XN3_D,2G94_B,1XN2_D,3UFL_A,1XN2_C,1SGZ_A,1XS7_D,1XN3_B," +
					 "2VKM_C,1SGZ_D,1XN2_A,1SGZ_B,1XN3_C,2G94_C,2P4J_B,2G94_B,3UFL_A,2P4J_B," +
					 "1XN3_A,2VKM_A,2VKM_D,2P4J_D,2VKM_A,2P4J_D,1XN3_D,1SGZ_B,2P4J_A,2G94_A," +
					 "1SGZ_A,2G94_D,2G94_D,1XN3_B,2P4J_C,1XN2_B,1XN3_A,1SGZ_C,2P4J_A,1XN2_A," +
					 "2VKM_C,1XN2_B,3CIB_A,3CIC_B,4FS4_B,3U6A_A,3CIC_A,3CID_A,3CIB_B,3CID_A", idChains);

		assertEquals("3PSY_A", results.getAlignment("3PSY_A").getPdbIdChain());
		assertEquals("3psy_1", results.getAlignment("3PSY_A").getEntryEntity());
		
		assertEquals("3zki_2,1xn3_3,1apw_5,3pcz_1,1gvu_1,4rld_entity,1apv_5,2anl_1,6apr_5," +
					 "4bfd_1,2jjj_1,3vuc_1,1apt_5,3vuc_2,2psg_1,4apr_5,2jxr_1,1apu_5,5pep_1," +
					 "3emy_1,1miq_1,1miq_2,1ibq_2,2p4j_3,3oqf_1,4gjb_1,1ppk_5,3oqf_2,3q6y_1," +
					 "4pep_1,3zl7_1,2g1r_1,4j0v_1,4j17_1,2g1r_2,1ppl_5,4j1c_1,3pll_1,1am5_1," +
					 "2p4j_2,4gja_1,2nr6_1,1fq8_1,2g1n_1,3k1w_2,3k1w_1,1fmx_1,4k8s_1,1fmu_1," +
					 "3zks_1,1fmx_2,4k8s_3,3pcw_1,1oew_1,1ppm_5,4gj9_2,3pld_1,3vcm_1,3pgi_1," +
					 "4j1e_1,4ewo_1,4cku_5,3lzy_1,4cku_4,2bju_1,1bxo_1,4b77_1,4aa8_1,4gj8_2," +
					 "3oag_1,3oag_4,2qzx_1,1sgz_4,3vcm_2,3pmy_1,2g27_1,4gj7_2,4gj7_1,3q5h_1," +
					 "3pmu_1,3q70_1,2igy_1,3zkg_1,3vsw_1,3q5h_2,2ren_1,3apr_5,1fq4_1,2v12_3," +
					 "3uri_1,3cic_2,4k9h_1,2fs4_1,1lf4_1,4pyv_2,1eag_1,3t7p_1,1fq6_1,2v12_15," +
					 "3fv3_8,4pyv_1,2v16_3,4gjd_2,1lf2_1,1fq5_1,3ufl_1,3url_1,3tne_1,4od9_4," +
					 "4od9_2,4od9_1,4j1k_1,1psn_1,3fns_2,1tzs_1,4q1n_2,1psa_2,4q1n_1,3psy_1," +
					 "2ewy_3,3bra_1,3f9q_1,2jji_1,2ewy_1,2g1y_1,1czi_5,1lyw_1,1lyw_2,4exg_1," +
					 "4auc_1,1w6h_1,4b72_1,1w6h_2,1w6i_1,2g1s_1,2g24_1,1rne_1,2fdp_1,2g24_2," +
					 "2r9b_1,2g22_2,2g1o_2,4fs4_2,4er1_5,2r9b_2,3fnt_1,2g20_1,2iko_1,2g94_3," +
					 "3fnu_4,4j1f_1,2g26_2,1mpp_1,3own_1,3own_2,2apr_1,1ize_1,1xe5_1,1dp5_1," +
					 "3zkn_1,3zkn_2,2qzw_2,2qzw_1,1e81_5,1pfz_3,1pfz_1,3g72_1,3t7x_1,1f34_1," +
					 "3urj_1,1e80_5,3t7q_1,1er8_5,1dpj_1,3cid_1,1e82_5,1xdh_2,1uh8_1,4l6b_1," +
					 "1bim_2,3psg_1,1pso_5,1xn2_4,4lbt_1,5er1_5,1od1_1,1uh9_1,2g21_2,5er2_5," +
					 "2g21_1,3qvi_4,3qvi_3,1bil_2,1bil_1,3zmg_1,2v11_15,5apr_5,4gid_4,2v11_3," +
					 "3prs_1,4gid_2,2bks_1,1avf_10,4lap_entity,4cms_1,2v00_1,4bfb_2,3oot_1," +
					 "4bfb_1,3qrv_1,4amt_1,3qrv_2,3q3t_1,3er5_5,1xn2_3,2bkt_1,3oot_2,2iku_2," +
					 "1sme_1,1ls5_1,3pi0_1,1smr_5,3c9x_1,3zkx_1,1j71_1,2er0_5,4b1d_1,3er3_5," +
					 "1oex_1,4ape_1,4b1e_1,4j0p_1,1qrp_5,1m43_1,3pep_1,1eed_16,1yx9_1,2rmp_1," +
					 "3vyf_1,2er7_5,3g6z_2,3liz_1,1yg9_1,4kup_1,3vye_1,2h6t_1,1xe6_1,4j0t_1," +
					 "1xe6_2,4er2_5,2i4q_1,4b1c_1,1b5f_3,3bug_1,4er4_5,1ls5_2,3buh_1,1me6_2," +
					 "1lya_2,1cms_1,1lya_3,4j0y_1,1ent_5,3pb5_1,3app_1,2vs2_1,3vyd_1,3km4_2," +
					 "1uh7_1,3buf_1,1qs8_1,3utl_1,3ixj_3,1lyb_2,1lyb_1,1lyb_4,1wkr_1,1gvw_1," +
					 "1me6_1,2il2_1,3pbz_1,1epq_5,1xs7_4,3qs1_1,3d91_2,3qs1_2,1lee_1,1epp_5," +
					 "1g0v_1,3q4b_1,1flh_1,1fq7_1,2asi_1,1epr_5,2igx_1,2v10_3,3cms_1,4b70_1," +
					 "3zlq_1,3zov_1,4obz_3,4gj6_1,4obz_2,4j1h_1,4gj6_2,1izd_1,1zap_1,1bxq_1," +
					 "4gj5_1,2x0b_3,3o9l_3,2x0b_5,4obz_4,3o9l_1,2er9_5,3o9l_4,3pww_1,4j1i_1," +
					 "3oad_4,1gvt_1,1qdm_1,3oad_1,3oad_2,3sfc_2,1htr_2,2er6_5,1epn_5,2wea_1," +
					 "1epo_5,2h6s_1,3pm4_1,1lf3_1,2web_1,3gw5_1,1epl_5,4lhh_1,3vsx_2,3vsx_1," +
					 "3zkq_1,1gkt_1,2v13_1,3g70_2,4gjc_2,3qvc_1,1epm_5,3oqk_2,1e5o_5,1gvx_1," +
					 "4lp9_1,3oqk_1,4b0q_1,4b78_1,3pbd_1,2wed_1,4aa9_1,3zkm_2,3u6a_1,2wec_1," +
					 "2v0z_3,4oc6_1,4oc6_2,3t6i_1,2vkm_2,1gvv_1,3pvk_1,4j0z_1,4bek_1,1hrn_2," +
					 "1bbs_1,1hrn_1,4bel_2,3cib_1", StringUtils.join(results.getJoinIds(), ','));
		
		assertEquals(results.getResult("3fv3_8").getPdbIdChain(), "3FV3_H");
	}
	
}
