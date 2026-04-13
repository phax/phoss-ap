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
package com.helger.phoss.ap.basic.mgr;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.base.tostring.ToStringGenerator;
import com.helger.datetime.helper.PDTFactory;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * Default implementation of {@link IAPTimestampManager}.
 *
 * @author Philip Helger
 */
public class APTimestampManager implements IAPTimestampManager
{
  /** {@inheritDoc} */
  @NonNull
  public OffsetDateTime getCurrentDateTimeUTC ()
  {
    // Use maximum precision
    return PDTFactory.getCurrentOffsetDateTimeUTC ();
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
