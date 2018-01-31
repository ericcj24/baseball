package com.princeton.algorithm2.week3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

public class BaseballElimination {
	private final int numberTeams;
	private String[] teams;
	private Map<String, Integer> teamNameToIdx;
	private int[] w;
	private int[] l;
	private int[] r;
	private int[][] g;

	// create a baseball division from given filename in format specified below
	public BaseballElimination(String filename)  {
		if (filename == null) {
			throw new IllegalArgumentException();
		}
		In inputFile = new In(filename);

		this.teamNameToIdx = new HashMap<>();

		this.numberTeams = Integer.parseInt(inputFile.readLine());
		this.teams = new String[numberTeams];
		this.w = new int[numberTeams];
		this.l = new int[numberTeams];
		this.r = new int[numberTeams];
		this.g = new int[numberTeams][numberTeams];

		int i=0;
		while (inputFile.hasNextLine()) {
			String[] sections = inputFile.readLine().trim().replaceAll(" +", " ").split(" ");
			teams[i] = sections[0];
			teamNameToIdx.put(sections[0], i);
			w[i] = Integer.parseInt(sections[1]);
			l[i] = Integer.parseInt(sections[2]);
			r[i] = Integer.parseInt(sections[3]);

			for (int j=4; j<sections.length; j++) {
				g[i][j-4] = Integer.parseInt(sections[j]);
			}
			i++;
		}
	}

	// number of teams
	public int numberOfTeams() {
		return this.numberTeams;
	}

	 // all teams
	public Iterable<String> teams() {
		return teamNameToIdx.keySet();
	}

	// number of wins for given team
	public int wins(String team) {
		if (!teamNameToIdx.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		int teamIdx = teamNameToIdx.get(team);
		return w[teamIdx];
	}

	// number of losses for given team
	public int losses(String team) {
		if (!teamNameToIdx.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		int teamIdx = teamNameToIdx.get(team);
		return l[teamIdx];
	}

	// number of remaining games for given team
	public int remaining(String team) {
		if (!teamNameToIdx.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		int teamIdx = teamNameToIdx.get(team);
		return r[teamIdx];
	}

	// number of remaining games between team1 and team2
	public int against(String team1, String team2) {
		if (!teamNameToIdx.containsKey(team1) ||
				!teamNameToIdx.containsKey(team2)) {
			throw new IllegalArgumentException();
		}
		int teamIdx1 = teamNameToIdx.get(team1);
		int teamIdx2 = teamNameToIdx.get(team2);
		return g[teamIdx1][teamIdx2];
	}

	// is given team eliminated?
	public boolean isEliminated(String team) {
		if (!teamNameToIdx.containsKey(team)) {
			throw new IllegalArgumentException();
		}

		if (!cachedResult.containsKey(team)) {
			Set<String> rSet = new HashSet<>();
			if (!isTrivialElimination(rSet, team)) {
				checkNonTrivialElimination(rSet, team);
			}
			cachedResult.put(team, rSet);
		}

		return !cachedResult.get(team).isEmpty();
	}

	private FordFulkerson constructFlowNetwork(String team) {
		int teamIdx = teamNameToIdx.get(team);

		// n-1 teams pick 2
		int numberVertices = (numberTeams-1)*(numberTeams-2)/2 + numberTeams-1 + 2;
		FlowNetwork fn = new FlowNetwork(numberVertices);

		int teamVerticesInFlowNetwork = numberTeams - 1;

		int teamWin = wins(team);
		int teamRemain = remaining(team);


		// sink/target vertex numberVertices-1
		// start vertex numberVertices-2
		int tVertex = numberVertices-1;
		int sVertex = numberVertices-2;
		// build edges to sink vertex
		for (int i=0; i<teamVerticesInFlowNetwork; i++) {
			int realTeamIdx=i;
			if (realTeamIdx>=teamIdx) {
				realTeamIdx+=1;
			}

			FlowEdge s = new FlowEdge(i, tVertex, teamWin+teamRemain-w[realTeamIdx]);
			fn.addEdge(s);
		}

		int flowNetworkIdx = teamVerticesInFlowNetwork;
		// build edges from start, and edges to individual team
		for (int i=0; i<numberTeams; i++) {
			int iIdxInFlowNetwork = i;
			if (iIdxInFlowNetwork==teamIdx) continue;
			if (iIdxInFlowNetwork>teamIdx) {
				iIdxInFlowNetwork-=1;
			}
			for (int j=i+1; j<numberTeams; j++) {
				int jIdxInFlowNetwork = j;
				if(jIdxInFlowNetwork==teamIdx) continue;
				if (jIdxInFlowNetwork>teamIdx) {
					jIdxInFlowNetwork-=1;
				}

				FlowEdge s1 = new FlowEdge(flowNetworkIdx, iIdxInFlowNetwork, Double.POSITIVE_INFINITY);
				fn.addEdge(s1);

				FlowEdge s2 = new FlowEdge(flowNetworkIdx, jIdxInFlowNetwork, Double.POSITIVE_INFINITY);
				fn.addEdge(s2);

				FlowEdge s = new FlowEdge(sVertex, flowNetworkIdx, g[i][j]);
				fn.addEdge(s);

				flowNetworkIdx++;
			}
		}

		//System.out.println(fn.toString());

		FordFulkerson ff = new FordFulkerson(fn, sVertex, tVertex);

		return ff;
	}


	private void checkNonTrivialElimination(Set<String> rSet, String team) {
		int teamIdx = teamNameToIdx.get(team);

		FordFulkerson ff = constructFlowNetwork(team);

		for (int i=0; i<numberTeams; i++) {
			if (i==teamIdx) continue;
			int j=i;
			if (j>teamIdx) {
				j-=1;
			}
			if (ff.inCut(j)) {
				rSet.add(teams[i]);
			}
		}
	}

	private boolean isTrivialElimination(Set<String> rSet, String team) {
		int teamIdx =  teamNameToIdx.get(team);
		int potentialWin = w[teamIdx] + r[teamIdx];
		for (int i=0; i<numberTeams; i++) {
			if (potentialWin < w[i]) {
				rSet.add(teams[i]);
			}
		}
		return !rSet.isEmpty();
	}

	private Map<String, Set<String>> cachedResult = new HashMap<>();

	// subset R of teams that eliminates given team; null if not eliminated
	public Iterable<String> certificateOfElimination(String team) {
		if (!teamNameToIdx.containsKey(team)) {
			throw new IllegalArgumentException();
		}

		if (!isEliminated(team))
			return null;


		return cachedResult.get(team);
	}



	public static void main(String[] args) {
	    BaseballElimination division = new BaseballElimination(args[0]);
	    for (String team : division.teams()) {
	        if (division.isEliminated(team)) {
	            StdOut.print(team + " is eliminated by the subset R = { ");
	            for (String t : division.certificateOfElimination(team)) {
	                StdOut.print(t + " ");
	            }
	            StdOut.println("}");
	        }
	        else {
	            StdOut.println(team + " is not eliminated");
	        }
	    }
	}
}