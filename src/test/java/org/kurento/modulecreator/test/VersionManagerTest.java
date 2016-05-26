/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.kurento.modulecreator.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.kurento.modulecreator.VersionManager.convertToMavenImport;
import static org.kurento.modulecreator.VersionManager.convertToNpmImport;
import static org.kurento.modulecreator.VersionManager.devCompatibleVersion;
import static org.kurento.modulecreator.VersionManager.versionCompare;

import org.junit.Test;

public class VersionManagerTest {

  @Test
  public void testNumericalVersions() {

    assertTrue(versionCompare("1.2.3", "1.2.3") == 0);
    assertTrue(versionCompare("1.2.3", "1.2.3.4") < 0);
    assertTrue(versionCompare("1.2.3.4", "1.2.3") > 0);
    assertTrue(versionCompare("1.2.3", "1.2.4") < 0);
    assertTrue(versionCompare("1.0", "1.2.4") < 0);
  }

  @Test
  public void testCompatibleVersions() {

    assertTrue(devCompatibleVersion("1.2.3-dev", "1.2.3-dev"));
    assertTrue(devCompatibleVersion("1.2.3-dev", "1.2.4"));
    assertTrue(devCompatibleVersion("1.2.3-dev", "1.3.0"));
    assertTrue(devCompatibleVersion("1.2.3-dev", "1.9.9"));
    assertFalse(devCompatibleVersion("1.2.3-dev", "2.0.0"));
    assertTrue(devCompatibleVersion("^6.0.0", "6.0.0-dev"));
    assertTrue(devCompatibleVersion("6.0.0-dev", "6.0.0"));
    assertTrue(devCompatibleVersion("6.0.0-dev", "^6.0.0"));
  }

  @Test
  public void mavenConversionTest() {

    assertThat(convertToMavenImport("^1.2.3"), is("[1.2.3,2.0.0-SNAPSHOT)"));
    assertThat(convertToMavenImport("1.0.0-dev"), is("1.0.0-SNAPSHOT"));
    assertThat(convertToMavenImport("~1.2.3"), is("[1.2.3,1.3.0-SNAPSHOT)"));
    assertThat(convertToMavenImport("~1.2"), is("[1.2.0,2.0.0-SNAPSHOT)"));
    assertThat(convertToMavenImport("~1"), is("[1.0.0,)"));
    assertThat(convertToMavenImport("1.0.0"), is("1.0.0"));
    assertThat(convertToMavenImport("<1.0.0"), is("(,1.0.0-SNAPSHOT)"));
    assertThat(convertToMavenImport(">1.0.0"), is("(1.0.0,)"));
    assertThat(convertToMavenImport("<=1.0.0"), is("(,1.0.0]"));
    assertThat(convertToMavenImport(">=1.0.0"), is("[1.0.0,)"));
    assertThat(convertToMavenImport("<=0.9.9 | >=1.0.0"), is("(,0.9.9],[1.0.0,)"));
    assertThat(convertToMavenImport("<0.9.9 | >=1.0.0"), is("(,0.9.9-SNAPSHOT),[1.0.0,)"));
    assertThat(convertToMavenImport(">=1.2 & <=1.3"), is("[1.2.0,1.3.0]"));
    assertThat(convertToMavenImport(">=1.0 & <2.0"), is("[1.0.0,2.0.0-SNAPSHOT)"));
    assertThat(convertToMavenImport("<=1.0 | >=1.2"), is("(,1.0.0],[1.2.0,)"));
  }

  @Test
  public void npmConversionTest() {

    assertThat(convertToNpmImport(null, "^1.2.3"), is(">=1.2.3 <2.0.0"));
    assertThat(convertToNpmImport(null, "1.0.0"), is("1.0.0"));
    assertThat(convertToNpmImport(null, "~1.2.3"), is(">=1.2.3 <1.3.0"));
    assertThat(convertToNpmImport(null, "~1.2"), is(">=1.2.0 <2.0.0"));
    assertThat(convertToNpmImport(null, "~1"), is(">=1.0.0"));
    assertThat(convertToNpmImport(null, "<1.0.0"), is("<1.0.0"));
    assertThat(convertToNpmImport(null, ">1.0.0"), is(">1.0.0"));
    assertThat(convertToNpmImport(null, "<=1.0.0"), is("<=1.0.0"));
    assertThat(convertToNpmImport(null, ">=1.0.0"), is(">=1.0.0"));
    assertThat(convertToNpmImport(null, ">=1.2 & <=1.3"), is(">=1.2.0 <=1.3.0"));
    assertThat(convertToNpmImport(null, ">=1.0 & <2.0"), is(">=1.0.0 <2.0.0"));
    assertThat(convertToNpmImport(null, "<=1.0 | >=1.2"), is("<=1.0.0 || >=1.2.0"));
  }

}
