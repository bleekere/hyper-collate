package nl.knaw.huygens.hypercollate.dropwizard.api;

/*-
 * #%L
 * hyper-collate-server
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import nl.knaw.huygens.hypercollate.api.CollationInput;
import nl.knaw.huygens.hypercollate.dropwizard.db.CollationInfo;
import nl.knaw.huygens.hypercollate.model.CollationGraph;

public interface CollationStore {

  void setCollation(UUID uuid, CollationGraph collationGraph, CollationInput collationInput, long collationDuration);

  Set<UUID> getCollationUUIDs();

  Optional<CollationGraph> getCollationGraph(UUID uuid);

  Optional<CollationInfo> getCollationInfo(UUID uuid);

}