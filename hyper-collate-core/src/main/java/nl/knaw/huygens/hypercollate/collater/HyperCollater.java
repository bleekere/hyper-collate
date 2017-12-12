package nl.knaw.huygens.hypercollate.collater;

    /*-
     * #%L
     * hyper-collate-core
     * =======
     * Copyright (C) 2017 Huygens ING (KNAW)
     * =======
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     * #L%
     */

import com.google.common.base.Preconditions;
import eu.interedition.collatex.dekker.Tuple;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.StreamUtil;
import nl.knaw.huygens.hypergraph.core.TraditionalEdge;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;

public class HyperCollater {

  private static final BiFunction<SimpleTokenVertex, SimpleTokenVertex, Boolean> matcher = //
      (stv1, stv2) -> {
        if (stv1.getNormalizedContent().isEmpty() && stv2.getNormalizedContent().isEmpty()) {
          if (stv1.getContent().isEmpty() && stv2.getContent().isEmpty()) {
            // both are milestones, so compare their tags.
            String parentTag1 = stv1.getParentXPath().replace("/.*/", "");
            String parentTag2 = stv2.getParentXPath().replace("/.*/", "");
            return parentTag1.equals(parentTag2);
          } else {
            return stv1.getContent().equals(stv2.getContent());
          }
        }
        return stv1.getNormalizedContent().equals(stv2.getNormalizedContent());
      };
  private final OptimalMatchSetFinder optimalMatchSetFinder;

  public HyperCollater(OptimalMatchSetFinder optimalMatchSetFinder) {
    Preconditions.checkNotNull(optimalMatchSetFinder);
    this.optimalMatchSetFinder = optimalMatchSetFinder;
  }

  public CollationGraph collate(VariantWitnessGraph... graphs) {
    List<String> sigils = new ArrayList<>();
    List<VariantWitnessGraph> witnesses = new ArrayList<>();
    List<VariantWitnessGraphRanking> rankings = new ArrayList<>();
    for (VariantWitnessGraph graph : graphs) {
      sigils.add(graph.getSigil());
      witnesses.add(graph);
      rankings.add(VariantWitnessGraphRanking.of(graph));
    }

    Set<Match> matches = match(witnesses, rankings);

    List<Match> matchesSortedByRank = sortMatchesByWitness(matches, sigils);

    VariantWitnessGraph witness1 = witnesses.get(0);
    VariantWitnessGraph witness2 = witnesses.get(1);
    String sigil1 = witness1.getSigil();
    String sigil2 = witness2.getSigil();
    Iterator<TokenVertex> iterator1 = VariantWitnessGraphTraversal.of(witness1).iterator();
    Iterator<TokenVertex> iterator2 = VariantWitnessGraphTraversal.of(witness2).iterator();

    CollationGraph collationGraph = new CollationGraph(sigils);

    for (VariantWitnessGraph witnessGraph : witnesses) {
      collate(collationGraph, witnessGraph);
    }
//    collateFirstTwoWitnesses(matchesSortedByRank, witness1, witness2, sigil1, sigil2, iterator1, iterator2, collatedTokenVertexMap, collationGraph);

    return collationGraph;
  }

  private void collateFirstTwoWitnesses(List<Match> matchesSortedByRank,
                                        VariantWitnessGraph witness1, VariantWitnessGraph witness2,
                                        String sigil1, String sigil2,
                                        Iterator<TokenVertex> iterator1, Iterator<TokenVertex> iterator2,
                                        Map<TokenVertex, Node> collatedTokenVertexMap,
                                        CollationGraph collationGraph) {
    Node lastCollatedVertex1 = collationGraph.getRootNode();
    collatedTokenVertexMap.put(iterator1.next(), lastCollatedVertex1);
    Node lastCollatedVertex2 = collationGraph.getRootNode();
    collatedTokenVertexMap.put(iterator2.next(), lastCollatedVertex2);
    while (!matchesSortedByRank.isEmpty()) {
      Match match = matchesSortedByRank.get(0);
      // System.out.println(match);

      TokenVertex tokenVertexForWitness1 = match.getTokenVertexForWitness(sigil1);
      TokenVertex tokenVertexForWitness2 = match.getTokenVertexForWitness(sigil2);
      matchesSortedByRank.remove(match);

      advanceWitness(collationGraph, collatedTokenVertexMap, sigil1, iterator1, tokenVertexForWitness1);
      advanceWitness(collationGraph, collatedTokenVertexMap, sigil2, iterator2, tokenVertexForWitness2);
      handleMatch(collationGraph, sigil1, sigil2, collatedTokenVertexMap, tokenVertexForWitness1, tokenVertexForWitness2);
      // System.out.println();
    }

    addEndNode(collationGraph, witness1, witness2, collatedTokenVertexMap);
    addEdges(collationGraph, collatedTokenVertexMap);
  }

  void collate(CollationGraph collationGraph, VariantWitnessGraph witnessGraph) {
    Map<TokenVertex, CollationGraph.Node> collatedTokenVertexMap = new HashMap<>();

    if (collationGraph.isEmpty()) {
      collationGraph.getSigils().add(witnessGraph.getSigil());
      collatedTokenVertexMap.put(witnessGraph.getStartTokenVertex(), collationGraph.getRootNode());
      StreamUtil.stream(witnessGraph.vertices())//
          .filter(v -> v instanceof SimpleTokenVertex)//
          .forEach(tokenVertex -> {
                Node node = collationGraph.addNodeWithTokens(tokenVertex.getToken());
                collatedTokenVertexMap.put(tokenVertex, node);
              }
          );
      Node endNode = collationGraph.addNodeWithTokens();
      collatedTokenVertexMap.put(witnessGraph.getEndTokenVertex(), endNode);
      addEdges(collationGraph, collatedTokenVertexMap);


    } else {
      collationGraph.getSigils().add(witnessGraph.getSigil());
      Node lastCollatedVertex1 = collationGraph.getRootNode();
//      collatedTokenVertexMap.put(iterator1.next(), lastCollatedVertex1);
//      Node lastCollatedVertex2 = collationGraph.getRootNode();
//      collatedTokenVertexMap.put(iterator2.next(), lastCollatedVertex2);
//      while (!matchesSortedByRank.isEmpty()) {
//        Match match = matchesSortedByRank.get(0);
//        // System.out.println(match);
//
//        TokenVertex tokenVertexForWitness1 = match.getTokenVertexForWitness(sigil1);
//        TokenVertex tokenVertexForWitness2 = match.getTokenVertexForWitness(sigil2);
//        matchesSortedByRank.remove(match);
//
//        advanceWitness(collationGraph, collatedTokenVertexMap, sigil1, iterator1, tokenVertexForWitness1);
//        advanceWitness(collationGraph, collatedTokenVertexMap, sigil2, iterator2, tokenVertexForWitness2);
//        handleMatch(collationGraph, sigil1, sigil2, collatedTokenVertexMap, tokenVertexForWitness1, tokenVertexForWitness2);
//        // System.out.println();
//      }
//
//      addEndNode(collationGraph, witness1, witness2, collatedTokenVertexMap);
//      addEdges(collationGraph, collatedTokenVertexMap);

    }

  }

  private static void addEndNode(CollationGraph collationGraph, VariantWitnessGraph witness1, VariantWitnessGraph witness2, Map<TokenVertex, Node> collatedTokenVertexMap) {
    Node endNode = collationGraph.addNodeWithTokens();
    collatedTokenVertexMap.put(witness1.getEndTokenVertex(), endNode);
    collatedTokenVertexMap.put(witness2.getEndTokenVertex(), endNode);
  }

  private static void handleMatch(CollationGraph collationGraph, String sigil1, String sigil2, Map<TokenVertex, Node> collatedTokenVertexMap, TokenVertex tokenVertexForWitness1,
                                  TokenVertex tokenVertexForWitness2) {
    // System.out.println("> " + sigil1 + "+" + sigil2);
    Node newCollationNode = collationGraph.addNodeWithTokens(tokenVertexForWitness1.getToken(), tokenVertexForWitness2.getToken());
    collatedTokenVertexMap.put(tokenVertexForWitness1, newCollationNode);
    collatedTokenVertexMap.put(tokenVertexForWitness2, newCollationNode);
  }

  private static void advanceWitness(CollationGraph collationGraph, //
                                     Map<TokenVertex, Node> collatedTokenVertexMap, //
                                     String sigil, Iterator<TokenVertex> tokenVertexIterator, //
                                     TokenVertex tokenVertexForWitness) {
    if (tokenVertexIterator.hasNext()) {
      TokenVertex nextWitnessVertex = tokenVertexIterator.next();
      while (tokenVertexIterator.hasNext() && !nextWitnessVertex.equals(tokenVertexForWitness)) {
        // System.out.println("> " + sigil);
        addCollationNode(collationGraph, collatedTokenVertexMap, nextWitnessVertex);
        nextWitnessVertex = tokenVertexIterator.next();
        // System.out.println();
      }
    }
  }

  private static void addEdges(CollationGraph collationGraph, Map<TokenVertex, Node> collatedTokenVertexMap) {
    collatedTokenVertexMap.keySet().forEach(tv -> tv.getIncomingTokenVertexStream().forEach(itv -> {
      Node source = collatedTokenVertexMap.get(itv);
      Node target = collatedTokenVertexMap.get(tv);
      if (source == null || target == null) {
        // System.out.println();
      }
      List<Node> existingTargetNodes = collationGraph.getOutgoingEdges(source)//
          .stream()//
          .map(collationGraph::getTarget)//
          .collect(toList());
      String sigil = tv.getSigil();
      if (!existingTargetNodes.contains(target)) {
        Set<String> sigils = new HashSet<>();
        sigils.add(sigil);
        collationGraph.addDirectedEdge(source, target, sigils);
        // System.out.println("> " + source + " -> " + target);
      } else {
        TraditionalEdge edge = collationGraph.getOutgoingEdges(source)//
            .stream()//
            .filter(e -> collationGraph.getTarget(e).equals(target))//
            .findFirst()//
            .orElseThrow(() -> new RuntimeException("There should be an edge!"));
        edge.getSigils().add(sigil);
        // System.err.println("duplicate edge: " + source + " -> " + target);
      }
    }));
  }

  private static void addCollationNode(CollationGraph collationGraph, //
                                       Map<TokenVertex, CollationGraph.Node> collatedTokenMap, //
                                       TokenVertex tokenVertex) {
    if (!collatedTokenMap.containsKey(tokenVertex)) {
      Node collationNode = collationGraph.addNodeWithTokens(tokenVertex.getToken());
      collatedTokenMap.put(tokenVertex, collationNode);
    }
  }

  private static List<Match> sortMatchesByWitness(Set<Match> matches, //
                                                  List<String> sigils) {
    Comparator<Match> matchComparator = matchComparator(sigils.get(0), sigils.get(1));
    return matches.stream()//
        .sorted(matchComparator)//
        .collect(toList());
  }

  private static Comparator<Match> matchComparator(String sigil1, //
                                                   String sigil2) {
    return (match1, match2) -> {
      Integer rank1 = match1.getRankForWitness(sigil1);
      Integer rank2 = match2.getRankForWitness(sigil1);
      if (rank1.equals(rank2)) {
        rank1 = match1.getRankForWitness(sigil2);
        rank2 = match2.getRankForWitness(sigil2);
      }
      return rank1.compareTo(rank2);
    };
  }

  private Set<Match> match(List<VariantWitnessGraph> witnesses, List<VariantWitnessGraphRanking> rankings) {
    Set<Match> allPotentialMatches = getAllPotentialMatches(witnesses, rankings);
    return optimalMatchSetFinder.getOptimalMatchSet(allPotentialMatches);
  }

  Set<Match> getAllPotentialMatches(List<VariantWitnessGraph> witnesses, List<VariantWitnessGraphRanking> rankings) {
    Set<Match> allPotentialMatches = new HashSet<>();
    List<Tuple<Integer>> permutations = permute(witnesses.size());
    Map<TokenVertex, Match> vertexToMatch = new HashMap<>();
    permutations.forEach(t -> {
      VariantWitnessGraph witness1 = witnesses.get(t.left);
      VariantWitnessGraph witness2 = witnesses.get(t.right);
      VariantWitnessGraphRanking ranking1 = rankings.get(t.left);
      VariantWitnessGraphRanking ranking2 = rankings.get(t.right);
      match(witness1, witness2, ranking1, ranking2, allPotentialMatches, vertexToMatch);
    });

    TokenVertex[] endTokenVertices = witnesses.stream().map(VariantWitnessGraph::getEndTokenVertex).toArray(TokenVertex[]::new);
    Match endMatch = new Match(endTokenVertices);
    for (int i = 0; i < endTokenVertices.length; i++) {
      String sigil = witnesses.get(i).getSigil();
      Integer rank = rankings.get(i).apply(endTokenVertices[i]);
      endMatch.setRank(sigil, rank);
    }
    allPotentialMatches.add(endMatch);
    return allPotentialMatches;
  }

  List<Tuple<Integer>> permute(int max) {
    List<Tuple<Integer>> list = new ArrayList<>();
    for (int left = 0; left < max; left++) {
      for (int right = left + 1; right < max; right++) {
        list.add(new Tuple(left, right));
      }
    }
    return list;
  }

  private void match(VariantWitnessGraph witness1, VariantWitnessGraph witness2, VariantWitnessGraphRanking ranking1, VariantWitnessGraphRanking ranking2, Set<Match> allPotentialMatches, Map<TokenVertex, Match> vertexToMatch) {
    VariantWitnessGraphTraversal traversal1 = VariantWitnessGraphTraversal.of(witness1);
    VariantWitnessGraphTraversal traversal2 = VariantWitnessGraphTraversal.of(witness2);
    String sigil1 = witness1.getSigil();
    String sigil2 = witness2.getSigil();
    stream(traversal1)//
        .filter(tv -> tv instanceof SimpleTokenVertex)//
        .map(SimpleTokenVertex.class::cast)//
        .forEach(tv1 -> stream(traversal2)//
            .filter(tv -> tv instanceof SimpleTokenVertex)//
            .map(SimpleTokenVertex.class::cast)//
            .forEach(tv2 -> {
              if (matcher.apply(tv1, tv2)) {
                if (vertexToMatch.containsKey(tv1)) {
                  vertexToMatch.get(tv1)//
                      .addTokenVertex(tv2)//
                      .setRank(tv2.getSigil(), ranking2.apply(tv2));

                } else if (vertexToMatch.containsKey(tv2)) {
                  vertexToMatch.get(tv2)//
                      .addTokenVertex(tv1)//
                      .setRank(tv1.getSigil(), ranking1.apply(tv1));

                } else {
                  Match match = new Match(tv1, tv2)//
                      .setRank(sigil1, ranking1.apply(tv1))//
                      .setRank(sigil2, ranking2.apply(tv2));
                  allPotentialMatches.add(match);
                  vertexToMatch.put(tv1, match);
                  vertexToMatch.put(tv2, match);
                }
              }
            }));
  }

  public String getOptimalMatchSetFinderName() {
    return optimalMatchSetFinder.getName();
  }

}
