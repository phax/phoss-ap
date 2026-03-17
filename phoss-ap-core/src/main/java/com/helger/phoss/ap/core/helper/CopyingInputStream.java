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
package com.helger.phoss.ap.core.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.WillCloseWhenClosed;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.io.stream.WrappedInputStream;

/**
 * A special input stream that copies all bytes to an output stream during
 * reading.
 *
 * @author Philip Helger
 */
public final class CopyingInputStream extends WrappedInputStream
{
  private final OutputStream m_aWrappedOS;

  /**
   * Constructor.
   *
   * @param aWrappedIS
   *        The input stream to read from. May not be <code>null</code>.
   * @param aWrappedOS
   *        The output stream to copy all read bytes to. May not be <code>null</code>. Will be
   *        closed when this stream is closed.
   */
  public CopyingInputStream (@NonNull final InputStream aWrappedIS,
                             @NonNull @WillCloseWhenClosed final OutputStream aWrappedOS)
  {
    super (aWrappedIS);
    m_aWrappedOS = aWrappedOS;
  }

  /** {@inheritDoc} */
  @Override
  public int read () throws IOException
  {
    final int ret = in.read ();
    if (ret != -1)
      m_aWrappedOS.write (ret);
    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public int read (final byte [] b, final int off, final int len) throws IOException
  {
    final int ret = in.read (b, off, len);
    if (ret > 0)
      m_aWrappedOS.write (b, off, ret);
    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public void close () throws IOException
  {
    try
    {
      super.close ();
    }
    finally
    {
      StreamHelper.close (m_aWrappedOS);
    }
  }
}
