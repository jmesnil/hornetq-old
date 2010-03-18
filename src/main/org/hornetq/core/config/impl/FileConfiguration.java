/*
 * Copyright 2009 Red Hat, Inc.
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.hornetq.core.config.impl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import org.hornetq.core.deployers.impl.FileConfigurationParser;
import org.hornetq.core.logging.Logger;
import org.hornetq.utils.XMLUtil;
import org.w3c.dom.Element;

/**
 * ConfigurationImpl
 * This class allows the Configuration class to be configured via a config file.
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 */
public class FileConfiguration extends ConfigurationImpl
{
   private static final long serialVersionUID = -4766689627675039596L;

   private static final Logger log = Logger.getLogger(FileConfiguration.class);

   // Constants ------------------------------------------------------------------------

   private static final String DEFAULT_CONFIGURATION_URL = "hornetq-configuration.xml";

   // For a bridge confirmations must be activated or send acknowledgements won't return

   public static final int DEFAULT_CONFIRMATION_WINDOW_SIZE = 1024 * 1024;

   // Static --------------------------------------------------------------------------

   // Attributes ----------------------------------------------------------------------

   private String configurationUrl = FileConfiguration.DEFAULT_CONFIGURATION_URL;

   private boolean started;

   // Public -------------------------------------------------------------------------

   public synchronized void start() throws Exception
   {
      if (started)
      {
         return;
      }
      
      
      URL url = getClass().getClassLoader().getResource(configurationUrl);
      
      if (url == null)
      {
         // The URL is outside of the classloader. Trying a pure url now
         url = new URL(configurationUrl);
      }
      
      FileConfiguration.log.debug("Loading server configuration from " + url);

      Reader reader = new InputStreamReader(url.openStream());
      String xml = org.hornetq.utils.XMLUtil.readerToString(reader);
      xml = XMLUtil.replaceSystemProps(xml);
      Element e = org.hornetq.utils.XMLUtil.stringToElement(xml);
      
      FileConfigurationParser parser = new FileConfigurationParser();

      parser.parseMainConfig(e, this);

      started = true;

   }

   public synchronized void stop() throws Exception
   {
      started = false;
   }

   public String getConfigurationUrl()
   {
      return configurationUrl;
   }

   public void setConfigurationUrl(final String configurationUrl)
   {
      this.configurationUrl = configurationUrl;
   }

   // Private -------------------------------------------------------------------------
}
