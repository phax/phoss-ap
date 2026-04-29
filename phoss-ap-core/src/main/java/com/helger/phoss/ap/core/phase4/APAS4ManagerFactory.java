/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.core.phase4;

import org.jspecify.annotations.NonNull;

import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.mgr.AS4ManagerFactoryInMemory;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * AS4 manager factory for phoss-ap. Reuses the default in-memory managers from phase4 except for
 * the AS4 duplicate manager, which is backed by the configured JDBC database (see
 * {@link com.helger.phoss.ap.db.AS4DuplicateManagerJdbc}). This ensures incoming AS4 message-ID
 * duplicate detection survives restarts and is shared across multiple AP instances.
 *
 * @author Philip Helger
 */
public class APAS4ManagerFactory extends AS4ManagerFactoryInMemory
{
  @Override
  @NonNull
  public IAS4DuplicateManager createDuplicateManager ()
  {
    return APJdbcMetaManager.getAS4DuplicateMgr ();
  }
}
