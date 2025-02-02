package ee.ut.mining.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import ee.ut.graph.util.GraphUtils;

public class PORun implements PORunConst {
	private static AtomicInteger nextId = new AtomicInteger();

	protected boolean[][] adjmatrix;
	protected Map<Integer, String> labels;
	private Multimap<Integer, Integer> concurrency;
	protected List<Integer> vertices;
	protected Map<Integer, Integer> vertexIndexMap;
	private Multimap<Integer, Integer> predList = null;

	public PORun(AlphaRelations alphaRelations, XTrace trace) {
		this.labels = new HashMap<>();	
		this.vertices = new ArrayList<>();
		this.vertexIndexMap = new HashMap<>();
		this.concurrency = HashMultimap.create();
		
		// === Map events to local identifiers
		Integer id = nextId.getAndIncrement();
		vertices.add(id);
		vertexIndexMap.put(id, 0);
		labels.put(id, GLOBAL_SOURCE_LABEL);
		for (XEvent e: trace)
			if (isCompleteEvent(e)) {
				id = nextId.getAndIncrement();
				vertices.add(id);
				vertexIndexMap.put(id, vertexIndexMap.size());
				labels.put(id, getEventName(e));
			}
		id = nextId.getAndIncrement();
		vertices.add(id);
		vertexIndexMap.put(id, vertexIndexMap.size());
		labels.put(id, GLOBAL_SINK_LABEL);
		id = nextId.getAndIncrement();
		vertices.add(id);
		vertexIndexMap.put(id, vertexIndexMap.size());
		labels.put(id, GLOBAL_SINK_PRIME_LABEL);
		
		// === We compute the transitive closure of causality
		// === Initially we have a trivial sequence. The order on events reflects
		// === the causal relation. Therefore, the transitive closure of causality
		// === corresponds with an upper triangle matrix.
		int size = labels.size();
		this.adjmatrix = new boolean[size][size];
		for (int i = 0; i < size-1; i++)
			Arrays.fill(adjmatrix[i], i+1, size, true);
//		System.out.println("=============================");
//		GraphUtils.print(adjmatrix);
		
		// ======================================================
		// Introduce the concurrency relation
		// ======================================================
		for (int i = 1; i < size - 1; i++) {
			Integer vertex1 = vertices.get(i);
			String label1 = labels.get(vertex1);
			for (int j = i + 1; j < size - 1; j++) {
				Integer vertex2 = vertices.get(j);
				String label2 = labels.get(vertex2);
				if (alphaRelations.areConcurrent(label1, label2)) {
					adjmatrix[i][j] = false;
					concurrency.put(vertex1, vertex2);
					concurrency.put(vertex2, vertex1);
				}
			}
		}
//		System.out.println("=============================");
//		GraphUtils.print(adjmatrix);

		GraphUtils.transitiveReduction(adjmatrix);
//		System.out.println("=============================");
//		GraphUtils.print(adjmatrix);
//		System.out.println("=============================");
	}
	
	private String getEventName(XEvent e) {
		return e.getAttributes().get(XConceptExtension.KEY_NAME).toString();
	}

	private boolean isCompleteEvent(XEvent e) {
		XAttributeMap amap = e.getAttributes();
		return (amap.get(XLifecycleExtension.KEY_TRANSITION).toString().toLowerCase().equals("complete"));
	}
	
	public Map<Integer, String> getLabelMap() {
		return labels;
	}
	
	public Multimap<Integer, Integer> asSuccessorsList() {
		Multimap<Integer, Integer> succList = HashMultimap.create();
		this.predList = HashMultimap.create();
		int size = adjmatrix.length;
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++)
				if (adjmatrix[i][j]) {
					succList.put(vertices.get(i), vertices.get(j));
					predList.put(vertices.get(j), vertices.get(i));
				}
		}
		return succList;
	}
	
	public Multimap<Integer, Integer> asPredecessorsList() {
		if (predList == null) {
			predList = HashMultimap.create();
			int size = adjmatrix.length;
			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++)
					if (adjmatrix[i][j])
						predList.put(vertices.get(j), vertices.get(i));
			}
		}
		return predList;
	}
	
	public Multimap<Integer, Integer> getConcurrencyRelation() {
		return concurrency;
	}
	
	public Integer getSource() {
		return vertices.get(0);
	}
	
	public Integer getSink() {
		return vertices.get(vertices.size() - 1);
	}
}
