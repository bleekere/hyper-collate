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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class VariantWitnessGraphRanking {

  private final Map<TokenVertex, Integer> byVertex = new HashMap<>();
  private final SortedMap<Integer, Set<TokenVertex>> byRank = new TreeMap<>();
  private final VariantWitnessGraph graph;

  VariantWitnessGraphRanking(VariantWitnessGraph graph) {
    this.graph = graph;
  }

  public static VariantWitnessGraphRanking of(VariantWitnessGraph graph) {
    final VariantWitnessGraphRanking ranking = new VariantWitnessGraphRanking(graph);
    for (TokenVertex v : graph.vertices()) {
      AtomicInteger rank = new AtomicInteger(-1);
      v.getIncomingTokenVertexStream().forEach(incoming -> rank.set(Math.max(rank.get(), ranking.byVertex.get(incoming))));
      rank.getAndIncrement();
      ranking.byVertex.put(v, rank.get());
      ranking.byRank.computeIfAbsent(rank.get(), r -> new HashSet<>()).add(v);
    }
    return ranking;
  }

}
