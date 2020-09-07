package org.processmining.behavioralspaces.alignmentbased;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import nl.tue.astar.AStarException;

	public class AlignmentBasedChecker {

		static int MAX_STATES = 200;
		
		Petrinet net;
		XLog log;
		PluginContext context;
		PNLogReplayer replayer;
		PetrinetReplayerWithoutILP replayerWithoutILP;
		private TransEvClassMapping transEventMap;
		private CostBasedCompleteParam parameters;
		Map<List<String>, Double> fitnessMap;
		
		public AlignmentBasedChecker(Petrinet net, XLog log, PluginContext context) {
			super();
			this.net = net;
			this.log = log;
			this.context = context;
			this.fitnessMap = new HashMap<>();
			
			AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
			
			// instantiate replayer
			replayer = new PNLogReplayer();
			replayerWithoutILP = new PetrinetReplayerWithoutILP();

			XEventClassifier classifierMap = XLogInfoImpl.NAME_CLASSIFIER;

			XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
			transEventMap = computeTransEventMapping(log, net);
			parameters = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
					transEventMap.getDummyEventClass(), apn.getNet().getTransitions(), 2, 5);
			parameters.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
			parameters.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);

			parameters.setGUIMode(false);
			parameters.setCreateConn(false);
			parameters.setInitialMarking(apn.getInitialMarking());
			
			List<Marking> finalMarkingList = computeMarkingCombinations(apn.getFinalMarkings());
			
			Marking[] finalMarkings = new Marking[finalMarkingList.size()];
//			Marking[] finalMarkings = new Marking[apn.getFinalMarkings().size()];
			int i = 0;
//			for (Marking m : apn.getFinalMarkings()) {
			for (Marking m : finalMarkingList) {
				finalMarkings[i] = m;
				i++;
			}
						
			parameters.setFinalMarkings(finalMarkings);
			parameters.setMaxNumOfStates(MAX_STATES);
		}
		
		
		private List<Marking> computeMarkingCombinations(Set<Marking> markings) {
			
			Set<Place> fullset = new HashSet<Place>();
			for (Marking m : markings) {
				fullset.addAll(m);
			}
			
			List<Marking> res = new ArrayList<Marking>();
			
			Set<Set<Place>> sets = powerSet(fullset);
			for (Set<Place> set : sets) {
				if (!set.isEmpty()) {
					res.add(new Marking(set));
				}
			}
			return res;
		}
	
	static <T> Set<Set<T>> powerSet( Set<T> set ) {
        T[] element = (T[]) set.toArray();
        final int SET_LENGTH = 1 << element.length;
        Set<Set<T>> powerSet = new HashSet<>();
        for( int binarySet = 0; binarySet < SET_LENGTH; binarySet++ ) {
            Set<T> subset = new HashSet<>();
            for( int bit = 0; bit < element.length; bit++ ) {
                int mask = 1 << bit;
                if( (binarySet & mask) != 0 ) {
                    subset.add( element[bit] );
                }
            }
            powerSet.add( subset );
        }
        return powerSet;
    }
		


		public boolean isConformant(XTrace trace) {
			XLog log2 = XFactoryRegistry.instance().currentDefault().createLog();
			log2.add(trace);
			
			try {
				PNRepResult replayRes = replayer.replayLog(context, net, log2, transEventMap, replayerWithoutILP, parameters);
				if (!replayRes.isEmpty()) {
					double fit = (Double) replayRes.getInfo().get(PNRepResult.TRACEFITNESS);
					boolean conformance = (fit == 1.0);
					
					for(Map.Entry<Transition, Integer> entry : parameters.getMapTrans2Cost().entrySet()) {
						if(!entry.getValue().equals(1)) {
							System.out.print(", Transition: "+ entry.getKey().getLabel() + " Int: " + entry.getValue());
						}
							
					}
					System.out.println();
					for(Map.Entry<XEventClass, Integer> entry : parameters.getMapEvClass2Cost().entrySet()) {
						System.out.print(", XEventclass: " + entry.getKey().getId() + " Int: " + entry.getValue());
					}
					System.out.println("Fitness Double: " + fit);
					
					return conformance;
				}

			} catch (AStarException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			return false;
		}





	private TransEvClassMapping computeTransEventMapping(XLog log, Petrinet net) {
		XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.NAME_CLASSIFIER, evClassDummy);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.NAME_CLASSIFIER).getEventClasses();
		for (Transition t : net.getTransitions()) {
			XEventClass eventClass = ecLog.getByIdentity(t.getLabel());
			if (eventClass != null) {
				mapping.put(t, eventClass);
			}
		}
		return mapping;
	}


	}



