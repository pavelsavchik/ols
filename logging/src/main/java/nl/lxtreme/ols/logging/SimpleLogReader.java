/*
 * OpenBench LogicSniffer / SUMP project 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.logging;


import java.text.*;
import java.util.*;

import org.osgi.service.log.*;


/**
 * 
 */
public final class SimpleLogReader implements LogListener
{
  // VARIABLES

  private final DateFormat formatter;

  // CONSTRUCTORS

  /**
   * 
   */
  public SimpleLogReader()
  {
    this.formatter = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM );
  }

  // METHODS

  /**
   * @see org.osgi.service.log.LogListener#logged(org.osgi.service.log.LogEntry)
   */
  @Override
  public void logged( final LogEntry aLogEntry )
  {
    if ( !logEvent( aLogEntry ) )
    {
      return;
    }

    if ( aLogEntry.getMessage() != null )
    {
      final StringBuilder sb = new StringBuilder();
      sb.append( '[' );
      sb.append( this.formatter.format( new Date( aLogEntry.getTime() ) ) );
      sb.append( " - " );
      sb.append( getLevel( aLogEntry.getLevel() ) );
      sb.append( " - " );
      sb.append( aLogEntry.getBundle().getSymbolicName() );
      sb.append( "]: " );
      sb.append( aLogEntry.getMessage() );

      if ( aLogEntry.getException() != null )
      {
        System.err.println( sb.toString() );
        aLogEntry.getException().printStackTrace();
      }

      System.out.println( sb.toString() );
    }
  }

  /**
   * @param aLogLevel
   * @return
   */
  private String getLevel( final int aLogLevel )
  {
    if ( LogService.LOG_DEBUG == aLogLevel )
    {
      return "DEBUG";
    }
    else if ( LogService.LOG_ERROR == aLogLevel )
    {
      return "ERROR";
    }
    else if ( LogService.LOG_INFO == aLogLevel )
    {
      return "INFO ";
    }
    else if ( LogService.LOG_WARNING == aLogLevel )
    {
      return "WARN ";
    }
    return "     ";
  }

  /**
   * @param aLogEntry
   * @return
   */
  private boolean logEvent( final LogEntry aLogEntry )
  {
    final String msg = aLogEntry.getMessage();
    if ( msg.startsWith( "BundleEvent" ) || msg.startsWith( "FrameworkEvent" ) || msg.startsWith( "ServiceEvent" ) )
    {
      if ( msg.endsWith( "STARTED" ) || msg.endsWith( "RESOLVED" ) || msg.endsWith( "STOPPED" )
          || msg.endsWith( "UNREGISTERING" ) || msg.endsWith( "REGISTERED" ) )
      {
        return false;
      }
    }
    return true;
  }
}

/* EOF */