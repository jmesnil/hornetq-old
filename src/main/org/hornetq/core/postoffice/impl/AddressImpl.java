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
package org.hornetq.core.postoffice.impl;

import static org.hornetq.core.postoffice.impl.WildcardAddressManager.ANY_WORDS;
import static org.hornetq.core.postoffice.impl.WildcardAddressManager.ANY_WORDS_SIMPLESTRING;
import static org.hornetq.core.postoffice.impl.WildcardAddressManager.DELIM;
import static org.hornetq.core.postoffice.impl.WildcardAddressManager.SINGLE_WORD;
import static org.hornetq.core.postoffice.impl.WildcardAddressManager.SINGLE_WORD_SIMPLESTRING;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.core.postoffice.Address;
import org.hornetq.utils.SimpleString;

/**
 * splits an address string into its hierarchical parts split by '.'
 *
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class AddressImpl implements Address
{
   private SimpleString address;

   private SimpleString[] addressParts;

   private boolean containsWildCard;

   private List<Address> linkedAddresses = new ArrayList<Address>();

   public AddressImpl(final SimpleString address)
   {
      this.address = address;
      this.addressParts = address.split(DELIM);
      containsWildCard = address.contains(SINGLE_WORD) || address.contains(ANY_WORDS);
   }

   public SimpleString getAddress()
   {
      return address;
   }

   public SimpleString[] getAddressParts()
   {
      return addressParts;
   }

   public boolean containsWildCard()
   {
      return containsWildCard;
   }

   public List<Address> getLinkedAddresses()
   {
      return linkedAddresses;
   }

   public void addLinkedAddress(Address address)
   {
      linkedAddresses.add(address);
   }

   public void removeLinkedAddress(Address actualAddress)
   {
      linkedAddresses.remove(actualAddress);
   }

   public void removeAddressPart(final int pos)
   {
      SimpleString newAddress = new SimpleString("");
      boolean started = false;
      for (int i = 0; i < addressParts.length; i++)
      {
         SimpleString addressPart = addressParts[i];
         if (i != pos)
         {
            if (started)
            {
               newAddress = newAddress.concat('.');
            }
            newAddress = newAddress.concat(addressPart);
            started = true;
         }
      }
      this.address = newAddress;
      this.addressParts = address.split(DELIM);
      containsWildCard = address.contains(SINGLE_WORD);
   }

   public boolean matches(final Address add)
   {
      if (containsWildCard == add.containsWildCard())
      {
         return address.equals(add.getAddress());
      }
      int pos = 0;
      int matchPos = 0;

      SimpleString nextToMatch;
      for (; matchPos < add.getAddressParts().length;)
      {
         if(pos >= addressParts.length)
         {
            //test for # as last address part
            return pos + 1 == add.getAddressParts().length && add.getAddressParts()[pos].equals(ANY_WORDS_SIMPLESTRING);
         }
         SimpleString curr = addressParts[pos];
         SimpleString next = addressParts.length > pos + 1 ? addressParts[pos + 1] : null;
         SimpleString currMatch = add.getAddressParts()[matchPos];
         if (currMatch.equals(SINGLE_WORD_SIMPLESTRING))
         {
            pos++;
            matchPos++;
         }
         else if (currMatch.equals(ANY_WORDS_SIMPLESTRING))
         {
            if (matchPos == addressParts.length - 1)
            {
               pos++;
               matchPos++;
            }
            else if (next == null)
            {
               return false;
            }
            else if (matchPos == add.getAddressParts().length - 1)
            {
               return true;
            }
            else
            {
               nextToMatch = add.getAddressParts()[matchPos + 1];
               while (curr != null)
               {
                  if (curr.equals(nextToMatch))
                  {
                     break;
                  }
                  pos++;
                  curr = next;
                  next = addressParts.length > pos + 1 ? addressParts[pos + 1] : null;
               }
               if (curr == null)
               {
                  return false;
               }
               matchPos++;
            }
         }
         else
         {
            if (!curr.equals(currMatch))
            {
               return false;
            }
            pos++;
            matchPos++;
         }
      }
      return pos == addressParts.length;
   }

   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (o == null || getClass() != o.getClass())
      {
         return false;
      }

      AddressImpl address1 = (AddressImpl) o;

      if (!address.equals(address1.address))
      {
         return false;
      }

      return true;
   }

   public int hashCode()
   {
      return address.hashCode();
   }
}
