package nl.knaw.huygens.hypercollate.api;

/*-
 * #%L
 * hyper-collate-api
 * =======
 * Copyright (C) 2017 - 2018 Huygens ING (KNAW)
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

import java.net.URI;

public class AboutInfo {
  private String appName;
  private String startedAt;
  private String version;
  private String buildDate;
  private String commitId;
  private String scmBranch;
  private URI projectDir;
  private boolean dotRendering;

  public AboutInfo setAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public String getAppName() {
    return appName;
  }

  public AboutInfo setStartedAt(String startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public AboutInfo setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public AboutInfo setBuildDate(String buildDate) {
    this.buildDate = buildDate;
    return this;
  }

  public String getBuildDate() {
    return buildDate;
  }

  public AboutInfo setCommitId(String commitId) {
    this.commitId = commitId;
    return this;
  }

  public String getCommitId() {
    return commitId;
  }

  public AboutInfo setScmBranch(String scmBranch) {
    this.scmBranch = scmBranch;
    return this;
  }

  public String getScmBranch() {
    return scmBranch;
  }

  public AboutInfo setProjectDirURI(URI projectDir) {
    this.projectDir = projectDir;
    return this;
  }

  public URI getProjectDirURI() {
    return projectDir;
  }

  public boolean isDotRendering() {
    return dotRendering;
  }

  public AboutInfo setDotRendering(boolean dotRendering) {
    this.dotRendering = dotRendering;
    return this;
  }
}
