package uk.co.flax.biosolr.pdbe;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import uk.ac.ebi.webservices.axis1.stubs.fasta.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.fasta.WsResultType;

public class TestFastaJob {

	@Test
	public void parse() throws IOException, URISyntaxException {
		byte[] result = Files.readAllBytes(Paths.get(TestFastaJob.class.getResource("result").toURI()));
		
		JDispatcherService_PortType fasta = mock(JDispatcherService_PortType.class);
		when(fasta.getStatus(null)).thenReturn(FastaStatus.DONE);
		WsResultType[] types = new WsResultType[] { mock(WsResultType.class) };
		when(fasta.getResultTypes(null)).thenReturn(types);
		when(fasta.getResult(null, null, null)).thenReturn(result);
		
		FastaJob job = new FastaJob(fasta, "", 100.0d);
		job.run();
		
		assertEquals(1000, job.getNumChains());
		assertEquals(317, job.getNumEntries());
	}
	
}
