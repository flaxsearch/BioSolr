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
		
		assertEquals(116, results.getNumChains());
		
    String[] pdbIds = new String[] {
        "1cms", "3cms", "4aa9", "5pep", "1psn", "1flh", "1f34", "3pep", "2psg", "1tzs",
        "1am5", "1avf", "1qdm", "1smr", "3vcm", "1bil", "1bbs", "3d91", "2i4q", "1g0v",
        "2x0b", "2fs4", "1dp5", "1fq4", "1lya", "4obz", "1b5f", "1uh7", "2apr", "3qrv",
        "1me6", "2r9b", "1lf3", "1pfz", "3f9q", "1lee", "2bju", "3o9l", "2anl", "1qs8",
        "1miq", "1ls5", "3liz", "1yg9", "3fns", "3qvc", "1mpp", "2asi", "3fv3", "1lya",
        "4obz", "2h6s", "3o9l", "1zap", "3emy", "1eag", "4b1c", "1j71", "1apt", "1ibq",
        "2ewy", "3zkm", "3zkg", "4b70", "4b0q", "4b1d", "4ewo", "2fdp", "1sgz", "4fs4",
        "3u6a", "1fkn", "4dpf", "3kmx", "2qk5", "2of0", "3r1g", "1ym2", "3udh", "2zjn",
        "3ixk", "2hm1", "3dm6", "1ym4", "4l7g", "3hvg", "1w50", "2zhr", "4fsl", "2wjo",
        "3l58", "2zdz", "3vv6", "3bra", "3tpr", "2zjk", "4x7i", "2va5", "2hiz", "3lpi",
        "2q11", "2q15", "2zji", "2vie", "1tqf", "3qi1", "2qzl", "3exo", "3tpj", "2zjh",
        "2qzx", "2zjj", "1e5o", "1izd", "1wkr", "1b5f"
    };
    Set<String> pdbIdSet = new HashSet<>(Arrays.asList(pdbIds));
    assertEquals(pdbIdSet, results.getPdbIds());

    // values checked from http://www.ebi.ac.uk/Tools/hmmer/results/7B8EF2BA-5C4B-11E5-BB6E-2FF0D26C98AD/score
    Alignment a = results.getAlignments().get("3zkm").get("A");
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
