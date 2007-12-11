package org.jboss.messaging.newcore.impl.bdbje.test.unit;

import org.jboss.messaging.newcore.impl.bdbje.BDBJEEnvironment;
import org.jboss.messaging.newcore.impl.bdbje.test.unit.fakes.FakeBDBJEEnvironment;

/**
 * 
 * A FakeBDBJEEnvironmentTest
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class FakeBDBJEEnvironmentTest extends BDBJEEnvironmentTestBase
{
   protected BDBJEEnvironment createEnvironment() throws Exception
   {
      BDBJEEnvironment env = new FakeBDBJEEnvironment();
      
      return env;
   }   
   
   protected void createDir(String path)
   {      
   }
   
   protected void copyEnvironment()
   {      
   }
   
   protected void copyBackEnvironment()
   {      
   }    
}
