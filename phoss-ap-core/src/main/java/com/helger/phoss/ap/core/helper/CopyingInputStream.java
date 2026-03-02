package com.helger.phoss.ap.core.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.WillCloseWhenClosed;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.io.stream.WrappedInputStream;

/**
 * A special input stream that copies all bytes to an output stream during reading.
 *
 * @author Philip Helger
 */
public final class CopyingInputStream extends WrappedInputStream
{
  private final OutputStream m_aWrappedOS;

  public CopyingInputStream (@NonNull final InputStream aWrappedIS,
                             @NonNull @WillCloseWhenClosed final OutputStream aWrappedOS)
  {
    super (aWrappedIS);
    m_aWrappedOS = aWrappedOS;
  }

  @Override
  public int read () throws IOException
  {
    final int ret = in.read ();
    if (ret != -1)
      m_aWrappedOS.write (ret);
    return ret;
  }

  @Override
  public int read (final byte [] b, final int off, final int len) throws IOException
  {
    final int ret = in.read (b, off, len);
    if (ret > 0)
      m_aWrappedOS.write (b, off, ret);
    return ret;
  }

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
