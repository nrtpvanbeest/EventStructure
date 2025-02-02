package ee.ut.eventstr.comparison.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jbpt.utils.IOUtils;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import ee.ut.eventstr.PESSemantics;
import ee.ut.eventstr.PrimeEventStructure;
import ee.ut.eventstr.comparison.PartialSynchronizedProduct;
import ee.ut.eventstr.util.PORuns2PES;

public class SeqLoopTest {
	@Test
	public void testCode() {
		PrimeEventStructure<Integer> pes1 = getPES1(), pes2 = getPES2();
		System.out.println("===================================");
		pes1.printBRelMatrix(System.out);
		System.out.println("===================================");
		pes2.printBRelMatrix(System.out);
		System.out.println("===================================");

		PartialSynchronizedProduct<Integer> psp = 
				new PartialSynchronizedProduct<>(new PESSemantics<>(pes1), new PESSemantics<>(pes2));
		
		for (String diff: psp.perform().prune().getDiff()) {
			System.out.println("DIFF: " + diff);
		}

		IOUtils.toFile("target/added_task.dot", psp.toDot());
	}

	public PrimeEventStructure<Integer> getPES1() {
		Multimap<Integer, Integer> adj = HashMultimap.create();
		adj.put(0, 1);
		adj.put(1, 2);
		Multimap<Integer, Integer> conc = HashMultimap.create();

		return PORuns2PES.getPrimeEventStructure(
				adj, conc, Arrays.asList(0), Arrays.asList(2), ImmutableMap.of(0, "a", 1, "b", 2, "d"), "PES1");
	}
	public PrimeEventStructure<Integer> getPES2() {
		Multimap<Integer, Integer> adj = HashMultimap.create();
		adj.put(0, 1);
		adj.put(1, 2);
		adj.put(1, 3);
		adj.put(3, 4);
		adj.put(4, 5);
		adj.put(4, 6);
		adj.put(6, 7);
		adj.put(7, 8);
		
		Multimap<Integer, Integer> conc = HashMultimap.create();

		Map<Integer,String> labels = new HashMap<>();
		labels.put(0,"a");
		labels.put(1,"b");
		labels.put(2,"d");
		labels.put(3,"c");
		labels.put(4,"b");
		labels.put(5,"d");
		labels.put(6,"c");
		labels.put(7,"b");
		labels.put(8,"d");

		return PORuns2PES.getPrimeEventStructure(adj, conc, Arrays.asList(0), Arrays.asList(2,5,8), labels, "PES2");
	}
}
