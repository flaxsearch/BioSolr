package uk.co.flax.biosolr.pdbe.phmmer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonReader;

import org.junit.Test;

public class TestPhmmerJob {

	private static final String RESULT_PATH = "result";
	
	@Test
	public void testParsing() throws IOException, URISyntaxException {
		byte[] result = Files.readAllBytes(Paths.get(TestPhmmerJob.class.getResource(RESULT_PATH).toURI()));

    PhmmerClient client = mock(PhmmerClient.class);
    JsonReader reader = Json.createReader(new ByteArrayInputStream(result));
    when(client.getResults(null, null)).thenReturn(reader.readObject());
    
		PhmmerJob job = new PhmmerJob(client, null, null);
		PhmmerResults results = job.runJob();
		
		assertEquals(116, results.getSize());
		
    String[] pdbIdChains = new String[] {
        "1cms_A", "3cms_A", "4aa9_A", "5pep_A", "1psn_A", "1flh_A", "1f34_A", "3pep_A", "2psg_A", "1tzs_A",
        "1am5_A", "1avf_A", "1qdm_A", "1smr_A", "3vcm_A", "1bil_A", "1bbs_A", "3d91_A", "2i4q_A", "1g0v_A",
        "2x0b_A", "2fs4_A", "1dp5_A", "1fq4_A", "1lya_B", "4obz_B", "1b5f_A", "1uh7_A", "2apr_A", "3qrv_A",
        "1me6_A", "2r9b_A", "1lf3_A", "1pfz_A", "3f9q_A", "1lee_A", "2bju_A", "3o9l_A", "2anl_A", "1qs8_A",
        "1miq_A", "1ls5_A", "3liz_A", "1yg9_A", "3fns_A", "3qvc_A", "1mpp_A", "2asi_A", "3fv3_A", "1lya_A",
        "4obz_A", "2h6s_A", "3o9l_B", "1zap_A", "3emy_A", "1eag_A", "4b1c_A", "1j71_A", "1apt_E", "1ibq_A",
        "2ewy_A", "3zkm_A", "3zkg_A", "4b70_A", "4b0q_A", "4b1d_A", "4ewo_A", "2fdp_A", "1sgz_A", "4fs4_A",
        "3u6a_A", "1fkn_A", "4dpf_A", "3kmx_A", "2qk5_A", "2of0_A", "3r1g_B", "1ym2_A", "3udh_A", "2zjn_A",
        "3ixk_A", "2hm1_A", "3dm6_A", "1ym4_A", "4l7g_A", "3hvg_A", "1w50_A", "2zhr_A", "4fsl_A", "2wjo_A",
        "3l58_A", "2zdz_A", "3vv6_A", "3bra_A", "3tpr_A", "2zjk_A", "4x7i_A", "2va5_A", "2hiz_A", "3lpi_A",
        "2q11_A", "2q15_A", "2zji_A", "2vie_A", "1tqf_A", "3qi1_A", "2qzl_A", "3exo_A", "3tpj_A", "2zjh_A",
        "2qzx_A", "2zjj_A", "1e5o_E", "1izd_A", "1wkr_A", "1b5f_B"
    };
    Set<String> pdbIdChainSet = new HashSet<>(Arrays.asList(pdbIdChains));
    assertEquals(pdbIdChainSet, results.getPdbIdChains());

    // values checked from http://www.ebi.ac.uk/Tools/hmmer/results/7B8EF2BA-5C4B-11E5-BB6E-2FF0D26C98AD/score
    Alignment a = results.getAlignment("3zkm_A");
    assertEquals("BETA-SECRETASE 2", a.getDescription());
    assertEquals("Homo sapiens", a.getSpecies());
    assertEquals(78.2, a.getScore(), 0.1);
    assertEquals(16, a.getQuerySequenceStart());
    assertEquals(227, a.getQuerySequenceEnd());
    assertEquals(9, a.getTargetEnvelopeStart());
    assertEquals(259, a.getTargetEnvelopeEnd());
    assertEquals(18, a.getTargetSequenceStart());
    assertEquals(240, a.getTargetSequenceEnd());
    assertEquals(0.75, a.getBias(), 0.01);
    assertEquals(0.78, a.getAccuracy(), 0.01);
    assertEquals(31.6, a.getIdentityPercent(), 0.1);
    assertEquals(67, a.getIdentityCount());
    assertEquals(57.1, a.getSimilarityPercent(), 0.1);
    assertEquals(121, a.getSimilarityCount());
    assertEquals(74.0, a.getBitScore(), 0.1);
    assertEquals(7.9e-21, a.getEValue(), 1e-22);
    assertEquals(1.5e-19, a.getEValueInd(), 1e-20);
    assertEquals(6.6e-23, a.getEValueCond(), 1e-24);
    assertEquals("yfgkiylgtppqeftvlfdtgssdfwvpsiycksnacknhqrfdprksstfqnlgkplsihygtgsmqgilgydtvtvsn" +
                 "ivdiqqtvglstqepgdvft..yaefdgilgmaypslaseys.ip.vfdnmmnrhlvaqdlfsvymdrng........qe" +
                 "smltlgaidpsyytgslhwvpvtvqqywqftvdsvtisgvvv..ace..ggcqaildtgtsklvgpss",
                 a.getQuerySequence());
    assertEquals("y+ ++ +gtppq++ +l dtgss+f v               fd ++sst+++ g  +++ y  gs  g++g d vt+  " +
                 "  +    v+++t   ++ f     +++gilg+ay +la   s +   fd+++ +     ++fs+ m   g          " +
                 "  l lg i+ps y g + + p+  + y+q+ +  + i g  +   c   ++ +ai+d+gt+ l  p  ",
                 a.getMatch());
    assertEquals("YYLEMLIGTPPQKLQILVDTGSSNFAVAGTPH----SYIDTYFDTERSSTYRSKGFDVTVKYTQGSWTGFVGEDLVTIPK" +
                 "GFNTSFLVNIATIFESENFFlpGIKWNGILGLAYATLAKPSSsLEtFFDSLVTQAN-IPNVFSMQMCGAGlpvagsgtNG" +
                 "GSLVLGGIEPSLYKGDIWYTPIKEEWYYQIEILKLEIGGQSLnlDCReyNADKAIVDSGTTLLRLPQK",
                 a.getTargetSequence());
    assertEquals("888999*******************9976432....233467**************************************" +
                 "**********9976665554114699***********9865425515677776655.46789888865441111111134" +
                 "5899*********************************99764115643356789******98877765",
                 a.getPosteriorProbability());
	}
	
}
