/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */ 

package org.hornetq.tests.unit.util;

import static java.lang.Integer.parseInt;

import java.util.Properties;

import org.hornetq.core.version.Version;
import org.hornetq.tests.util.UnitTestCase;
import org.hornetq.utils.VersionLoader;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 * @version <tt>$Revision$</tt>
 *
 */
public class VersionLoaderTest extends UnitTestCase
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testLoadVersion() throws Exception
   {
      Version version = VersionLoader.getVersion();
      Properties props = new Properties();
      props.load(ClassLoader.getSystemResourceAsStream(VersionLoader.PROP_FILE_NAME));
   
      assertEquals(props.get("hornetq.version.versionName"), version.getVersionName());
      assertEquals(props.get("hornetq.version.versionSuffix"), version.getVersionSuffix());

      assertEquals(parseInt((String) props.get("hornetq.version.majorVersion")), version.getMajorVersion());
      assertEquals(parseInt((String) props.get("hornetq.version.minorVersion")), version.getMinorVersion());
      assertEquals(parseInt((String) props.get("hornetq.version.microVersion")), version.getMicroVersion());
      assertEquals(parseInt((String) props.get("hornetq.version.incrementingVersion")), version.getIncrementingVersion());
   }
   
   
   // Z implementation ----------------------------------------------

   // Y overrides ---------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}