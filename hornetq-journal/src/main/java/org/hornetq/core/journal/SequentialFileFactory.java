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

package org.hornetq.core.journal;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 
 * A SequentialFileFactory
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public interface SequentialFileFactory
{
   SequentialFile createSequentialFile(String fileName, int maxIO);

   List<String> listFiles(String extension) throws Exception;

   boolean isSupportsCallbacks();
   
   /** The SequentialFile will call this method when a disk IO Error happens during the live phase. */
   void onIOError(int errorCode, String message, SequentialFile file);

   /**
    * Note: You need to release the buffer if is used for reading operations.
    *       You don't need to do it if using writing operations (AIO Buffer Lister will take of writing operations)
    * @param size
    * @return
    */
   ByteBuffer newBuffer(int size);

   void releaseBuffer(ByteBuffer buffer);

   void activateBuffer(SequentialFile file);

   void deactivateBuffer();

   // To be used in tests only
   ByteBuffer wrapBuffer(byte[] bytes);

   int getAlignment();

   int calculateBlockSize(int bytes);

   void clearBuffer(ByteBuffer buffer);

   void start();

   void stop();

   /** 
    * Create the directory if it doesn't exist yet
    */
   void createDirs() throws Exception;

   void flush();
}
